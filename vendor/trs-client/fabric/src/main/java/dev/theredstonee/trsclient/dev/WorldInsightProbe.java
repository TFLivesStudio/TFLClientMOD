//? if >=26.3 {
/*package dev.theredstonee.trsclient.dev;
import dev.theredstonee.trsclient.TrsClient;
import dev.theredstonee.trsclient.core.insight.TargetInfo;
import dev.theredstonee.trsclient.hud.WorldInsightHud;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.*;
import net.minecraft.core.Direction;
final class WorldInsightProbe {
    static void run(Minecraft mc, TrsClient client) throws Exception {
        WorldInsightHud hud = new WorldInsightHud(client.modules());
        var field = WorldInsightHud.class.getDeclaredField("info"); field.setAccessible(true);
        var clock = WorldInsightHud.class.getDeclaredField("next"); clock.setAccessible(true);
        var original = mc.hitResult;
        try {
            var pos = mc.player.blockPosition().below();
            mc.hitResult = new BlockHitResult(mc.player.position(), Direction.UP, pos, false);
            hud.width(mc.font, false);
            TargetInfo block = (TargetInfo)field.get(hud);
            if (block == null || block.name.isEmpty() || !block.id.startsWith("minecraft:") || block.icon == null)
                throw new AssertionError("World Insight failed to inspect a received block");
            mc.hitResult = new EntityHitResult(mc.player);clock.setLong(hud, 0);hud.width(mc.font, false);
            TargetInfo player = (TargetInfo)field.get(hud);
            if (player == null || player.health != null || player.icon == null)
                throw new AssertionError("Player identity or hidden-health privacy regression");
            mc.hitResult = null;clock.setLong(hud, 0);hud.width(mc.font, false);
            if (field.get(hud) != null) throw new AssertionError("Miss retained a stale target");
        } finally { mc.hitResult = original; }
    }
}
*///?}
