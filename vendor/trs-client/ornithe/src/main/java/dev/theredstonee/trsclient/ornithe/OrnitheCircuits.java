package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.circuit.*;
import dev.theredstonee.trsclient.core.ui.Canvas;
import dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry.State;
import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.state.property.Property;

/**
 * Shared local templates, previews, placement and checks; native reads never fetch unloaded chunks.
 */
public final class OrnitheCircuits implements Circuits.Platform, CircuitWorld {
  private final OrnitheClient c;
  private final Circuits.Context context = new Circuits.Context();

  public OrnitheCircuits(OrnitheClient c) {
    this.c = c;
    var dir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
    Circuits.get().install(c.modules, this, OrnitheKeys::isDown, dir, false);
    Thread load =
        new Thread(
            () -> {
              var cached =
                  new CircuitCache(dir.resolve("trsclient/circuits"))
                      .load(CircuitLibrary.blockCatalog());
              CircuitLibrary.publish(
                  cached.isEmpty() ? CircuitLibrary.loadResources("/circuits/", false) : cached);
            },
            "TFL-Local-Circuits");
    load.setDaemon(true);
    load.start();
    c.capabilities.connect(
        c.modules.circuits.circuitLibrary,
        State.EXPERIMENTAL,
        "Shared offline library/ghost templates, layers, rotation and block-state checks, with a"
            + " small bundled GPL recipe set and local cache. Remote submissions and downloads are"
            + " disabled.",
        c.modules.circuits.circuitLibrary.settings().stream().map(s -> s.key()).toList(),
        "OrnitheCircuits",
        "Circuits.Platform",
        "CircuitWorld",
        "GhostPainter",
        "CircuitLibraryPage");
  }

  public String minecraftVersion() {
    return "1.8.9";
  }

  public boolean legacy() {
    return true;
  }

  public Map<String, Integer> inventory() {
    Map<String, Integer> counts = new HashMap<>();
    if (c.mc.player != null)
      for (var stack : c.mc.player.inventory.items) {
        if (stack == null) continue;
        var id = net.minecraft.item.Item.REGISTRY.getKey(stack.getItem());
        if (id != null)
          counts.merge(id.toString().replace("minecraft:", ""), stack.size, Integer::sum);
      }
    return counts;
  }

  public Object stack(String id) {
    var item =
        net.minecraft.item.Item.REGISTRY.get(
            new net.minecraft.resource.Identifier(id.indexOf(':') >= 0 ? id : "minecraft:" + id));
    return item == null ? null : new net.minecraft.item.ItemStack(item);
  }

  public void openMenu() {
    CircuitLibraryPage.requestOpen();
    c.openMenu(c.mc.screen);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public String block(int x, int y, int z, Map<String, String> props) {
    if (!c.redstone.world.loaded(x, y, z)) return null;
    var pos = new net.minecraft.util.math.BlockPos(x, y, z);
    var state = c.mc.world.getBlockState(pos);
    state = state.getBlock().resolveVirtualProperties(state, c.mc.world, pos);
    for (Property property : state.properties())
      props.put(property.getName(), property.getName(state.get(property)));
    var id = Block.REGISTRY.getKey(state.getBlock());
    return id == null ? null : id.toString();
  }

  public boolean conductor(int x, int y, int z) {
    return c.redstone.world.conductor(x, y, z);
  }

  public void tick() {
    var p = c.mc.player;
    context.inWorld = c.mapPlatform.inWorld();
    context.singleplayer = c.mc.isSingleplayer();
    context.levelName = c.mc.getServer() == null ? null : c.mc.getServer().getWorldSaveName();
    var server = c.mc.getCurrentServerEntry();
    context.serverAddress = server == null ? null : server.ip;
    context.dimension = c.mapPlatform.dimension();
    context.screenOpen = c.mc.screen != null;
    var hit = c.mc.crosshairTarget;
    context.hit = hit != null && hit.type == net.minecraft.world.HitResult.Type.BLOCK;
    if (context.hit) {
      var pos = hit.getPos();
      context.hitX = pos.getX();
      context.hitY = pos.getY();
      context.hitZ = pos.getZ();
      context.face = hit.face.getId();
    }
    if (p != null) {
      context.x = p.x;
      context.y = p.y;
      context.z = p.z;
      context.yaw = p.yaw;
    }
    Circuits.get().tick(this, context);
  }

  public void draw(Canvas canvas, int width, int height) {
    if (!c.capabilities.active(c.modules.circuits.circuitLibrary)
        || c.mc.player == null
        || c.mc.options.perspective != 0
        || c.cinematic.detached()) return;
    var p = c.mc.player;
    Circuits.get()
        .draw(canvas, p.x, p.y + p.getEyeHeight(), p.z, p.yaw, p.pitch, c.worldFov, width, height);
  }
}
