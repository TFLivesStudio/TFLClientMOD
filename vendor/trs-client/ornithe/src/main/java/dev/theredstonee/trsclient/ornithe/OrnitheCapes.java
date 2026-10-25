package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.cape.*;
import dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry.State;
import java.util.*;
import net.minecraft.client.entity.living.player.ClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerRenderer;
import net.minecraft.client.render.platform.GlStateManager;

/** Shared bounded cloth simulation, using only the cape texture already authorized by Vanilla. */
public final class OrnitheCapes {
  private final OrnitheClient c;
  public final CapePhysics physics = new CapePhysics();
  private final CapeSettings settings = new CapeSettings();
  private final ClothMesh mesh = new ClothMesh();
  private final List<CapePhysics.Sample> samples = new ArrayList<>();
  private Object world;
  public long rendered;

  public OrnitheCapes(OrnitheClient c) {
    this.c = c;
    c.capabilities.connect(
        c.modules.capePhysics,
        State.EXPERIMENTAL,
        "Shared bounded cloth/LOD and native cape layer. Uses existing Vanilla cape"
            + " ownership/resources only, no remote cosmetics. Menu player preview is not"
            + " connected.",
        c.modules.capePhysics.settings().stream().map(s -> s.key()).toList(),
        "OrnitheCapes.tick",
        "CapePhysics",
        "CapeLayerMixin",
        "ClothMesh");
  }

  public void tick() {
    if (world != c.mc.world) {
      world = c.mc.world;
      physics.clear();
    }
    samples.clear();
    boolean active =
        c.capabilities.active(c.modules.capePhysics) && c.mc.player != null && c.mc.world != null;
    if (active) {
      for (var e : c.mc.world.entities) {
        if (!(e instanceof ClientPlayerEntity p) || samples.size() >= 256) continue;
        double dx = p.x - c.mc.player.x, dy = p.y - c.mc.player.y, dz = p.z - c.mc.player.z;
        double distance = dx * dx + dy * dy + dz * dz;
        if (distance > 1600) continue;
        var s = new CapePhysics.Sample();
        s.id = p.getNetworkId();
        s.x = p.x;
        s.y = p.y;
        s.z = p.z;
        s.bodyYaw = p.bodyYaw;
        s.crouching = p.isSneaking();
        s.self = p == c.mc.player;
        s.distanceSq = distance;
        s.hasCape = hasCape(p);
        s.special = p.isSleeping();
        samples.add(s);
      }
    }
    physics.tick(
        samples,
        active,
        c.modules.capeScope.get()
            == dev.theredstonee.trsclient.core.module.TrsModules.CapeScope.OWN,
        c.modules.capeSettings(settings));
  }

  private boolean hasCape(ClientPlayerEntity p) {
    return p.hasInfo()
        && !p.isInvisible()
        && p.isModelPartVisible(net.minecraft.client.render.model.PlayerModelPart.CAPE)
        && p.getCapeTextureLocation() != null;
  }

  public boolean render(PlayerRenderer renderer, ClientPlayerEntity player) {
    var sim = physics.sim(player.getNetworkId());
    if (!c.capabilities.active(c.modules.capePhysics) || sim == null || !hasCape(player))
      return false;
    c.mc.getTextureManager().bind(player.getCapeTextureLocation());
    GlStateManager.color4f(1, 1, 1, 1);
    GlStateManager.pushMatrix();
    try {
      if (player.isSneaking()) GlStateManager.translatef(0, .2f, 0);
      renderer.getModel().body.transform(.0625f);
      float zOff = (player.inventory.armor[2] == null ? 2 : 3.1f) / 16;
      var tess = net.minecraft.client.render.vertex.Tesselator.getInstance();
      var buffer = tess.getBuffer();
      buffer.begin(
          org.lwjgl.opengl.GL11.GL_QUADS,
          net.minecraft.client.render.vertex.DefaultVertexFormat.POSITION_TEX_NORMAL);
      try {
        mesh.emit(
            sim,
            physics.partial(),
            physics.blocky(),
            (x, y, z, u, v, nx, ny, nz) ->
                buffer.vertex(x, y, z + zOff).texture(u, v).normal(nx, ny, nz).nextVertex());
      } finally {
        tess.end();
      }
      rendered++;
    } finally {
      GlStateManager.popMatrix();
    }
    return true;
  }
}
