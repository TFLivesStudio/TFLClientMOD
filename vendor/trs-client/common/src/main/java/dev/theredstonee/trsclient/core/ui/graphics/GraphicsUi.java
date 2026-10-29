package dev.theredstonee.trsclient.core.ui.graphics;

import dev.theredstonee.trsclient.core.i18n.I18n;
import dev.theredstonee.trsclient.core.ui.*;
import dev.theredstonee.trsclient.core.ui.menus.*;
import java.util.*;

/** Presentation only: the provider's option objects remain the sole source of state. */
public final class GraphicsUi extends WindowUi {
  public interface Option {
    String id();

    String name();

    String description();

    String value();

    int category();

    boolean available();

    boolean changed();

    void step(int direction);

    void revert();

    default String backend() {
      return getClass().getName();
    }

    default boolean slider() {
      return false;
    }

    default double fraction() {
      return 0;
    }

    default void endSlide() {}

    default void setFraction(double value) {
      throw new UnsupportedOperationException();
    }
  }

  public interface Host {
    List<Option> options();

    void apply();
    default String applyConfirmation(){return "";}
    default String confirmation(Option option,int direction){return "";}
    default void change(Option option,int direction){option.step(direction);}

    default void revert() {
      for (Option option : options()) option.revert();
    }

    void close();

    void shaders();

    boolean hasShaders();

    String status();
  }

  private static final String[] CATEGORIES = {
    "display", "quality", "performance", "shaders", "advanced"
  };
  private final List<SliderHit> sliders = new ArrayList<SliderHit>();
  private SliderHit dragging;

  private static final class SliderHit {
    Option option;
    int x, y, w, index;

    SliderHit(Option o, int x, int y, int w, int index) {
      option = o;
      this.x = x;
      this.y = y;
      this.w = w;
      this.index = index;
    }
  }

  private final Host host;
  private final TextInput search = new TextInput(100);
  private int category, scroll, focus, focusZone, actionFocus, visibleRows = 1;
  private List<Option> shown = Collections.emptyList();
  private String error = "";
  private String selectedGroup="";
  private Runnable pending;private String confirmation="";

  public GraphicsUi(Host host) {
    this.host = host;
    I18n.refresh();
  }

  protected String title() {
    return I18n.tr("graphics.title");
  }

  protected void playClick() {}

  protected int[] size(int width, int height) {
    return new int[] {Math.max(180, width - 16), Math.max(140, height - 16)};
  }

