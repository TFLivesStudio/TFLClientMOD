package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.inventory.AutoRefill;
import dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry.State;

/**
 * Same inventory swap protocol as Vanilla; disabled without explicit platform approval or in Fair
 * Play.
 */
public final class OrnitheRefill implements AutoRefill.Game {
  private final OrnitheClient c;
  private final AutoRefill refill = new AutoRefill();
  private Object world;

  public OrnitheRefill(OrnitheClient c) {
    this.c = c;
    c.capabilities.connect(
        c.modules.autoRefill,
        State.EXPERIMENTAL,
        "Replenishes an emptied selected slot using a normal inventory swap. Survival/adventure"
            + " only, no GUI, no Fair Play. Old enabled flags require an explicit off/on toggle"
            + " here.",
        java.util.List.of(),
        "OrnitheRefill.tick",
        "AutoRefill.Game",
        "ClientPlayerInteractionManager.clickSlot",
        "OrnitheConsent");
  }

  public boolean active() {
    if (!c.automation.allowed(c.modules.autoRefill)
        || c.mc.screen != null
        || c.map.fairPlay().active()
        || c.mc.player.menu != c.mc.player.playerMenu) return false;
    var mode = c.mc.interactionManager.getGameMode();
    return mode == net.minecraft.world.WorldSettings.GameMode.SURVIVAL
        || mode == net.minecraft.world.WorldSettings.GameMode.ADVENTURE;
  }

  public int selectedSlot() {
    return c.mc.player.inventory.selectedSlot;
  }

  public String itemKey(int slot) {
    var stack = c.mc.player.inventory.items[slot];
    if (stack == null || stack.size <= 0) return null;
    return net.minecraft.item.Item.REGISTRY.getKey(stack.getItem())
        + ":"
        + (stack.isDamageable() ? 0 : stack.getDamage())
        + ":"
        + (stack.hasNbt() ? stack.getNbt().toString() : "");
  }

  public void swap(int from, int toHotbar) {
    if (active())
      c.mc.interactionManager.clickSlot(
          c.mc.player.playerMenu.networkId, from < 9 ? 36 + from : from, toHotbar, 2, c.mc.player);
  }

  public void tick() {
    if (world != c.mc.world) {
      world = c.mc.world;
      refill.reset();
    }
    refill.tick(this);
  }
}
