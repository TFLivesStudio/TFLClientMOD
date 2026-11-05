package dev.theredstonee.trsclient.ornithe.dev;

import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import dev.theredstonee.trsclient.core.insight.TargetInfo;
import net.minecraft.world.HitResult;
import net.minecraft.util.math.*;

/** Native fixture only; excluded from distributed jars. */
final class WorldInsightProbe {
    static void run(OrnitheClient c) throws Exception {
        Class<?> type = Class.forName("dev.theredstonee.trsclient.ornithe.OrnitheWorldInsight");
        var constructor = type.getDeclaredConstructor(OrnitheClient.class); constructor.setAccessible(true);
        Object hud = constructor.newInstance(c);
        var refresh = type.getDeclaredMethod("refresh"); refresh.setAccessible(true);
        var clock = type.getDeclaredField("next"); clock.setAccessible(true);
        var field = type.getDeclaredField("info"); field.setAccessible(true);
        var original = c.mc.crosshairTarget;
        try {
            // The fixture may reopen a save with its creative player still flying. Select a
            // solid block from the actual received column rather than assuming a floor at y-1.
            int x=(int)Math.floor(c.mc.player.x),z=(int)Math.floor(c.mc.player.z);
            var chunk=c.mc.world.getChunkSource().getChunk(x>>4,z>>4);
            if(chunk==null||chunk instanceof net.minecraft.world.chunk.EmptyChunk)throw new AssertionError("Insight fixture requires a received chunk");
            var pos = new BlockPos(x, chunk.getHeight(x&15,z&15)-1, z);
            c.mc.crosshairTarget = new HitResult(new Vec3d(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5), Direction.UP, pos);
            refresh.invoke(hud);
            TargetInfo block = (TargetInfo)field.get(hud);
            if (block == null || block.name.isEmpty() || !block.id.startsWith("minecraft:") || block.icon == null)
                throw new AssertionError("World Insight did not inspect a real received block");
            c.mc.crosshairTarget = new HitResult(c.mc.player);
            clock.setLong(hud, 0);refresh.invoke(hud);
            TargetInfo player = (TargetInfo)field.get(hud);
            if (player == null || player.health != null)
                throw new AssertionError("World Insight must not advertise hidden player health");
            c.mc.crosshairTarget = null;clock.setLong(hud, 0);refresh.invoke(hud);
            if (field.get(hud) != null) throw new AssertionError("Miss retained stale target information");
        } finally { c.mc.crosshairTarget = original; }
    }
}