  protected void content(Canvas c, int x, int y, int w, int h, int mx, int my, float dt) {
    Theme t = Theme.get();
    if(pending!=null){
      int row=y+10;for(String line:Paint.wrap(c,confirmation,w-20)){c.text(line,x+10,row,t.text,false);row+=12;}
      button(c,x+10,Math.min(y+h-28,row+15),90,22,I18n.tr("graphics.confirm"),true,true,mx,my,()->confirm());
      button(c,x+105,Math.min(y+h-28,row+15),90,22,I18n.tr("graphics.cancel"),false,true,mx,my,()->{pending=null;});
      return;
    }
    int side = Math.min(108, Math.max(64, w / 4)), right = x + side + 10, bodyW = w - side - 10;
    MenuSkin.textField(c, right, y, bodyW, 20, search.focused(), false, true);
    c.text(
        c.clip(search.text().isEmpty() ? I18n.tr("graphics.search") : search.text(), bodyW - 10),
        right + 5,
        y + 6,
        t.text,
        false);
    hits.add(
        right,
        y,
        bodyW,
        20,
        () -> {
          focusZone = 1;
          search.setFocused(true);
        });
    int categoryRow = 0;
    for (int i = 0; i < CATEGORIES.length; i++) {
      final int selected = i;
      boolean present = false;
      for (Option o : host.options())
        if (o.category() == i) {
          present = true;
          break;
        }
      if (i == 3 && host.hasShaders()) present = true;
      if (!present) continue;
      int row = y + categoryRow++ * 27;
      SelectionStyle.button(c, x, row, side, 23, category == i, false);
      c.text(
          c.clip(I18n.tr("graphics.category." + CATEGORIES[i]), side - 10),
          x + 5,
          row + 8,
          t.text,
          false);
      hits.add(
          x,
          row,
          side,
          23,
          () -> {
            focusZone = 0;
            category = selected;
            scroll = focus = 0;
            search.setFocused(false);
          });
    }
    List<String> groups=new ArrayList<String>();groups.add("");
    for(Option option:host.options())if(option.category()==category&&!groups.contains(GraphicsPurpose.group(option.id())))groups.add(GraphicsPurpose.group(option.id()));
    if(!groups.contains(selectedGroup))selectedGroup="";
    String groupLabel=I18n.tr("graphics.group."+(selectedGroup.isEmpty()?"all":selectedGroup));
    button(c,right,y+24,bodyW,18,groupLabel+"  ▾",false,true,mx,my,()->{selectedGroup=groups.get((groups.indexOf(selectedGroup)+1)%groups.size());scroll=focus=0;});
    String query = search.text().toLowerCase(Locale.ROOT);
    shown = new ArrayList<Option>();
    for (Option o : host.options())
      if ((query.isEmpty()
          ? o.category() == category && (selectedGroup.isEmpty() || selectedGroup.equals(GraphicsPurpose.group(o.id())))
          : (o.id() + " " + GraphicsPurpose.name(o.id(),o.name()) + " " + o.name() + " " + o.description() + " " + searchAliases(o.id()))
              .toLowerCase(Locale.ROOT)
              .contains(query))) shown.add(o);
    int rows = Math.max(1, (h - (category == 3 && host.hasShaders() ? 130 : 100)) / 30);
    visibleRows = rows;
    scroll = Math.max(0, Math.min(scroll, Math.max(0, shown.size() - rows)));
    int top = y + 47;
    sliders.clear();
    String tooltip = "";
    for (int i = scroll; i < Math.min(shown.size(), scroll + rows); i++) {
      final Option o = shown.get(i);
      final int index = i;
      int row = top + (i - scroll) * 30;
      boolean active = o.available();
      if (focusZone == 2 && focus == i) c.fill(right, row, right + bodyW, row + 27, t.surfaceHigh);
      int control = Math.min(95, bodyW / 2);
      c.text(
          c.clip(GraphicsPurpose.name(o.id(),o.name()), bodyW - control - 12),
          right + 3,
          row + 4,
          active ? t.text : t.textDim,
          false);
      c.text(c.clip(o.description(), bodyW - control - 12), right + 3, row + 16, t.textDim, false);
      int cx = right + bodyW - control;
      if (o.slider()) {
        c.text(c.clip(o.value(), control - 8), cx + 4, row + 2, active ? t.text : t.textDim, false);
        c.fill(cx + 3, row + 19, cx + control - 3, row + 22, t.border);
        int fill = (int) Math.round((control - 6) * Math.max(0, Math.min(1, o.fraction())));
        c.fill(cx + 3, row + 19, cx + 3 + fill, row + 22, active ? t.accent : t.textDim);
        if (active) sliders.add(new SliderHit(o, cx + 3, row, control - 6, i));
      } else
        button(
            c,
            cx,
            row,
            control,
            24,
            c.clip(o.value(), control - 8),
            o.changed(),
            active,
            mx,
            my,
            () -> {
              focusZone = 2;
              focus = index;
              change(o, 1);
            });
      if (inside(mx, my, right, row, bodyW - control - 6, 27)) tooltip = o.description();
    }
    if (shown.isEmpty()) c.text(I18n.tr("graphics.empty"), right + 4, top + 8, t.textDim, false);
    int bottom = y + h - 24;
    button(
        c,
        x,
        bottom,
        Math.min(85, side),
        22,
        I18n.tr("graphics.back"),
        false,
        true,
        mx,
        my,
        this::requestClose);
    button(
        c,
        right,
        bottom,
        75,
        22,
        I18n.tr("graphics.apply"),
        true,
        dirty(),
        mx,
        my,
        this::applyRequested);
    button(
        c,
        right + 80,
        bottom,
        Math.min(95, Math.max(45, bodyW - 80)),
        22,
        I18n.tr("graphics.undo"),
        false,
        dirty(),
        mx,
        my,
        () -> {
          host.revert();
        });
    if (category == 3 && host.hasShaders())
      button(
          c,
          right,
          y + h - 53,
          Math.min(180, bodyW),
          22,
          I18n.tr("graphics.shaderPacks"),
          false,
          !dirty(),
          mx,
          my,
          host::shaders);
    if (!tooltip.isEmpty()) {
      int tw = Math.min(bodyW, 260), ty = Math.min(y + h - 75, my + 14);
      List<String> lines = Paint.wrap(c, tooltip, tw - 12);
      int count = Math.min(5, lines.size());
      ty = Math.max(y, Math.min(ty, y + h - count * 12 - 36));
      SelectionStyle.panel(c, right, ty, tw, count * 12 + 10, t.surfaceHigh, t.border);
      for (int line = 0; line < count; line++)
        c.text(lines.get(line), right + 6, ty + 5 + line * 12, t.text, false);
    }
    if (focusZone == 3) {
      int fx = actionFocus == 0 ? right : actionFocus == 1 ? right + 80 : x;
      int fw =
          actionFocus == 0
              ? 75
              : actionFocus == 1 ? Math.min(95, Math.max(45, bodyW - 80)) : Math.min(85, side);
      c.fill(fx + 3, bottom + 21, fx + fw - 3, bottom + 22, t.accent);
    }
    String status = error.isEmpty() ? (category == 3 || category == 4 ? host.status() : "") : error;
    if (!status.isEmpty())
      c.text(
          c.clip(status, bodyW),
          right,
          category == 3 && host.hasShaders() ? y + h - 68 : y + h - 36,
          t.textDim,
          false);
  }

