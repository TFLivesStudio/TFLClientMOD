package online.tflives.tflcore.client.pvp;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import online.tflives.tflcore.client.TflCoreClient;
import online.tflives.tflcore.client.config.TflSettings;
import online.tflives.tflcore.client.module.ModuleCategory;
import online.tflives.tflcore.client.module.TflModule;

/**
 * Replaces the vanilla crosshair through the public Fabric HUD registry (no mixin, no OpenGL).
 * When the module is disabled, the original vanilla element is rendered untouched.
 */
public final class CrosshairModule implements TflModule {
    private boolean installed;
    private boolean active;

    @Override
    public String id() {
        return "crosshair";
    }

    @Override
    public ModuleCategory category() {
        return ModuleCategory.PVP;
    }

    @Override
    public void onEnable(Minecraft client) {
        active = true;
        if (installed) return;
        installed = true;
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (graphics, delta) ->
                extract(graphics, delta, original));
    }

    @Override
    public void onDisable(Minecraft client) {
        active = false;
    }

    private void extract(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker delta, HudElement original) {
        Minecraft client = Minecraft.getInstance();
        TflSettings settings = TflCoreClient.settings().get();
        if (!active || !settings.pvp.customCrosshair || client.gui.hud.isHidden()) {
            original.extractRenderState(graphics, delta);
            return;
        }
        if (client.gui.screen() != null || client.player == null) {
            original.extractRenderState(graphics, delta);
            return;
        }
        boolean firstPerson = client.options.getCameraType() == CameraType.FIRST_PERSON;
        if (!firstPerson && settings.pvp.crosshairHideThirdPerson) {
            original.extractRenderState(graphics, delta);
            return;
        }
        draw(graphics, settings);
    }

    private void draw(GuiGraphicsExtractor graphics, TflSettings settings) {
        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2;
        int color = settings.pvp.crosshairColor;
        int outline = 0x99000000;
        float scale = settings.pvp.crosshairScale;
        int thickness = Math.max(1, Math.round(1.0F * scale));
        int gap = Math.max(1, Math.round(2.0F * scale));
        int length = Math.max(2, Math.round(4.0F * scale));
        String style = settings.pvp.crosshairStyle;

        switch (style) {
            case "DOT" -> box(graphics, centerX - 1, centerY - 1, 3, 3, color, outline);
            case "T" -> {
                box(graphics, centerX - length, centerY - gap - thickness, length * 2, thickness, color, outline);
                box(graphics, centerX, centerY - gap, thickness, length + gap, color, outline);
            }
            case "CIRCLE" -> {
                int radius = Math.max(3, Math.round(5 * scale));
                for (int i = 0; i < 8; i++) {
                    double angle = Math.PI * 2.0 * i / 8.0;
                    int x = centerX + (int) Math.round(Math.cos(angle) * radius);
                    int y = centerY + (int) Math.round(Math.sin(angle) * radius);
                    box(graphics, x, y, 2, 2, color, outline);
                }
            }
            case "SQUARE" -> {
                int half = Math.max(3, Math.round(5 * scale));
                graphics.outline(centerX - half - 1, centerY - half - 1, (half + 1) * 2, (half + 1) * 2, outline);
                graphics.outline(centerX - half, centerY - half, half * 2, half * 2, color);
            }
            default -> {
                box(graphics, centerX - gap - length, centerY, length, thickness, color, outline);
                box(graphics, centerX + gap, centerY, length, thickness, color, outline);
                box(graphics, centerX, centerY - gap - length, thickness, length, color, outline);
                box(graphics, centerX, centerY + gap, thickness, length, color, outline);
                box(graphics, centerX - 1, centerY - 1, 2, 2, color, outline);
            }
        }
    }

    private static void box(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color, int outline) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, outline);
        graphics.fill(x, y, x + width, y + height, color);
    }
}
