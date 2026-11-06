package dev.theredstonee.trsclient.core.ui.clips;

import dev.theredstonee.trsclient.core.clips.LocalClipTools;
import dev.theredstonee.trsclient.core.i18n.I18n;
import dev.theredstonee.trsclient.core.ui.*;
import dev.theredstonee.trsclient.core.ui.menus.WindowUi;
import java.nio.file.Path;

/** Non-destructive local editing. The native export backend remains the only writer. */
public final class ClipEditorUi extends WindowUi {
  private final Path input;
  private final Runnable close;
  private final LocalClipTools tools = new LocalClipTools();
  private final TextInput start = new TextInput(20), end = new TextInput(20);
  private final LocalClipTools.Edit edit;
  private volatile boolean exporting;
  private volatile String status;
  private int scroll, maxScroll;

  public ClipEditorUi(Path input, long durationMs, Runnable close) {
    this.input = input;
    this.close = close;
    edit = new LocalClipTools.Edit(durationMs > 0 ? durationMs / 1000.0 : 30);
    start.setText("0");
    end.setText(String.valueOf(edit.end));
    status = I18n.tr("overhaul.clip.requirements");
  }

  protected String title() {
    return I18n.tr("overhaul.clip.title");
  }

  protected void playClick() {}

  protected void content(Canvas c, int x, int y, int w, int h, int mx, int my, float dt) {
    Theme t = Theme.get();
    c.strongText(c.clip(input.getFileName().toString(), w), x, y, t.text);
    int top = y + 24, view = Math.max(1, h - 62), half = (w - 8) / 2;
    c.scissor(x, top, x + w, top + view);
    hits.clip(x, top, w, view);
    int row = top - scroll;
    c.text(I18n.tr("overhaul.clip.trim"), x, row, t.textDim, false);
    row += 18;
    c.text(c.clip(I18n.tr("overhaul.clip.start"), half), x, row, t.text, false);
    c.text(c.clip(I18n.tr("overhaul.clip.end"), half), x + half + 8, row, t.text, false);
    row += 14;
    field(c, start, x, row, half);
    field(c, end, x + half + 8, row, half);
    final int fieldY = row;
    hits.add(
        x,
        fieldY,
        half,
        22,
        () -> {
          if (!exporting) {
            start.setFocused(true);
            end.setFocused(false);
          }
        });
    hits.add(
        x + half + 8,
        fieldY,
        half,
        22,
        () -> {
          if (!exporting) {
            start.setFocused(false);
            end.setFocused(true);
          }
        });
    row += 36;
    c.text(I18n.tr("overhaul.clip.format"), x, row, t.textDim, false);
    row += 18;
    button(
        c,
        x,
        row,
        half,
        26,
        I18n.tr(edit.mute ? "overhaul.clip.muted" : "overhaul.clip.audio"),
        false,
        !exporting,
        mx,
        my,
        () -> edit.mute = !edit.mute);
    button(
        c,
        x + half + 8,
        row,
        half,
        26,
        I18n.tr("overhaul.clip.speed", edit.speed),
        false,
        !exporting,
        mx,
        my,
        () -> edit.speed = edit.speed == 1 ? 2 : edit.speed == 2 ? .5 : 1);
    row += 32;
    button(
        c,
        x,
        row,
        half,
        26,
        I18n.tr(edit.portrait ? "overhaul.clip.portrait" : "overhaul.clip.original"),
        false,
        !exporting,
        mx,
        my,
        () -> edit.portrait = !edit.portrait);
    button(
        c,
        x + half + 8,
        row,
        half,
        26,
        I18n.tr(edit.rotate ? "overhaul.clip.rotated" : "overhaul.clip.upright"),
        false,
        !exporting,
        mx,
        my,
        () -> edit.rotate = !edit.rotate);
    row += 36;
    for (String line : Paint.wrap(c, status, w - 8)) {
      c.text(c.clip(line, w - 8), x + 4, row, t.textDim, false);
      row += 12;
    }
    maxScroll = Math.max(0, row + scroll - top - view);
    scroll = Math.min(scroll, maxScroll);
    hits.noClip();
    c.noScissor();
    scrollbar(c, x + w - 2, top, view, scroll, maxScroll, view + maxScroll);
    button(
        c,
        x,
        y + h - 28,
        w,
        26,
        I18n.tr(exporting ? "overhaul.clip.exporting" : "overhaul.clip.export"),
        true,
        !exporting,
        mx,
        my,
        this::export);
  }

  private void field(Canvas c, TextInput input, int x, int y, int w) {
    dev.theredstonee.trsclient.core.ui.menus.MenuSkin.textField(
        c, x, y, w, 22, input.focused(), false, !exporting);
    c.text(
        c.clip(input.text() + (input.focused() ? "|" : ""), w - 10),
        x + 5,
        y + 7,
        Theme.get().text,
        false);
  }

  private void export() {
    try {
      edit.start = Double.parseDouble(start.text());
      edit.end = Double.parseDouble(end.text());
      edit.validate();
    } catch (IllegalArgumentException failure) {
      status = I18n.tr("overhaul.clip.invalid");
      return;
    }
    exporting = true;
    start.setFocused(false);
    end.setFocused(false);
    status = I18n.tr("overhaul.clip.exporting");
    Thread worker =
        new Thread(
            () -> {
              try {
                status = I18n.tr("overhaul.clip.saved", tools.export(input, edit).getFileName());
              } catch (Exception failure) {
                status = I18n.tr("overhaul.clip.failed", failure.getMessage());
              } finally {
                exporting = false;
              }
            },
            "TFL-clip-export");
    worker.setDaemon(true);
    worker.start();
  }

  public boolean mouseScrolled(double x, double y, double amount) {
    scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(amount) * 24));
    return true;
  }

  public boolean charTyped(char ch) {
    return !exporting && (start.focused() ? start.type(ch) : end.focused() && end.type(ch));
  }

  public boolean keyPressed(int raw, UiKey key, boolean shift) {
    if (key == UiKey.TAB) {
      start.setFocused(false);
      end.setFocused(false);
      return hits.focusNext(shift);
    }
    if (!exporting && ((start.focused() && start.key(key)) || (end.focused() && end.key(key))))
      return true;
    return super.keyPressed(raw, key, shift);
  }

  public boolean wantsTextInput() {
    return !exporting && (start.focused() || end.focused());
  }

  public void removed() {
    tools.close();
  }

  protected void onClosed() {
    tools.close();
    close.run();
  }
}
