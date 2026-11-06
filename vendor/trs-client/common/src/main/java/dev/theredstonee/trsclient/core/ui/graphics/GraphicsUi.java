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

    /** Optional typed, reversible binding. Null explicitly excludes unsafe controls from presets. */
    default GraphicsProfiles.Capability capability() { return null; }

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

    default String applyConfirmation() {
      return "";
    }

    default String confirmation(Option option, int direction) {
      return "";
    }

    default void change(Option option, int direction) {
      option.step(direction);
    }

    default void revert() {
      for (Option option : options()) option.revert();
    }

    void close();

    void shaders();

    boolean hasShaders();

    String status();
  }

  private static final String[] CATEGORIES = {
    "display", "quality", "performance", "shaders", "advanced", "lighting", "geometry"
  };
  private static final int[] NAVIGATION={-1,0,1,5,6,2,3,4};
  private final List<SliderHit> sliders = new ArrayList<SliderHit>();
  private SliderHit dragging;
  private int navScroll;
  private final int[] navigation=new int[4];

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
  private final GraphicsProfiles profiles;
  private GraphicsProfiles.Plan profilePlan;
  private int profileFocus, profileScroll;
  private String profileReport = "";
  private final TextInput search = new TextInput(100);
  private int category = -1, scroll, focus, focusZone, actionFocus, visibleRows = 1;
  private List<Option> shown = Collections.emptyList();
  private String error = "";
  private String selectedGroup = "";
  private Runnable pending;
  private String confirmation = "";

  public GraphicsUi(Host host) {
    this.host = host;
    this.profiles = new GraphicsProfiles(host);
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
    if (pending != null) {
      int row = y + 10;
      for (String line : Paint.wrap(c, confirmation, w - 20)) {
        c.text(line, x + 10, row, t.text, false);
        row += 12;
      }
      button(
          c,
          x + 10,
          Math.min(y + h - 28, row + 15),
          90,
          22,
          I18n.tr("graphics.confirm"),
          true,
          true,
          mx,
          my,
          () -> confirm());
      button(
          c,
          x + 105,
          Math.min(y + h - 28, row + 15),
          90,
          22,
          I18n.tr("graphics.cancel"),
          false,
          true,
          mx,
          my,
          () -> {
            pending = null;
          });
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
    navigation[0]=x;navigation[1]=y;navigation[2]=side;navigation[3]=h;
    int navHeight=27*(CATEGORIES.length+1);
    navScroll=Math.max(0,Math.min(navScroll,Math.max(0,navHeight-h)));
    c.scissor(x,y,x+side,y+h);hits.clip(x,y,side,h);
    button(c, x, y-navScroll, side, 25, I18n.tr("graphics.profile.title"), category == -1, true, mx, my,
        () -> { category=-1; profilePlan=null; profileScroll=0; search.setFocused(false); focusZone=0; });
    int categoryRow = 1;
    for (int sectionIndex=1;sectionIndex<NAVIGATION.length;sectionIndex++) {
      final int i=NAVIGATION[sectionIndex], selected=i;
      boolean present = false;
      for (Option o : host.options())
        if (section(o) == i) {
          present = true;
          break;
        }
      if (i == 3 && host.hasShaders()) present = true;
      if (!present) continue;
      int row = y + categoryRow++ * 27-navScroll;
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
    hits.noClip();c.noScissor();
    scrollbar(c,x+side-2,y,h,navScroll,Math.max(0,navHeight-h),navHeight);
    if(category == -1 && search.isEmpty()) {
      profilePage(c,right,y+27,bodyW,h-27,mx,my);
      return;
    }
    List<String> groups = new ArrayList<String>();
    groups.add("");
    for (Option option : host.options())
      if (section(option) == category && !groups.contains(GraphicsPurpose.group(option.id())))
        groups.add(GraphicsPurpose.group(option.id()));
    if (!groups.contains(selectedGroup)) selectedGroup = "";
    String groupLabel =
        I18n.tr("graphics.group." + (selectedGroup.isEmpty() ? "all" : selectedGroup));
    button(
        c,
        right,
        y + 24,
        bodyW,
        18,
        groupLabel + "  ▾",
        false,
        true,
        mx,
        my,
        () -> {
          selectedGroup = groups.get((groups.indexOf(selectedGroup) + 1) % groups.size());
          scroll = focus = 0;
        });
    String query = search.text().toLowerCase(Locale.ROOT);
    shown = new ArrayList<Option>();
    for (Option o : host.options())
      if ((query.isEmpty()
          ? section(o) == category
              && (selectedGroup.isEmpty() || selectedGroup.equals(GraphicsPurpose.group(o.id())))
          : (o.id()
                  + " "
                  + GraphicsPurpose.name(o.id(), o.name())
                  + " "
                  + o.name()
                  + " "
                  + o.description()
                  + " "
                  + searchAliases(o.id()))
              .toLowerCase(Locale.ROOT)
              .contains(query))) shown.add(o);
    int rows = Math.max(1, (h - (category == 3 && host.hasShaders() ? 130 : 100)) / 36);
    visibleRows = rows;
    scroll = Math.max(0, Math.min(scroll, Math.max(0, shown.size() - rows)));
    int top = y + 47;
    sliders.clear();
    String tooltip = "";
    for (int i = scroll; i < Math.min(shown.size(), scroll + rows); i++) {
      final Option o = shown.get(i);
      final int index = i;
      int row = top + (i - scroll) * 36;
      boolean active = o.available();
      SelectionStyle.rounded(c,right,row,bodyW,32,4,
          focusZone == 2 && focus == i ? t.surfaceHover : t.surface);
      if (focusZone == 2 && focus == i) c.fill(right,row+6,right+2,row+26,t.accent);
      int control = Math.min(95, bodyW / 2);
      c.text(
          c.clip(GraphicsPurpose.name(o.id(), o.name()), bodyW - control - 12),
          right + 7,
          row + 5,
          active ? t.text : t.textDim,
          false);
      c.text(
          c.clip(GraphicsPurpose.description(o.id(), o.description()), bodyW - control - 12),
          right + 7,
          row + 18,
          t.textDim,
          false);
      int cx = right + bodyW - control;
      if (o.slider()) {
        c.text(
            c.clip(GraphicsPurpose.text(o.value()), control - 8),
            cx + 4,
            row + 2,
            active ? t.text : t.textDim,
            false);
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
            c.clip(GraphicsPurpose.text(o.value()), control - 8),
            o.changed(),
            active,
            mx,
            my,
            () -> {
              focusZone = 2;
              focus = index;
              change(o, 1);
            });
      if (inside(mx, my, right, row, bodyW - control - 6, 27))
        tooltip = GraphicsPurpose.description(o.id(), o.description());
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
      c.overlayLayer();
      SelectionStyle.rounded(
          c, right, ty, tw, count * 12 + 10, UiAppearance.radius(), t.surfaceHigh | 0xff000000);
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

  private void profilePage(Canvas c,int x,int y,int w,int h,int mx,int my) {
    Theme t=Theme.get();
    GraphicsProfiles.Profile effective=profiles.effective();
    c.strongText(I18n.tr("graphics.profile.title"),x,y,t.text);
    c.text(c.clip(I18n.tr("graphics.profile.current", effective==null ? I18n.tr("graphics.profile.custom") : profileName(effective)),w),x,y+15,t.textDim,false);
    int bottom=y+h-24;
    if(profilePlan==null) {
      boolean horizontal=w>=210 && h>=132;
      int rowH=horizontal?Math.min(82,h-62):44,view=Math.max(1,bottom-y-37);
      int max=horizontal?0:Math.max(0,3*(rowH+4)-view);
      profileScroll=Math.max(0,Math.min(profileScroll,max));
      c.scissor(x,y+31,x+w,bottom-5);hits.clip(x,y+31,w,view);
      for(GraphicsProfiles.Profile profile:GraphicsProfiles.Profile.values()) {
        int cardW=horizontal?(w-12)/3:w;
        int cardX=horizontal?x+profile.ordinal()*(cardW+6):x;
        int row=y+32+(horizontal?0:profile.ordinal()*(rowH+4)-profileScroll);
        boolean active=profile==effective, hovered=inside(mx,my,cardX,row,cardW,rowH);
        SelectionStyle.panel(c,cardX,row,cardW,rowH,hovered?t.surfaceHover:t.surface,active?t.accent:t.border);
        c.strongText(c.clip(profileName(profile),cardW-16),cardX+8,row+8,t.text);
        String description=I18n.tr("graphics.profile."+profile.name().toLowerCase(Locale.ROOT)+".desc");
        if(horizontal) {
          int line=0;
          for(String text:Paint.wrap(c,description,cardW-16)) {
            if(25+line*12>rowH-13)break;
            c.text(c.clip(text,cardW-16),cardX+8,row+25+line++*12,t.textDim,false);
          }
        } else c.text(c.clip(description,cardW-16),cardX+8,row+25,t.textDim,false);
        if(focusZone==2 && profileFocus==profile.ordinal()) c.fill(cardX,row+6,cardX+2,row+rowH-6,t.accent);
        hits.add(cardX,row,cardW,rowH,()->prepareProfile(profile));
      }
      hits.noClip();c.noScissor();
      scrollbar(c,x+w-2,y+32,view,profileScroll,max,3*(rowH+4));
      button(c,x,bottom,Math.min(100,w/2-3),22,I18n.tr("graphics.back"),false,true,mx,my,this::requestClose);
      button(c,x+w/2+3,bottom,Math.max(40,w/2-3),22,I18n.tr("graphics.profile.restore"),false,profiles.canUndo(),mx,my,()->runProfile(()->profiles.undo()));
    } else {
      List<String> lines=new ArrayList<>();
      lines.add(I18n.tr("graphics.profile.preview",profileName(profilePlan.profile)));
      for(GraphicsProfiles.Change change:profilePlan.changes) {
        lines.add(GraphicsPurpose.name(change.option.id(),change.option.name())+": "+GraphicsPurpose.text(String.valueOf(change.before))+" → "+GraphicsPurpose.text(String.valueOf(change.after)));
        lines.add("  "+change.option.backend());
      }
      lines.add(I18n.tr("graphics.profile.omitted",profilePlan.omitted.size()));
      lines.addAll(profilePlan.omitted);
      lines.addAll(Paint.wrap(c,I18n.tr("graphics.profile.safe"),w-8));
      lines.addAll(Paint.wrap(c,I18n.tr("graphics.profile.reload"),w-8));
      int rows=Math.max(1,(h-67)/13);
      profileScroll=Math.max(0,Math.min(profileScroll,Math.max(0,lines.size()-rows)));
      for(int i=profileScroll;i<Math.min(lines.size(),profileScroll+rows);i++)
        c.text(c.clip(lines.get(i),w-10),x+4,y+32+(i-profileScroll)*13,t.textDim,false);
      scrollbar(c,x+w-2,y+32,rows*13,profileScroll,Math.max(0,lines.size()-rows),lines.size()*13);
      button(c,x,bottom,Math.max(40,w/2-3),22,I18n.tr("graphics.cancel"),false,true,mx,my,()->profilePlan=null);
      button(c,x+w/2+3,bottom,Math.max(40,w/2-3),22,I18n.tr("graphics.apply"),true,!profilePlan.changes.isEmpty(),mx,my,
          ()->runProfile(()->{ profiles.apply(profilePlan); profileReport=I18n.tr("graphics.profile.applied",profilePlan.changes.size(),profilePlan.omitted.size()); profilePlan=null; }));
    }
    if(focusZone==3) {
      int fx=actionFocus==0?x:x+w/2+3;
      c.fill(fx+3,bottom+21,fx+Math.max(40,w/2-3)-3,bottom+22,t.accent);
    }
    String message=error.isEmpty()?profileReport:error;
    if(!message.isEmpty()) c.text(c.clip(message,w),x,y+h-37,error.isEmpty()?t.textDim:0xFFEF7D89,false);
  }
  private static String profileName(GraphicsProfiles.Profile profile) {
    return I18n.tr("graphics.profile."+profile.name().toLowerCase(Locale.ROOT));
  }
  private void prepareProfile(GraphicsProfiles.Profile profile) {
    runProfile(()->{profilePlan=profiles.prepare(profile);profileScroll=0;});
  }
  private void runProfile(Runnable action) {
    try { action.run(); error=""; }
    catch(RuntimeException e) {error=I18n.trOr(e.getMessage(),e.getMessage()==null?e.getClass().getSimpleName():e.getMessage());}
  }

  private static int section(Option option) {
    String group=GraphicsPurpose.group(option.id());
    if(option.category()!=3 && group.equals("lighting"))return 5;
    if(option.category()==1 && (group.equals("geometry") || group.equals("entities")))return 6;
    return option.category();
  }

  public Host provider() {
    return host;
  }

  public boolean selectCategory(int next) {
    for (Option o : host.options())
      if (section(o) == next) {
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
      row.put("localizedName", GraphicsPurpose.name(o.id(), o.name()));
      row.put("group", GraphicsPurpose.group(o.id()));
      row.put("category", I18n.tr("graphics.category." + CATEGORIES[section(o)]));
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

  private void ask(String message, Runnable action) {
    if (message == null || message.isEmpty()) action.run();
    else {
      confirmation = message;
      pending = action;
    }
  }

  private void confirm() {
    Runnable action = pending;
    pending = null;
    try {
      action.run();
      error = "";
    } catch (RuntimeException e) {
      error = I18n.tr("graphics.error.apply", e.getMessage());
    }
  }

  private void applyRequested() {
    try {
      ask(host.applyConfirmation(), () -> host.apply());
    } catch (RuntimeException e) {
      error = I18n.tr("graphics.error.apply", e.getMessage());
    }
  }

  private void change(Option option, int direction) {
    search.setFocused(false);
    try {
      ask(host.confirmation(option, direction), () -> host.change(option, direction));
      error = "";
    } catch (RuntimeException e) {
      error = I18n.tr("graphics.error.change", e.getMessage());
    }
  }

  public boolean mouseClicked(double x, double y, int button) {
    if (button == 0 && pending == null && category != -1)
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
      error = I18n.tr("graphics.error.value", e.getMessage());
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
    if(inside(x,y,navigation[0],navigation[1],navigation[2],navigation[3])) {
      navScroll-=(amount>0?27:-27);return true;
    }
    if(category == -1 && search.isEmpty()) profileScroll -= amount > 0 ? (profilePlan==null?24:1) : (profilePlan==null?-24:-1);
    else scroll -= amount > 0 ? 1 : -1;
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
    if(profilePlan!=null) {profilePlan=null;return;}
    if (pending != null) {
      pending = null;
      return;
    }
    if (dirty()) {
      error = I18n.tr("graphics.pending");
      return;
    }
    super.requestClose();
  }

  public boolean keyPressed(int raw, UiKey key, boolean shift) {
    if (pending != null) {
      if (key == UiKey.ENTER) confirm();
      else if (key == UiKey.ESCAPE) pending = null;
      return true;
    }
    if (key == UiKey.ESCAPE) return super.keyPressed(raw, key, shift);
    if(category == -1 && search.isEmpty() && focusZone==2) {
      if(key==UiKey.UP || key==UiKey.DOWN || key==UiKey.LEFT || key==UiKey.RIGHT) {
        if(profilePlan!=null) profileScroll=Math.max(0,profileScroll+(key==UiKey.DOWN?1:-1));
        else {profileFocus=Math.floorMod(profileFocus+(key==UiKey.DOWN || key==UiKey.RIGHT?1:-1),3);profileScroll=Math.max(0,profileFocus*48-24);}
        return true;
      }
      if(key==UiKey.ENTER) {
        if(profilePlan==null) prepareProfile(GraphicsProfiles.Profile.values()[profileFocus]);
        else if(!profilePlan.changes.isEmpty()) runProfile(()->{profiles.apply(profilePlan);profilePlan=null;});
        return true;
      }
    }
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
        int current=0;
        for(int i=0;i<NAVIGATION.length;i++)if(NAVIGATION[i]==category)current=i;
        for(int i=1;i<=NAVIGATION.length;i++) {
          int at=Math.floorMod(current+step*i,NAVIGATION.length),next=NAVIGATION[at];
          if(next==-1) {category=-1;profilePlan=null;navScroll=0;break;}
          if(selectCategory(next)) {navScroll=Math.max(0,at*27-27);break;}
        }
        return true;
      }
      if (key == UiKey.ENTER) {
        focusZone = 2;
        return true;
      }
    }
    if (focusZone == 3 && category==-1 && search.isEmpty()) {
      if(key==UiKey.LEFT || key==UiKey.RIGHT) {actionFocus=Math.floorMod(actionFocus+1,2);return true;}
      if(key==UiKey.ENTER) {
        if(actionFocus==0) {if(profilePlan!=null)profilePlan=null;else requestClose();}
        else if(profilePlan!=null && !profilePlan.changes.isEmpty())runProfile(()->{profiles.apply(profilePlan);profilePlan=null;});
        else if(profiles.canUndo())runProfile(()->profiles.undo());
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