  public Host provider() {
    return host;
  }

  public boolean selectCategory(int next) {
    for (Option o : host.options())
      if (o.category() == next) {
        category = next;
        scroll = focus = 0;
        return true;
      }
    return false;
  }

  public List<Map<String, String>> inventory() {
    List<Map<String, String>> result = new ArrayList<Map<String, String>>();
    for (Option o : host.options()) {
      Map<String, String> row = new LinkedHashMap<String, String>();
      row.put("id", o.id());
      row.put("name", o.name());
      row.put("category", I18n.tr("graphics.category." + CATEGORIES[o.category()]));
      row.put("backend", o.backend());
      result.add(row);
    }
    return result;
  }

  private static String searchAliases(String id) {
    String s = id.toLowerCase(Locale.ROOT), r = "";
    if (s.contains("brightness") || s.contains("gamma")) r += " brillo gamma";
    if (s.contains("frame") || s.contains("fps")) r += " fps fotogramas frames";
    if (s.contains("resolution") || s.contains("fullscreen"))
      r += " resolución resolucion pantalla screen";
    if (s.contains("shadow")) r += " sombras shadows";
    if (s.contains("sharpness")) r += " nitidez sharpness";
    if (s.contains("amplio") || s.contains("upscal")) r += " escalado upscaling fsr1 dlss";
    if (s.contains("iris") || s.contains("shader")) r += " shaders sombreadores";
    return r;
  }

  private boolean dirty() {
    for (Option o : host.options()) if (o.changed()) return true;
    return false;
  }

  private void ask(String message,Runnable action){if(message==null||message.isEmpty())action.run();else{confirmation=message;pending=action;}}
  private void confirm(){Runnable action=pending;pending=null;try{action.run();error="";}catch(RuntimeException e){error=I18n.tr("graphics.error.apply",e.getMessage());}}
  private void applyRequested(){try{ask(host.applyConfirmation(),()->host.apply());}catch(RuntimeException e){error=I18n.tr("graphics.error.apply",e.getMessage());}}
  private void change(Option option, int direction) {
    search.setFocused(false);
    try {
      ask(host.confirmation(option,direction),()->host.change(option,direction));
      error = "";
    } catch (RuntimeException e) {
      error = I18n.tr("graphics.error.change",e.getMessage());
    }
  }

