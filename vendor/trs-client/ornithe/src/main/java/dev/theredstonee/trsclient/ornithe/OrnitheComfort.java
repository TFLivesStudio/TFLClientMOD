package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.hunger.*;
import dev.theredstonee.trsclient.core.tooltip.*;
import dev.theredstonee.trsclient.core.ui.Canvas;
import dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry.State;
import java.util.*;
import net.minecraft.item.*;

/** Native item metadata feeds the shared tooltip cards and the 1.8 hunger prediction rules. */
public final class OrnitheComfort {
  private final OrnitheClient c;
  private final HungerState state = new HungerState();
  private final HungerOverlay hunger = new HungerOverlay();
  private final HudIcons icons;
  private Object world;

  public OrnitheComfort(OrnitheClient c) {
    this.c = c;
    icons = HudIcons.sheet(c.canvas);
    Tooltips.get().init(c.modules.comfort);
    c.capabilities.connect(
        c.modules.comfort.tooltips,
        State.LIMITED,
        "Shared durability, food and filled-map tooltip cards. Shulker boxes do not exist in 1.8.9;"
            + " compact enchantments are not connected yet.",
        List.of("durability", "food", "map", "mapSize"),
        "TooltipMixin",
        "TooltipLinesMixin",
        "TooltipText",
        "Tooltips.draw",
        "FilledMapItem.getMapData");
    c.capabilities.connect(
        c.modules.comfort.hunger,
        State.LIMITED,
        "Shared saturation, held-food and legacy health prediction over Vanilla HUD. Exhaustion is"
            + " not sent by the server and is left unknown.",
        List.of("saturation", "heldFood", "health"),
        "OrnitheComfort.draw",
        "HungerOverlay",
        "HungerState.modernRegen=false");
  }

  public void tick() {
    if (world != c.mc.world) {
      world = c.mc.world;
      Tooltips.get().clearWorld();
    }
  }

  public void lines(ItemStack stack, List<String> lines) {
    if (!c.capabilities.active(c.modules.comfort.tooltips) || !stack.isDamageable()) return;
    String line =
        TooltipText.durabilityLine(
            c.modules.comfort.tipDurability.get(), stack.getMaxDamage(), stack.getDamage());
    if (line != null)
      lines.add("§" + TooltipText.colorCode(stack.getMaxDamage(), stack.getDamage()) + line);
  }

  public void tooltip(ItemStack stack, int x, int y) {
    if (!c.capabilities.active(c.modules.comfort.tooltips)) return;
    var tips = Tooltips.get();
    var card = tips.begin();
    if (card == null) return;
    if (stack.getItem() instanceof FoodItem food && tips.food()) {
      int points = food.getHungerPoints(stack);
      card.food(points, points * food.getSaturation(stack) * 2);
    }
    if (stack.getItem() instanceof FilledMapItem && tips.map() && c.mc.world != null) {
      var data = FilledMapItem.getMapData(stack.getDamage(), c.mc.world);
      tips.map(card, stack.getDamage(), data == null ? null : data.colors);
    }
    var lines = stack.getTooltip(c.mc.player, c.mc.options.advancedItemTooltips);
    int width = 0;
    for (String line : lines) width = Math.max(width, c.mc.textRenderer.getWidth(line));
    var win = c.window();
    c.canvas.begin(win.getWidth(), win.getHeight(), win.getScale());
    try {
      tips.draw(c.canvas, card, x, y, width, lines.size(), win.getWidth(), win.getHeight(), false);
    } finally {
      c.canvas.end();
    }
  }

  public void draw(Canvas canvas, int width, int height) {
    if (!c.capabilities.active(c.modules.comfort.hunger) || c.mc.player == null) return;
    var p = c.mc.player;
    var mode = c.mc.interactionManager.getGameMode();
    state.reset();
    state.survival =
        mode == net.minecraft.world.WorldSettings.GameMode.SURVIVAL
            || mode == net.minecraft.world.WorldSettings.GameMode.ADVENTURE;
    state.foodBar = !(p.vehicle instanceof net.minecraft.entity.living.LivingEntity);
    state.width = width;
    state.height = height;
    state.guiTicks = c.mc.gui.getTicks();
    state.food = p.getHungerManager().getFoodLevel();
    state.saturation = p.getHungerManager().getSaturationLevel();
    state.health = p.getHealth();
    state.maxHealth = p.getMaxHealth();
    state.absorption = p.getAbsorption();
    state.hardcore = c.mc.world.getData().isHardcore();
    state.hungerEffect = p.hasStatusEffect(17);
    state.regenEffect = p.hasStatusEffect(10);
    state.modernRegen = false;
    var hand = p.inventory.getSelectedItem();
    if (hand != null && hand.getItem() instanceof FoodItem food) {
      state.heldNutrition = food.getHungerPoints(hand);
      state.heldSaturation = state.heldNutrition * food.getSaturation(hand) * 2;
      state.heldCanEat =
          state.food < 20 || Item.REGISTRY.getKey(food).toString().equals("minecraft:golden_apple");
    }
    hunger.showSaturation = c.modules.comfort.hungerSaturation.get();
    hunger.showHeldFood = c.modules.comfort.hungerHeldFood.get();
    hunger.showHealth = c.modules.comfort.hungerHealth.get();
    hunger.showExhaustion = false;
    hunger.draw(canvas, state, icons, System.currentTimeMillis());
  }
}
