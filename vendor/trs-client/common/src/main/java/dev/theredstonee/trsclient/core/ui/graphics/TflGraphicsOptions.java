package dev.theredstonee.trsclient.core.ui.graphics;

import dev.theredstonee.trsclient.core.module.*;
import java.util.List;

/**
 * Shared controls bind to existing settings; their normal immediate-save semantics are retained.
 */
public final class TflGraphicsOptions {
  private TflGraphicsOptions() {}

  public static void add(List<GraphicsUi.Option> out, TrsModules m, Runnable save) {
    add(out, m, save, false);
  }

  public static void add(
      List<GraphicsUi.Option> out, TrsModules m, Runnable save, boolean colorsSupported) {
    add(out, "titleScreen", m.menuFpsLimit, 0, save);
    add(out, "titleScreen", m.menuBackground, 1, save);
    toggle(out, m.dynamicFps, 2, save);
    add(out, "dynamicFps", m.dynamicFpsUnfocused, 2, save, () -> m.dynamicFps.isEnabled());
    add(out, "dynamicFps", m.dynamicFpsMinimized, 2, save, () -> m.dynamicFps.isEnabled());
    add(out, "dynamicFps", m.dynamicFpsAfk, 2, save, () -> m.dynamicFps.isEnabled());
    add(out, "worldMap", m.worldMapCloudQuality, 1, save);
    add(out, "worldMap", m.worldMapCloudMotion, 1, save);
    add(out, "miniPlayer", m.miniPlayer.fps, 1, save);
    toggle(out, m.fullbright, 1, save);
    add(out, "fullbright", m.fullbrightLevel, 1, save, () -> m.fullbright.isEnabled());
    if (colorsSupported) {
      toggle(out, m.colors, 1, save);
      add(out, "colors", m.colorSaturation, 1, save, () -> m.colors.isEnabled());
      add(out, "colors", m.colorContrast, 1, save, () -> m.colors.isEnabled());
      add(out, "colors", m.colorBrightness, 1, save, () -> m.colors.isEnabled());
      add(out, "colors", m.colorVibrance, 1, save, () -> m.colors.isEnabled());
      add(out, "colors", m.colorTemperature, 1, save, () -> m.colors.isEnabled());
    }
  }

  private static void toggle(
      List<GraphicsUi.Option> out,
      dev.theredstonee.trsclient.core.module.Module module,
      int category,
      Runnable save) {
    out.add(
        new GraphicsUi.Option() {
          public String id() {
            return "tflcore:" + module.id() + "/enabled";
          }

          public String name() {
            return module.name();
          }

          public String description() {
            return module.description();
          }

          public String value() {
            return module.isEnabled()
                ? dev.theredstonee.trsclient.core.i18n.I18n.tr("graphics.on")
                : dev.theredstonee.trsclient.core.i18n.I18n.tr("graphics.off");
          }

          public int category() {
            return category;
          }

          public boolean available() {
            return module.available();
          }

          public boolean changed() {
            return false;
          }

          public void step(int direction) {
            module.setEnabled(!module.isEnabled());
            save.run();
          }

          public void revert() {}

          public String backend() {
            return "TFL ConfigStore: " + module.id();
          }
        });
  }

  private static void add(
      List<GraphicsUi.Option> out, String owner, Setting setting, int category, Runnable save) {
    add(out, owner, setting, category, save, () -> true);
  }

  private static void add(
      List<GraphicsUi.Option> out,
      String owner,
      Setting setting,
      int category,
      Runnable save,
      java.util.function.BooleanSupplier availability) {
    out.add(
        new GraphicsUi.Option() {
          public String id() {
            return "tflcore:" + owner + "/" + setting.key();
          }

          public String name() {
            return owner.equals("fullbright")
                ? dev.theredstonee.trsclient.core.i18n.I18n.tr("graphics.extraGamma")
                : owner.equals("colors") ? "Color: " + setting.label() : setting.label();
          }

          public String description() {
            return dev.theredstonee.trsclient.core.i18n.I18n.tr("graphics.immediate");
          }

          public int category() {
            return category;
          }

          public boolean available() {
            return availability.getAsBoolean();
          }

          public boolean changed() {
            return false;
          }

          public String value() {
            if (setting instanceof NumberSetting) return ((NumberSetting) setting).display();
            if (setting instanceof BoolSetting)
              return ((BoolSetting) setting).get()
                  ? dev.theredstonee.trsclient.core.i18n.I18n.tr("graphics.on")
                  : dev.theredstonee.trsclient.core.i18n.I18n.tr("graphics.off");
            ChoiceSetting<?> choice = (ChoiceSetting<?>) setting;
            return choice.optionLabel(choice.index());
          }

          public void step(int direction) {
            if (setting instanceof NumberSetting) ((NumberSetting) setting).nudge(direction);
            else if (setting instanceof BoolSetting) {
              BoolSetting b = (BoolSetting) setting;
              b.set(!b.get());
            } else ((ChoiceSetting<?>) setting).cycle(direction);
            save.run();
          }

          public boolean slider() {
            return setting instanceof NumberSetting;
          }

          public double fraction() {
            NumberSetting n = (NumberSetting) setting;
            return (n.get() - n.min()) / (n.max() - n.min());
          }

          public void setFraction(double f) {
            NumberSetting n = (NumberSetting) setting;
            double v = n.min() + Math.round(f * (n.max() - n.min()) / n.step()) * n.step();
            n.set(v);
            save.run();
          }

          public void revert() {}

          public String backend() {
            return "TFL ConfigStore: " + owner + "." + setting.key();
          }
        });
  }
}
