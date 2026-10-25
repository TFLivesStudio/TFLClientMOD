package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.module.HudModule;
import dev.theredstonee.trsclient.core.pvp.*;
import dev.theredstonee.trsclient.core.qol.*;
import dev.theredstonee.trsclient.core.ui.*;
import dev.theredstonee.trsclient.core.ui.menu.HudItem;
import dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry.State;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.item.*;
import net.minecraft.world.WorldSettings;

/** Informational combat observation only: no reach, attack, damage or network modification. */
public final class OrnithePvp {
  private static final class LocalSound
      extends net.minecraft.client.sound.instance.AbstractSoundInstance {
    LocalSound(String id, float volume, float pitch) {
      super(new net.minecraft.resource.Identifier("minecraft:" + id));
      this.volume = Math.max(0, Math.min(1, volume));
      this.pitch = Math.max(.5f, Math.min(2, pitch));
      this.attenuation = net.minecraft.client.sound.instance.SoundInstance.Attenuation.NONE;
    }
  }

  private final OrnitheClient c;
  public final ReachTracker reach = new ReachTracker();
  public final ComboTracker combo = new ComboTracker();
  public final Qol qol;
  private final QolPanels.Where where = new QolPanels.Where();
  private final TextWidth measure;
  private int pending = Integer.MIN_VALUE;
  private long pendingAt;
  private Object world;
  private boolean dead;
  private final List<CounterRow> rows = new ArrayList<>(6);
  private String reachLabel = "", comboLabel = "";

  private record CounterRow(ItemStack stack, String label, int width) {}

  public OrnithePvp(OrnitheClient c) {
    this.c = c;
    measure = c.canvas::textWidth;
    qol =
        Qol.init(
            c.modules,
            new Qol.Platform() {
              public void play(QolSound sound, float volume, float pitch) {
                String id = sound.old == null ? QolSound.PLING.old : sound.old;
                c.mc.getSoundManager().play(new LocalSound(id, volume, pitch));
              }

              public boolean focused() {
                return org.lwjgl.opengl.Display.isActive();
              }

              public long windowHandle() {
                return 0;
              }
            });
    for (var m :
        List.of(
            c.modules.lowFire,
            c.modules.oldAnimations,
            c.modules.hitColor,
            c.modules.hitboxes,
            c.modules.blockOutline))
      c.capabilities.connect(
          m,
          State.EXPERIMENTAL,
          "Native cosmetic transform/tint hooks only; does not change attacks, cooldowns, damage or"
              + " packets",
          all(m),
          "ItemInHandMixin",
          "HitColorMixin",
          "HitboxMixin",
          "WorldDetailsMixin.renderBlockOutline");
    connect(c.modules.reach, all(c.modules.reach), "Observed eye-to-hit distance only");
    connect(
        c.modules.combo, all(c.modules.combo), "Confirmed native hurt events after a local attack");
    connect(
        c.modules.qol.hitFeedback,
        List.of("marker", "markerColor", "markerSize", "markerTime", "sound", "volume"),
        "Shared marker/sound on confirmed hits; extra particle emitters not connected");
    connect(
        c.modules.qol.warnings,
        all(c.modules.qol.warnings),
        "Shared threshold/cooldown warnings with real armor/food/health/inventory data and death"
            + " compass");
    var counter = new ArrayList<>(all(c.modules.qol.itemCounter));
    counter.remove("totems");
    counter.remove("totemPops");
    connect(c.modules.qol.itemCounter, counter, "Native inventory icons; 1.8.9 has no totems");
    c.hudItems.add(text(c.modules.reach, () -> reachLabel));
    c.hudItems.add(text(c.modules.combo, () -> comboLabel));
    c.hudItems.add(
        new HudItem() {
          public HudModule module() {
            return c.modules.qol.warnings;
          }

          private boolean preview() {
            return c.mc.world == null;
          }

          public int width() {
            return QolPanels.warningsWidth(
                qol, where, measure, preview(), System.currentTimeMillis());
          }

          public int height() {
            return QolPanels.warningsHeight(qol, where, preview(), System.currentTimeMillis());
          }

          public void draw(Canvas canvas) {
            QolPanels.drawWarnings(
                canvas,
                qol,
                where,
                measure,
                preview(),
                System.currentTimeMillis(),
                module().backgroundArgb(),
                module().shadow());
          }
        });
    c.hudItems.add(
        new HudItem() {
          public HudModule module() {
            return c.modules.qol.itemCounter;
          }

          private boolean horizontal() {
            return c.modules.qol.countLayout.get() == ItemCounter.Layout.HORIZONTAL;
          }

          public int width() {
            if (horizontal()) {
              int width = 6;
              for (var row : rows) width += Math.max(22, row.width + 4);
              return width;
            }
            int width = 30;
            for (var row : rows) width = Math.max(width, row.width + 27);
            return width;
          }

          public int height() {
            return rows.isEmpty() ? 1 : horizontal() ? 32 : rows.size() * 19 + 6;
          }

          public void draw(Canvas canvas) {
            if (rows.isEmpty()) return;
            canvas.fill(0, 0, width(), height(), module().backgroundArgb());
            int cursor = 3;
            for (var row : rows) {
              int x = horizontal() ? cursor : 3, y = horizontal() ? 3 : cursor;
              c.mc.getItemRenderer().renderGuiItem(row.stack, x, y);
              canvas.text(
                  row.label,
                  horizontal() ? x : x + 20,
                  horizontal() ? 21 : y + 4,
                  module().textColor.argb(),
                  module().shadow());
              cursor += horizontal() ? Math.max(22, row.width + 4) : 19;
            }
          }
        });
  }

