package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.ui.graphics.*;
import dev.theredstonee.trsclient.ornithe.mixin.VideoOptionsAccess;
import java.util.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.menu.options.VideoOptionsScreen;
import net.minecraft.client.options.GameOptions;

/** Vanilla fallback uses its existing validated option setters and their real callbacks. */
public final class VanillaGraphics implements GraphicsUi.Host {
  private final OrnitheClient client;
  private final Screen parent;
  private final List<GraphicsUi.Option> options = new ArrayList<>();

  public VanillaGraphics(OrnitheClient c, VideoOptionsScreen source) {
    client = c;
    parent = ((VideoOptionsAccess) source).tfl$parent();
    for (GameOptions.Option o : VideoOptionsAccess.tfl$options()) options.add(new Bound(o));
    TflGraphicsOptions.add(options, c.modules, c::save, c.capabilities.runnable(c.modules.colors));
  }

  public List<GraphicsUi.Option> options() {
    return options;
  }

  public void apply() {
    client.mc.options.save();
  }

  public void close() {
    client.mc.options.save();
    client.mc.openScreen(parent);
  }

  public boolean hasShaders() {
    return false;
  }

  public void shaders() {}

  public String status() {
    return "";
  }

  private final class Bound implements GraphicsUi.Option {
    private final GameOptions.Option option;

    Bound(GameOptions.Option option) {
      this.option = option;
    }

    public String id() {
      return "minecraft:" + option.name().toLowerCase(Locale.ROOT);
    }

    public int category() {
      return GraphicsPurpose.category(id());
    }

    public String name() {
      String text = client.mc.options.getAsString(option);
      int i = text.indexOf(':');
      return i < 0 ? text : text.substring(0, i);
    }

    public String description() {
      return dev.theredstonee.trsclient.core.i18n.I18n.tr("graphics.immediate");
    }

    public String value() {
      String text = client.mc.options.getAsString(option);
      int i = text.indexOf(':');
      return i < 0 ? text : text.substring(i + 1).trim();
    }

    public boolean available() {
      return true;
    }

    public boolean changed() {
      return false;
    }

    public void revert() {}

    public String backend() {
      return "Minecraft GameOptions: " + option.name();
    }

    private Object profileValue() {
      if(option.isFloat()) return client.mc.options.getFloat(option);
      return switch(option.name()) {
        case "GRAPHICS" -> client.mc.options.fancyGraphics;
        case "RENDER_CLOUDS" -> client.mc.options.cloudRenderMode;
        case "PARTICLES" -> client.mc.options.particles;
        case "AMBIENT_OCCLUSION" -> client.mc.options.ambientOcclusion;
        default -> null;
      };
    }
    public GraphicsProfiles.Capability capability() {
      if(profileValue()==null) return null;
      return new GraphicsProfiles.Capability() {
        public Object read() { return profileValue(); }
        public boolean accepts(Object value) {
          Object current=read();
          if(current instanceof Float && value instanceof Float n)
            return Float.isFinite(n) && Math.abs(option.denormalize(option.normalize(n))-n)<.001f;
          if(current instanceof Boolean) return value instanceof Boolean;
          return current instanceof Integer && value instanceof Integer n && n>=0 && n<=2;
        }
        public void write(Object value) {
          if(!accepts(value)) throw new IllegalArgumentException(id());
          if(option.isFloat()) client.mc.options.set(option,(Float)value);
          else {
            for(int i=0;i<4 && !Objects.equals(read(),value);i++) client.mc.options.set(option,1);
            if(!Objects.equals(read(),value)) throw new IllegalStateException(id());
          }
        }
      };
    }

    public boolean slider() {
      return option.isFloat();
    }

    public double fraction() {
      return option.normalize(client.mc.options.getFloat(option));
    }

    public void setFraction(double f) {
      client.mc.options.set(option, option.denormalize((float) Math.max(0, Math.min(1, f))));
    }

    public void endSlide() {
      client.mc.options.save();
    }

    public void step(int direction) {
      if (option.isFloat()) {
        setFraction(fraction() + direction * .05);
        endSlide();
      } else {
        client.mc.options.set(option, direction);
        client.mc.options.save();
        if (option == GameOptions.Option.GUI_SCALE)
          client.mc.openScreen(new CoreScreen(new GraphicsUi(VanillaGraphics.this), false));
      }
    }
  }
}
