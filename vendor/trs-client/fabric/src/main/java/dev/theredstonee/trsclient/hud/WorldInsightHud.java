package dev.theredstonee.trsclient.hud;
//? if >=26.3 {
/*
import dev.theredstonee.trsclient.core.insight.*;
import dev.theredstonee.trsclient.core.module.TrsModules;
import dev.theredstonee.trsclient.ui.Gfx;
import net.minecraft.client.gui.Font;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;

public final class WorldInsightHud extends HudElement {
    private final WorldInsightModule settings;
    private final dev.theredstonee.trsclient.core.map.MobHeads heads = new dev.theredstonee.trsclient.core.map.MobHeads();
    private java.lang.ref.WeakReference<Object> world = new java.lang.ref.WeakReference<>(null);
    private long next;
    private TargetInfo info;
    private List<String> lines = List.of();
    public WorldInsightHud(TrsModules modules) { super(modules.worldInsight); settings = modules.worldInsight; }
    private void refresh(boolean preview) {
        if (world.get() != mc.level) { world = new java.lang.ref.WeakReference<>(mc.level); next = 0; info = null; lines = List.of(); }
        if (preview) { lines = List.of("World Insight", "Minecraft"); return; }
        long now = System.nanoTime(); if (now < next) return; next = now + 100_000_000L;
        info = null;
        if (mc.level != null && mc.player != null) {
            if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK && mc.level.hasChunkAt(hit.getBlockPos())) {
                var state = mc.level.getBlockState(hit.getBlockPos());
                String id = String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
                var properties = new ArrayList<String>();
                for (var property : state.getProperties()) properties.add(property.getName() + ": " + String.valueOf(state.getValue(property)));
                Collections.sort(properties);
                info = new TargetInfo(state.getBlock().getName().getString(), id, id.startsWith("minecraft:") ? "Minecraft" : "", properties, new ItemStack(state.getBlock()), null, null);
            } else if (mc.hitResult instanceof EntityHitResult hit) {
                var entity = hit.getEntity();
                String id = String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
                var engine = dev.theredstonee.trsclient.core.map.MapEngine.get();
                Object visual = entity instanceof net.minecraft.client.player.AbstractClientPlayer player
                    ? new dev.theredstonee.trsclient.core.ui.TextureRef(player.getSkin().body().texturePath(), 64, 64)
                    : engine == null ? null : heads.head(id, engine.palette(), dev.theredstonee.trsclient.ui.TextureStore.INSTANCE.resourceGeneration());
                info = new TargetInfo(entity.getName().getString(), id, id.startsWith("minecraft:") ? "Minecraft" : "", List.of(), visual,
                    !(entity instanceof net.minecraft.world.entity.player.Player) && entity instanceof LivingEntity living ? living.getHealth() : null, !(entity instanceof net.minecraft.world.entity.player.Player) && entity instanceof LivingEntity living ? living.getMaxHealth() : null);
            }
        }
        lines = info == null ? List.of() : info.lines(settings);
    }
    public boolean visible() { if (!settings.shouldDraw(dev.theredstonee.trsclient.compat.Mc.hudHidden(), dev.theredstonee.trsclient.compat.Mc.screen() instanceof dev.theredstonee.trsclient.core.ui.menus.PauseOverlay, dev.theredstonee.trsclient.compat.Mc.screen() != null, false)) return false; refresh(false); return !lines.isEmpty(); }
    public int width(Font font, boolean preview) { refresh(preview); int w = 100; for (String line : lines) w = Math.max(w, font.width(line) + 32); return w; }
    public int height(Font font, boolean preview) { refresh(preview); return Math.max(24, lines.size() * 11 + 8); }
    public void draw(Gfx g, Font font, boolean preview) {
        refresh(preview); if (lines.isEmpty()) return;
        g.fill(0, 0, width(font, preview), height(font, preview), settings.backgroundArgb());
        int left = !preview && settings.icon.get() && info != null && info.icon != null ? 26 : 5;
        if (left == 26) InsightIcon.draw(dev.theredstonee.trsclient.ui.GfxCanvas.of(g, font), info.icon, 5, 5);
        for (int i = 0; i < lines.size(); i++) g.text(font, lines.get(i), left, 4 + i * 11, settings.textColor.argb(), settings.shadow());
    }
}
*///?}
