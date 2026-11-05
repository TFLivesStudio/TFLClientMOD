package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.insight.*;
import dev.theredstonee.trsclient.core.module.HudModule;
import dev.theredstonee.trsclient.core.ui.Canvas;
import dev.theredstonee.trsclient.core.ui.menu.HudItem;
import net.minecraft.block.Block;
import net.minecraft.block.state.property.Property;
import net.minecraft.world.HitResult;
import net.minecraft.item.ItemStack;
import java.util.*;

final class OrnitheWorldInsight implements HudItem {
    private final OrnitheClient c;
    private final dev.theredstonee.trsclient.core.map.MobHeads heads = new dev.theredstonee.trsclient.core.map.MobHeads();
    private java.lang.ref.WeakReference<Object> world = new java.lang.ref.WeakReference<>(null);
    private long next;
    private TargetInfo info;
    private List<String> lines = List.of();
    OrnitheWorldInsight(OrnitheClient c) { this.c = c; }
    public HudModule module() { return c.modules.worldInsight; }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void refresh() {
        if (world.get() != c.mc.world) { world = new java.lang.ref.WeakReference<>(c.mc.world); next = 0; info = null; lines = List.of(); }
        if (c.mc.screen instanceof CoreScreen screen && screen.ui instanceof dev.theredstonee.trsclient.core.ui.menu.HudEditor) {
            info = new TargetInfo("World Insight", "minecraft:stone", "Minecraft", List.of(), null, null, null);
            lines = info.lines(c.modules.worldInsight); next = 0; return;
        }
        long now = System.nanoTime();
        if (now < next) return;
        next = now + 100_000_000L;
        info = null;
        var hit = c.mc.crosshairTarget;
        if (c.mc.world != null && c.mc.player != null && hit != null) {
            if (hit.type == HitResult.Type.BLOCK) {
                var pos = hit.getPos();
                if (c.redstone.world.loaded(pos.getX(), pos.getY(), pos.getZ())) {
                    var state = c.mc.world.getBlockState(pos);
                    var block = state.getBlock();
                    var key = Block.REGISTRY.getKey(block);
                    String id = key == null ? "" : key.toString();
                    var properties = new ArrayList<String>();
                    for (Property property : state.properties()) properties.add(property.getName() + ": " + property.getName(state.get(property)));
                    Collections.sort(properties);
                    var stack = new ItemStack(block);
                    info = new TargetInfo(block.getName(), id, id.startsWith("minecraft:") ? "Minecraft" : "", properties, stack, null, null);
                }
            } else if (hit.entity != null) {
                var entity = hit.entity;
                Float health = null, max = null;
                if (!(entity instanceof net.minecraft.entity.living.player.PlayerEntity) && entity instanceof net.minecraft.entity.living.LivingEntity living) { health = living.getHealth(); max = living.getMaxHealth(); }
                String kind = net.minecraft.entity.Entities.getKey(entity);
                Object visual = entity instanceof net.minecraft.client.entity.living.player.ClientPlayerEntity player
                    ? c.canvas.game(player.getSkinTextureLocation().toString(), 64, 64)
                    : c.map == null || kind == null ? null : heads.head(kind, c.map.palette(), c.canvas.resourceGeneration());
                info = new TargetInfo(entity.getName(), kind, "", List.of(), visual, health, max);
            }
        }
        lines = info == null ? List.of() : info.lines(c.modules.worldInsight);
    }
    public int width() { refresh(); int width = 0; for (String line : lines) width = Math.max(width, c.canvas.textWidth(line)); return Math.max(100, width + 32); }
    public int height() { refresh(); return Math.max(24, lines.size() * 11 + 8); }
    public void draw(Canvas canvas) {
        boolean preview = c.mc.screen instanceof CoreScreen screen && screen.ui instanceof dev.theredstonee.trsclient.core.ui.menu.HudEditor;
        if (!c.modules.worldInsight.shouldDraw(c.mc.options.hideGui, c.mc.screen instanceof dev.theredstonee.trsclient.core.ui.menus.PauseOverlay, c.mc.screen != null, preview)) return;
        refresh(); if (lines.isEmpty()) return;
        var settings = c.modules.worldInsight;
        canvas.fill(0, 0, width(), height(), settings.backgroundArgb());
        int left = settings.icon.get() && info.icon != null ? 26 : 5;
        if (left == 26) InsightIcon.draw(canvas, info.icon, 5, 5);
        for (int i = 0; i < lines.size(); i++) canvas.text(lines.get(i), left, 4 + i * 11, settings.textColor.argb(), settings.shadow());
    }
}