  public boolean mouseClicked(double x, double y, int button) {
    if (button == 0)
      for (SliderHit hit : sliders)
        if (x >= hit.x - 3 && x < hit.x + hit.w + 3 && y >= hit.y && y < hit.y + 26) {
          dragging = hit;
          focusZone = 2;
          focus = hit.index;
          search.setFocused(false);
          slide(x);
          return true;
        }
    return super.mouseClicked(x, y, button);
  }

  private void slide(double x) {
    if (dragging == null) return;
    try {
      dragging.option.setFraction(Math.max(0, Math.min(1, (x - dragging.x) / dragging.w)));
    } catch (RuntimeException e) {
      error = I18n.tr("graphics.error.value",e.getMessage());
    }
  }

  public boolean mouseDragged(double x, double y, int button) {
    if (dragging == null) return false;
    slide(x);
    return true;
  }

  public boolean mouseReleased(double x, double y, int button) {
    boolean had = dragging != null;
    if (had) {
      try {
        dragging.option.endSlide();
      } catch (RuntimeException failure) {
        error =
            failure.getMessage() == null
                ? failure.getClass().getSimpleName()
                : failure.getMessage();
      }
    }
    dragging = null;
    return had || super.mouseReleased(x, y, button);
  }

  public boolean mouseScrolled(double x, double y, double amount) {
    scroll -= amount > 0 ? 1 : -1;
    return true;
  }

  public boolean charTyped(char ch) {
    if (!search.focused()) return false;
    scroll = focus = 0;
    return search.type(ch);
  }

  public boolean wantsTextInput() {
    return search.focused();
  }

  @Override
  public void requestClose() {
    if(pending!=null){pending=null;return;}
    if (dirty()) {
      error = I18n.tr("graphics.pending");
      return;
    }
    super.requestClose();
  }

  public boolean keyPressed(int raw, UiKey key, boolean shift) {
    if(pending!=null){if(key==UiKey.ENTER)confirm();else if(key==UiKey.ESCAPE)pending=null;return true;}
    if (key == UiKey.ESCAPE) return super.keyPressed(raw, key, shift);
    if (key == UiKey.TAB) {
      focusZone = Math.floorMod(focusZone + (shift ? -1 : 1), 4);
      search.setFocused(focusZone == 1);
      return true;
    }
    if (search.focused()) {
      if (search.key(key)) {
        scroll = focus = 0;
        return true;
      }
      return false;
    }
    if (focusZone == 0) {
      if (key == UiKey.DOWN || key == UiKey.RIGHT || key == UiKey.UP || key == UiKey.LEFT) {
        int step = key == UiKey.DOWN || key == UiKey.RIGHT ? 1 : -1;
        for (int i = 1; i <= CATEGORIES.length; i++) {
          int next = Math.floorMod(category + step * i, CATEGORIES.length);
          if (selectCategory(next)) break;
        }
        return true;
      }
      if (key == UiKey.ENTER) {
        focusZone = 2;
        return true;
      }
    }
    if (focusZone == 3) {
      if (key == UiKey.LEFT || key == UiKey.RIGHT) {
        actionFocus = Math.floorMod(actionFocus + (key == UiKey.LEFT ? -1 : 1), 3);
        return true;
      }
      if (key == UiKey.ENTER) {
        if (actionFocus == 0 && dirty()) {
          applyRequested();
        } else if (actionFocus == 1) {
          host.revert();
        } else requestClose();
        return true;
      }
    }
    if (focusZone == 2) {
      if (key == UiKey.DOWN || key == UiKey.UP) {
        focus = Math.max(0, Math.min(shown.size() - 1, focus + (key == UiKey.DOWN ? 1 : -1)));
        if (focus < scroll) scroll = focus;
        if (focus >= scroll + visibleRows) scroll = focus - visibleRows + 1;
        return true;
      }
      if ((key == UiKey.ENTER || key == UiKey.RIGHT || key == UiKey.LEFT)
          && focus < shown.size()
          && !shown.isEmpty()) {
        Option o = shown.get(focus);
        if (o.available()) change(o, key == UiKey.LEFT ? -1 : 1);
        return true;
      }
    }
    return super.keyPressed(raw, key, shift);
  }

  protected void onClosed() {
    host.revert();
    host.close();
  }
}