  private java.util.List<String> all(dev.theredstonee.trsclient.core.module.Module m) {
    return m.settings().stream().map(s -> s.key()).toList();
  }

  private void connect(
      dev.theredstonee.trsclient.core.module.Module m, Collection<String> settings, String detail) {
    c.capabilities.connect(
        m,
        State.LIMITED,
        detail,
        settings,
        "OrnithePvp.tick",
        "AttackMixin.attackEntity",
        "LivingEntityMixin.doEvent",
        "QolPanels",
        "GameGuiMixin.render");
  }

  private HudItem text(HudModule m, java.util.function.Supplier<String> value) {
    return new HudItem() {
      public HudModule module() {
        return m;
      }

      public int width() {
        return c.canvas.textWidth(value.get()) + 10;
      }

      public int height() {
        return value.get().isEmpty() ? 1 : 18;
      }

      public void draw(Canvas canvas) {
        if (value.get().isEmpty()) return;
        canvas.fill(0, 0, width(), height(), m.backgroundArgb());
        canvas.text(value.get(), 5, 4, m.textColor.argb(), m.shadow());
      }
    };
  }

  public void attack(Entity target) {
    if (!c.ready || c.mc.player == null || target == null) return;
    long now = System.currentTimeMillis();
    pending = target.getNetworkId();
    pendingAt = now;
    combo.onAttack(pending, now);
    if (c.capabilities.active(c.modules.reach)) {
      var hit = c.mc.crosshairTarget;
      if (hit != null && hit.entity == target && hit.facePos != null)
        reach.record(c.mc.player.getEyePosition(1).distanceTo(hit.facePos), now);
    }
    c.emotes.attack();
  }

  public void event(Entity entity, byte event) {
    if (!c.ready || c.mc.player == null) return;
    long now = System.currentTimeMillis();
    if (event == 2) {
      if (entity == c.mc.player) combo.onSelfHurt();
      else if (entity.getNetworkId() == pending && now - pendingAt <= ComboTracker.CONFIRM_MS) {
        combo.onTargetHurt(pending, now);
        qol.onHitConfirmed(c.mc.player.fallDistance > 0, now);
        c.seasonal.hit(entity);
        pending = Integer.MIN_VALUE;
      }
    }
  }

