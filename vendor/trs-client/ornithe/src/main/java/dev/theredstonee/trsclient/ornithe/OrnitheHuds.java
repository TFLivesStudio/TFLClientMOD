package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.format.HudFormat;
import dev.theredstonee.trsclient.core.hud.ArmorLayout;
import dev.theredstonee.trsclient.core.module.HudModule;
import dev.theredstonee.trsclient.core.pvp.SpeedTracker;
import dev.theredstonee.trsclient.core.ui.Canvas;
import dev.theredstonee.trsclient.core.ui.menu.HudItem;
import dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry.State;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.item.ItemStack;

/**
 * Legacy HUD presentation adapted to Feather. Samples are cached on ticks, not computed in draw.
 */
public final class OrnitheHuds {
  private final OrnitheClient c;
  private final List<TextWidget> texts = new ArrayList<>();
  private final SpeedTracker speed = new SpeedTracker();
  private final Armor armor;

  public OrnitheHuds(OrnitheClient c) {
    this.c = c;
    add(
        c.modules.coords,
        () -> {
          if (c.mc.player == null) return List.of("X 0  Y 64  Z 0");
          var lines = new ArrayList<String>(3);
          lines.add(HudFormat.coords(c.mc.player.x, c.mc.player.y, c.mc.player.z));
          if (c.modules.coordsDirection.get()) lines.add(HudFormat.directionName(c.mc.player.yaw));
          if (c.modules.coordsBiome.get()) lines.add(c.mapPlatform.biome());
          return lines;
        });
    add(
        c.modules.clock,
        () ->
            List.of(
                HudFormat.clock(
                    java.time.LocalTime.now(),
                    c.modules.clockSeconds.get(),
                    c.modules.clockTwelveHour.get())));
    add(
        c.modules.memory,
        () -> {
          Runtime r = Runtime.getRuntime();
          return List.of(
              "RAM " + HudFormat.memory(r.totalMemory() - r.freeMemory(), r.maxMemory()));
        });
    add(c.modules.speed, () -> List.of(SpeedTracker.format(speed.blocksPerSecond())));
    var pingPanel = new dev.theredstonee.trsclient.core.net.PingPanel(c.modules);
    var pingSettings =
        new ArrayList<>(c.modules.ping.settings().stream().map(setting -> setting.key()).toList());
    pingSettings.remove("interval");
    connect(
        c.modules.ping,
        pingSettings,
        "Shared tab latency, keepalive jitter, history, spike warning and TPS estimate. 1.8.9 has"
            + " no active ping-request packet; interval is preserved but not exposed.");
    c.hudItems.add(
        new HudItem() {
          public HudModule module() {
            return c.modules.ping;
          }

          private boolean preview() {
            return c.mc.world == null
                || c.mc.screen instanceof CoreScreen cs
                    && cs.ui instanceof dev.theredstonee.trsclient.core.ui.menu.HudEditor;
          }

          public int width() {
            return pingPanel.width(c.canvas::textWidth, preview());
          }

          public int height() {
            return pingPanel.height(preview());
          }

          public void draw(Canvas canvas) {
            if (preview() || pingPanel.visible())
              pingPanel.draw(canvas, c.canvas::textWidth, preview());
          }
        });

    add(
        c.modules.server,
        () -> {
          var s = c.mc.getCurrentServerEntry();
          return c.mc.isSingleplayer() || s == null ? List.of() : List.of(s.ip);
        });
    add(
        c.modules.packs,
        () -> c.mc.getResourcePacks().getSelectedPacks().stream().map(p -> p.getName()).toList());
    add(
        c.modules.effects,
        () ->
            c.mc.player == null
                ? List.of()
                : c.mc.player.getStatusEffects().stream()
                    .map(
                        e ->
                            net.minecraft.locale.I18n.translate(e.getName())
                                + " "
                                + HudFormat.level(e.getAmplifier())
                                + " "
                                + HudFormat.duration(e.getDuration(), false))
                    .toList());
    add(
        c.modules.toggleSneak,
        () -> List.of(c.sneak.active() ? "Sneak ON" : "Sneak OFF"),
        Set.of("textColor", "shadow", "background", "backgroundOpacity", "scale"),
        "Native edge-triggered sneak; resets on GUI/world change");
    armor = new Armor();
    c.hudItems.add(armor);
    connect(
        c.modules.armor,
        c.modules.armor.settings().stream().map(s -> s.key()).toList(),
        "Native item icons with shared ArmorLayout and durability formatting");
  }

