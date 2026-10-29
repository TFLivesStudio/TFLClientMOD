//? if >=26.3 {
/*package dev.theredstonee.trsclient.screen;

import dev.theredstonee.trsclient.core.ui.graphics.GraphicsUi;
import java.lang.reflect.*;
import java.util.*;

// Sodium Config owns values, validation, persistence and reload callbacks.
public final class SodiumGraphics implements GraphicsUi.Host {
  private final Object screen, config;
  private final List<GraphicsUi.Option> options = new ArrayList<>();

  public SodiumGraphics(Object screen) {
    this.screen = screen;
    try {
      config =
          Class.forName("net.caffeinemc.mods.sodium.client.config.ConfigManager")
              .getField("CONFIG")
              .get(null);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
    Set<Object> ids = new HashSet<>();
    for (Object mod : (List<?>) call(config, "getModOptions"))
      for (Object page : (List<?>) call(mod, "pages")) {
        Object groups;
        try {
          groups = call(page, "groups");
        } catch (RuntimeException externalPage) {
          continue;
        }
        for (Object group : (List<?>) groups)
          for (Object option : (List<?>) call(group, "options")) {
            Object id = field(option, "id");
            if (ids.add(id)) options.add(new Bound(option, id.toString()));
          }
      }
  }

  private boolean sharedInstalled;

  public List<GraphicsUi.Option> options() {
    if (!sharedInstalled) {
      sharedInstalled = true;
      var c = dev.theredstonee.trsclient.TrsClient.get();
      dev.theredstonee.trsclient.core.ui.graphics.TflGraphicsOptions.add(
          options,
          c.modules(),
          c::saveConfig,
          dev.theredstonee.trsclient.render.ColorPass.supported());
    }
    return options;
  }

  public void apply() {
    call(config, "applyAllOptions");
  }

  public void revert() {
    call(config, "resetAllOptionsFromBindings");
  }

  public void close() {
    ((net.minecraft.client.gui.screens.Screen) screen).onClose();
  }

  public boolean hasShaders() {
    return false;
  }

  public void shaders() {}

  public String status() {
    return "";
  }

  private static String text(Object component) {
    return component instanceof net.minecraft.network.chat.Component c
        ? c.getString()
        : String.valueOf(component);
  }

  private final class Bound implements GraphicsUi.Option {
    private final Object option;
    private final String id, name, description;
    private final int category;
    private final boolean stateful;

    Bound(Object o, String id) {
      option = o;
      this.id = id;
      name = text(call(o, "getName"));
      description = text(call(o, "getTooltip"));
      stateful = has(o, "getValidatedValue");
      String key = id.toLowerCase(Locale.ROOT);
      category = dev.theredstonee.trsclient.core.ui.graphics.GraphicsPurpose.category(id);
    }

    public String id() {
      return id;
    }

    public String name() {
      return name;
    }

    public String description() {
      return description;
    }

    public int category() {
      return category;
    }

    public boolean available() {
      return (stateful || has(option, "getCurrentScreenConsumer"))
          && Boolean.TRUE.equals(call(option, "isEnabled"));
    }

    public boolean changed() {
      return Boolean.TRUE.equals(call(option, "hasChanged"));
    }

    public String value() {
      if (!stateful) return has(option, "getCurrentScreenConsumer") ? "Open" : "Information";
      Object v = call(option, "getValidatedValue");
      if (v instanceof Boolean b)
        return b
            ? dev.theredstonee.trsclient.core.i18n.I18n.tr("graphics.on")
            : dev.theredstonee.trsclient.core.i18n.I18n.tr("graphics.off");
      if (v instanceof Enum) return text(invoke(option, "getElementName", v));
      if (v instanceof Integer) return text(invoke(option, "formatValue", v));
      return String.valueOf(v);
    }

    public void step(int direction) {
      if (has(option, "getCurrentScreenConsumer")) {
        if (Boolean.TRUE.equals(call(config, "anyOptionChanged")))
          throw new IllegalStateException("Apply pending changes first");
        ((java.util.function.Consumer<net.minecraft.client.gui.screens.Screen>)
                call(option, "getCurrentScreenConsumer"))
            .accept(dev.theredstonee.trsclient.compat.Mc.screen());
        return;
      }
      Object v = call(option, "getValidatedValue"), next;
      if (v instanceof Boolean b) next = !b;
      else if (v instanceof Integer i) {
        Object validator = call(option, "getSteppedValidator");
        int min = (Integer) call(validator, "min"),
            max = (Integer) call(validator, "max"),
            step = (Integer) call(validator, "step");
        next = Math.max(min, Math.min(max, i + direction * step));
      } else if (v instanceof Enum e) {
        Object[] values = e.getDeclaringClass().getEnumConstants();
        int index = e.ordinal();
        next = v;
        for (int i = 1; i <= values.length; i++) {
          Object candidate = values[Math.floorMod(index + direction * i, values.length)];
          if (Boolean.TRUE.equals(invoke(option, "isValueAllowed", candidate))) {
            next = candidate;
            break;
          }
        }
      } else throw new IllegalStateException("Unsupported option type");
      invoke(option, "modifyValue", next);
    }

    public boolean slider() {
      return stateful && has(option, "getSteppedValidator");
    }

    public double fraction() {
      Object v = call(option, "getSteppedValidator");
      int min = (Integer) call(v, "min"), max = (Integer) call(v, "max");
      return ((Integer) call(option, "getValidatedValue") - min) / (double) (max - min);
    }

    public void setFraction(double f) {
      Object v = call(option, "getSteppedValidator");
      int min = (Integer) call(v, "min"),
          max = (Integer) call(v, "max"),
          step = (Integer) call(v, "step");
      int next = min + (int) Math.round(f * (max - min) / step) * step;
      invoke(option, "modifyValue", Math.max(min, Math.min(max, next)));
    }

    public void revert() {
      if (stateful) call(option, "resetFromBinding");
    }
  }

  private static boolean has(Object o, String name) {
    for (Method m : o.getClass().getMethods()) if (m.getName().equals(name)) return true;
    return false;
  }

  private static Object field(Object o, String name) {
    for (Class<?> t = o.getClass(); t != null; t = t.getSuperclass())
      try {
        Field f = t.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
      } catch (NoSuchFieldException ignored) {
      } catch (ReflectiveOperationException e) {
        throw new IllegalStateException(e);
      }
    throw new IllegalStateException(name);
  }

  private static Object call(Object o, String name) {
    return invoke(o, name);
  }

  private static Object invoke(Object o, String name, Object... args) {
    for (Class<?> t = o.getClass(); t != null; t = t.getSuperclass())
      for (Method m : t.getDeclaredMethods())
        if (m.getName().equals(name) && m.getParameterCount() == args.length)
          try {
            m.setAccessible(true);
            return m.invoke(o, args);
          } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(name, e);
          }
    throw new IllegalStateException(name);
  }
}
*///?}