  public void tick() {
    long now = System.currentTimeMillis();
    if (world != c.mc.world) {
      world = c.mc.world;
      reach.reset();
      combo.reset();
      pending = Integer.MIN_VALUE;
      qol.notices.clear();
      qol.hits.reset();
      dead = false;
    }
    qol.tick(c.mc.player == null ? "" : c.mc.player.getName(), now);
    combo.tick(now, (long) (c.modules.comboTimeout.get() * 1000));
    reachLabel =
        reach.valid(now, (long) (c.modules.reachHold.get() * 1000))
            ? ReachTracker.format(reach.distance(), c.modules.reachDecimals.getInt())
            : "";
    comboLabel =
        combo.combo() > 0
            ? "Combo "
                + combo.combo()
                + (c.modules.comboBest.get() ? " · Best " + combo.best() : "")
            : "";
    where.valid = c.mc.player != null;
    where.dimension = c.mapPlatform.dimension();
    if (c.mc.player == null) return;
    where.x = c.mc.player.x;
    where.z = c.mc.player.z;
    where.yaw = c.mc.player.yaw;
    boolean nowDead = c.mc.player.getHealth() <= 0;
    if (nowDead && !dead) {
      qol.onOwnDeath(now);
      if (c.modules.waypoints.isEnabled() && c.modules.waypointDeath.get()) {
        c.waypoints.setDeath(
            c.mapPlatform.worldKey(),
            (int) Math.floor(c.mc.player.x),
            (int) Math.floor(c.mc.player.y),
            (int) Math.floor(c.mc.player.z),
            c.mapPlatform.dimension(),
            0xE44C55);
        c.mapPlatform.waypointsChanged();
      }
    }
    dead = nowDead;
    if (c.capabilities.active(c.modules.qol.warnings)) {
      var input = qol.warnings.input().clear();
      input.alive = !nowDead;
      input.health = c.mc.player.getHealth();
      input.maxHealth = c.mc.player.getMaxHealth();
      input.food = c.mc.player.getHungerManager().getFoodLevel();
      input.inventoryFull = c.mc.player.inventory.getEmptySlot() < 0;
      var info = c.mc.getNetworkHandler().getOnlinePlayer(c.mc.player.getUuid());
      input.survival =
          info != null
              && (info.getGameMode() == WorldSettings.GameMode.SURVIVAL
                  || info.getGameMode() == WorldSettings.GameMode.ADVENTURE);
      for (int i = 0; i < 4; i++) {
        ItemStack stack = c.mc.player.inventory.armor[3 - i];
        if (stack != null && stack.isDamageable()) {
          input.armorDamage[i] = stack.getDamage();
          input.armorMax[i] = stack.getMaxDamage();
          input.armorName[i] = stack.getHoverName();
        }
      }
      ItemStack hand = c.mc.player.inventory.getSelectedItem();
      if (hand != null && hand.isDamageable()) {
        input.handDamage = hand.getDamage();
        input.handMax = hand.getMaxDamage();
        input.handName = hand.getHoverName();
      }
      qol.tickWarnings(now);
    }
    if (c.capabilities.active(c.modules.qol.itemCounter)) inventory();
    else rows.clear();
  }

  private boolean shown(ItemCounter.Kind kind) {
    var m = c.modules.qol;
    return switch (kind) {
      case ARROWS -> m.countArrows.get();
      case TOTEMS -> false;
      case HEALING -> m.countHealing.get();
      case SPLASH -> m.countSplash.get();
      case GAPPLES -> m.countGapples.get();
      case PEARLS -> m.countPearls.get();
      case BLOCKS -> m.countBlocks.get();
    };
  }

  private void inventory() {
    qol.counter.clear();
    for (ItemStack stack : c.mc.player.inventory.items) {
      if (stack == null) continue;
      var item = stack.getItem();
      Object key = Item.REGISTRY.getKey(item);
      boolean healing = false, splash = false;
      if (item instanceof PotionItem potion) {
        var effects = potion.getPotionEffects(stack);
        healing = effects != null && effects.stream().anyMatch(e -> e.getId() == 6);
        splash = PotionItem.isSplashPotion(stack.getDamage());
      }
      qol.counter.add(
          ItemCounter.classify(
              key == null ? "" : key.toString(), item instanceof BlockItem, healing, splash),
          stack.size,
          stack);
    }
    rows.clear();
    for (var kind : ItemCounter.Kind.values()) {
      if (!shown(kind)) continue;
      int count = qol.counter.count(kind);
      if (count == 0 && c.modules.qol.countHideEmpty.get()) continue;
      ItemStack icon = (ItemStack) qol.counter.icon(kind);
      if (icon == null) {
        Item item =
            Item.REGISTRY.get(new net.minecraft.resource.Identifier("minecraft:" + kind.iconItem));
        if (item == null) continue;
        icon = new ItemStack(item);
      }
      String label = Integer.toString(count);
      rows.add(new CounterRow(icon, label, c.canvas.textWidth(label)));
    }
  }

  public void draw(Canvas canvas, int width, int height) {
    QolPanels.drawHitmarker(canvas, qol, width, height, System.currentTimeMillis());
    if(!c.modules.qol.warnings.isEnabled()&&!qol.notices.isEmpty()){
      long now=System.currentTimeMillis();var module=c.modules.qol.warnings;int w=QolPanels.warningsWidth(qol,null,measure,false,now),h=QolPanels.warningsHeight(qol,null,false,now);
      canvas.push();canvas.translate(dev.theredstonee.trsclient.core.hud.HudLayout.resolveX(module.position(),w,width),dev.theredstonee.trsclient.core.hud.HudLayout.resolveY(module.position(),h,height));
      QolPanels.drawWarnings(canvas,qol,null,measure,false,now,0x90000000,true);canvas.pop();
    }
  }
}