  private void add(HudModule m, Supplier<List<String>> supplier) {
    add(
        m,
        supplier,
        new HashSet<>(m.settings().stream().map(s -> s.key()).toList()),
        "Native tick-cached HUD data");
  }

  private void add(
      HudModule m, Supplier<List<String>> supplier, Set<String> settings, String note) {
    TextWidget w = new TextWidget(m, supplier);
    texts.add(w);
    c.hudItems.add(w);
    connect(m, settings, note);
  }

  private void connect(HudModule m, Collection<String> settings, String note) {
    c.capabilities.connect(
        m,
        State.LIMITED,
        note,
        settings,
        "OrnitheHuds.tick",
        "GameGuiMixin.render",
        "HudLayout",
        "HudEditor");
  }

  public void tick() {
    if (c.mc.player != null && c.capabilities.active(c.modules.speed))
      speed.tick(c.mc.player.x, c.mc.player.y, c.mc.player.z, c.modules.speedVertical.get());
    else speed.reset();
    boolean editor =
        c.mc.screen instanceof CoreScreen screen
            && screen.ui instanceof dev.theredstonee.trsclient.core.ui.menu.HudEditor;
    for (TextWidget text : texts) if (c.capabilities.active(text.module) || editor) text.refresh();
    if (c.capabilities.active(c.modules.armor) || editor) armor.refresh(editor);
  }

  private final class TextWidget implements HudItem {
    private final HudModule module;
    private final Supplier<List<String>> supplier;
    private List<String> lines = List.of();
    private int width = 70;

    TextWidget(HudModule m, Supplier<List<String>> supplier) {
      module = m;
      this.supplier = supplier;
    }

    void refresh() {
      lines = supplier.get();
      width = 0;
      for (String line : lines) width = Math.max(width, c.canvas.textWidth(line));
      width += 10;
    }

    public HudModule module() {
      return module;
    }

    public int width() {
      return width;
    }

    public int height() {
      return Math.max(1, lines.size()) * 11 + 8;
    }

    public void draw(Canvas canvas) {
      if (lines.isEmpty()) {
        if (c.mc.world == null)
          canvas.text(module.name(), 5, 4, module.textColor.argb(), module.shadow());
        return;
      }
      canvas.fill(0, 0, width(), height(), module.backgroundArgb());
      for (int i = 0; i < lines.size(); i++)
        canvas.text(lines.get(i), 5, 4 + i * 11, module.textColor.argb(), module.shadow());
    }
  }

  private final class Armor implements HudItem {
    private final ItemStack[] stacks = new ItemStack[5];
    private final String[] labels = new String[5];
    private final int[] widths = new int[5], positions = new int[20], colors = new int[5];
    private int count;

    public HudModule module() {
      return c.modules.armor;
    }

    private boolean horizontal() {
      return c.modules.armorLayout.get() == ArmorLayout.Orientation.HORIZONTAL;
    }

    void refresh(boolean preview) {
      count = 0;
      if (c.mc.player == null) return;
      for (int i = 0; i < (c.modules.armorHand.get() ? 5 : 4); i++) {
        ItemStack stack =
            i == 4 ? c.mc.player.inventory.getSelectedItem() : c.mc.player.inventory.armor[3 - i];
        if (stack == null) continue;
        stacks[count] = stack;
        labels[count] = null;
        widths[count] = 0;
        if (c.modules.armorDurability.get() && stack.isDamageable()) {
          int pct = HudFormat.durabilityPercent(stack.getDamage(), stack.getMaxDamage());
          labels[count] =
              c.modules.armorPercent.get()
                  ? pct + " %"
                  : Integer.toString(stack.getMaxDamage() - stack.getDamage());
          widths[count] = c.canvas.textWidth(labels[count]);
          colors[count] = 0xFF000000 | HudFormat.durabilityColor(pct);
        }
        count++;
      }
    }

    public int width() {
      return ArmorLayout.width(horizontal(), count, widths);
    }

    public int height() {
      return ArmorLayout.height(horizontal(), count, widths);
    }

    public void draw(Canvas canvas) {
      if (count == 0) return;
      canvas.fill(0, 0, width(), height(), module().backgroundArgb());
      ArmorLayout.place(horizontal(), count, widths, positions);
      for (int i = 0; i < count; i++) {
        c.mc.getItemRenderer().renderGuiItem(stacks[i], positions[4 * i], positions[4 * i + 1]);
        if (labels[i] != null)
          canvas.text(
              labels[i], positions[4 * i + 2], positions[4 * i + 3], colors[i], module().shadow());
      }
    }
  }
}
