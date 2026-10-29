package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.ui.graphics.GraphicsUi;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.client.gui.screen.Screen;

/** Optional Celeritas/Argentum binding. Never writes a provider's configuration file. */
public final class OrnitheGraphics implements GraphicsUi.Host {
  private static final Map<String, Method> METHODS = new java.util.concurrent.ConcurrentHashMap<>();
  private static final Map<String, Field> FIELDS = new java.util.concurrent.ConcurrentHashMap<>();
  private final OrnitheClient client;
  private final Screen nativeScreen;
  private final Object controller, context;
  private final List<GraphicsUi.Option> options = new ArrayList<>();
  private final Map<String, Object> byId = new LinkedHashMap<>();
  private Class<?> shaders, amplio;
  private Field requested, active;

  private OrnitheGraphics(OrnitheClient client, Screen screen) {
    this.client = client;
    nativeScreen = screen;
    controller = field(screen, "controller");
    context = field(screen, "context");
    for (Object page : (List<?>) field(controller, "pages"))
      for (Object option : (List<?>) call(page, "getOptions")) {
        Object id = call(option, "getId");
        String key = id.toString();
        if (byId.putIfAbsent(key, option) == null) options.add(new Bound(option, key));
      }
    dev.theredstonee.trsclient.core.ui.graphics.TflGraphicsOptions.add(
        options, client.modules, client::save, client.capabilities.runnable(client.modules.colors));
    String gpu = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER);
    String driver = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VERSION);
    options.add(
        new GraphicsUi.Option() {
          public String id() {
            return "tflcore:gpu_info";
          }

          public String name() {
            return "GPU";
          }

          public String description() {
            return gpu + " · " + driver;
          }

          public String value() {
            return gpu;
          }

          public int category() {
            return 4;
          }

          public boolean available() {
            return false;
          }

          public boolean changed() {
            return false;
          }

          public void step(int n) {}

          public void revert() {}
        });
    try {
      shaders = Class.forName("us.z1x.lucerna.gui.screen.ShaderPackScreen");
    } catch (ClassNotFoundException absent) {
    }
    try {
      amplio = Class.forName("us.z1x.amplio.Amplio");
      Object config = amplio.getField("CONFIG").get(null);
      requested = config.getClass().getField("upscaler");
      active = amplio.getDeclaredField("active");
      active.setAccessible(true);
    } catch (ReflectiveOperationException absent) {
      amplio = null;
    }
  }

  public static boolean open(OrnitheClient client, Screen screen) {
    if (!screen.getClass().getName().equals("dev.rdh.argentum.impl.gui.VideoOptionsScreen"))
      return false;
    try {
      OrnitheGraphics host = new OrnitheGraphics(client, screen);
      client.mc.openScreen(new CoreScreen(new GraphicsUi(host), false));
      return true;
    } catch (RuntimeException error) {
      OrnitheClient.LOG.warn(
          "Unified graphics binding unavailable; keeping native controls", error);
      return false;
    }
  }

  public List<GraphicsUi.Option> options() {
    return options;
  }

  public void apply() {
    call(controller, "applyChanges");
  }

  public void revert(){call(controller,"undoChanges");}

  public void close() {
    client.mc.openScreen((Screen) field(nativeScreen, "parent"));
  }

  public boolean hasShaders() {
    return shaders != null;
  }

  public void shaders() {
    try {
      client.mc.openScreen(
          (Screen) shaders.getConstructor(Screen.class).newInstance(client.mc.screen));
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  public String status() {
    if (amplio == null) return "";
    try {
      Object config = amplio.getField("CONFIG").get(null);
      Object wanted = requested.get(config), running = active.get(null);
      return "Escalado solicitado: "
          + wanted
          + " · Activo: "
          + running
          + (wanted.toString().equals("DLSS") && running.toString().equals("FSR1")
              ? " (fallback)"
              : "");
    } catch (ReflectiveOperationException e) {
      return "No se pudo consultar el escalador activo";
    }
  }

  private boolean dlssRejected() {
    return amplio != null && Boolean.TRUE.equals(fieldValueStatic(amplio, "dlssFailed"));
  }

  private static Object fieldValueStatic(Class<?> type, String name) {
    try {
      Field f = type.getDeclaredField(name);
      f.setAccessible(true);
      return f.get(null);
    } catch (ReflectiveOperationException e) {
      return false;
    }
  }

  private String text(Object component) {
    return String.valueOf(invoke(context, "extractString", component));
  }

  private final class Bound implements GraphicsUi.Option {
    private final Object option, control;
    private final String id, name, description;
    private final int category;

    Bound(Object option, String id) {
      this.option = option;
      this.id = id;
      control = call(option, "getControl");
      name = text(call(option, "getName"));
      description = text(call(option, "getTooltip"));
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
      return id.equals("amplio:upscaler")
          ? description
              + " · DLSS requires compatible NVIDIA hardware and OpenGL/Vulkan memory sharing. FSR1"
              + " is used if the backend rejects it."
          : description;
    }

    public int category() {
      return category;
    }

    public boolean available() {
      return Boolean.TRUE.equals(call(option, "isAvailable"))
          && (!id.equals("amplio:dlss_model") || !dlssRejected());
    }

    public boolean changed() {
      return Boolean.TRUE.equals(call(option, "hasChanged"));
    }

    public String value() {
      Object value = call(option, "getValue");
      if (value instanceof Boolean)
        return (Boolean) value
            ? dev.theredstonee.trsclient.core.i18n.I18n.tr("graphics.on")
            : dev.theredstonee.trsclient.core.i18n.I18n.tr("graphics.off");
      if (control.getClass().getSimpleName().equals("CyclingControl")) {
        Object[] values = (Object[]) field(control, "allowedValues"),
            names = (Object[]) call(control, "getNames");
        for (int i = 0; i < values.length; i++)
          if (Objects.equals(value, values[i])) return text(names[i]);
      }
      if (control.getClass().getSimpleName().equals("SliderControl"))
        return text(invoke(field(control, "mode"), "format", value));
      return String.valueOf(value);
    }

    public void step(int direction) {
      Object value = call(option, "getValue"), next;
      if (value instanceof Boolean) next = !(Boolean) value;
      else if (control.getClass().getSimpleName().equals("CyclingControl")) {
        Object[] values = (Object[]) field(control, "allowedValues");
        int index = Arrays.asList(values).indexOf(value);
        int nextIndex = Math.floorMod(index + direction, values.length);
        if (id.equals("amplio:upscaler")
            && dlssRejected()
            && values[nextIndex].toString().equals("DLSS"))
          nextIndex = Math.floorMod(nextIndex + direction, values.length);
        next = values[nextIndex];
      } else if (control.getClass().getSimpleName().equals("SliderControl")) {
        int min = (Integer) field(control, "min"),
            max = (Integer) field(control, "max"),
            interval = (Integer) field(control, "interval");
        next = Math.max(min, Math.min(max, (Integer) value + direction * interval));
      } else throw new IllegalStateException("Control no compatible; conserva el valor original");
      invoke(option, "setValue", next);
    }

    public boolean slider() {
      return control.getClass().getSimpleName().equals("SliderControl");
    }

    public double fraction() {
      return ((Integer) call(option, "getValue") - (Integer) field(control, "min"))
          / (double) ((Integer) field(control, "max") - (Integer) field(control, "min"));
    }

    public void setFraction(double f) {
      int min = (Integer) field(control, "min"),
          max = (Integer) field(control, "max"),
          interval = (Integer) field(control, "interval");
      int n = min + (int) Math.round(f * (max - min) / interval) * interval;
      invoke(option, "setValue", Math.max(min, Math.min(max, n)));
    }

    public String backend() {
      return call(option, "getStorage").getClass().getName();
    }

    public void revert() {
      call(option, "reset");
    }
  }

  private static Object field(Object target, String name) {
    String key = target.getClass().getName() + "#" + name;
    try {
      Field f = FIELDS.get(key);
      if (f == null) {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass())
          try {
            f = type.getDeclaredField(name);
            f.setAccessible(true);
            FIELDS.put(key, f);
            break;
          } catch (NoSuchFieldException ignored) {
          }
      }
      if (f == null) throw new IllegalStateException("Missing provider field: " + name);
      return f.get(target);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  private static Object call(Object target, String name) {
    return invoke(target, name);
  }

  private static Object invoke(Object target, String name, Object... args) {
    String key = target.getClass().getName() + "#" + name + "/" + args.length;
    try {
      Method m = METHODS.get(key);
      if (m == null) {
        for (Class<?> type = target.getClass();
            type != null && m == null;
            type = type.getSuperclass())
          for (Method candidate : type.getDeclaredMethods())
            if (candidate.getName().equals(name) && candidate.getParameterCount() == args.length) {
              m = candidate;
              break;
            }
        if (m == null)
          for (Method candidate : target.getClass().getMethods())
            if (candidate.getName().equals(name) && candidate.getParameterCount() == args.length) {
              m = candidate;
              break;
            }
        if (m == null) throw new IllegalStateException("Missing provider operation: " + name);
        m.setAccessible(true);
        METHODS.put(key, m);
      }
      return m.invoke(target, args);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Provider operation failed: " + name, e);
    }
  }
}
