package online.tflives.tflcore.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import online.tflives.tflcore.client.TflCoreClient;
import online.tflives.tflcore.client.config.SettingsStore;
import online.tflives.tflcore.client.config.TflSettings;
import online.tflives.tflcore.client.config.WidgetPlacement;
import online.tflives.tflcore.client.ui.TflDraw;
import online.tflives.tflcore.client.ui.TflTheme;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Owns every HUD widget and renders them in a single Fabric HUD element.
 *
 * <p>One element means one hook, one iteration and one place to enforce visibility rules (F1,
 * screens, streamer mode, global scale). Widgets only rebuild their text on their own refresh
 * interval.</p>
 */
public final class HudManager {
    public static final Identifier LAYER_ID = Identifier.fromNamespaceAndPath(TflCoreClient.MOD_ID, "hud");

    public record HitBox(String id, int x, int y, int width, int height, float scale) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    private final SettingsStore store;
    private final Map<String, HudWidget> widgets = new LinkedHashMap<>();
    private final List<HitBox> hitBoxes = new ArrayList<>();
    private boolean editorActive;
    private boolean attached;

    public HudManager(SettingsStore store) {
        this.store = store;
    }

    public void register(HudWidget widget) {
        if (widgets.containsKey(widget.id())) {
            throw new IllegalStateException("Duplicate HUD widget id: " + widget.id());
        }
        widgets.put(widget.id(), widget);
    }

    /** Ensures every registered widget has a persisted placement. */
    public void initialize() {
        TflSettings settings = store.get();
        for (HudWidget widget : widgets.values()) {
            settings.widget(widget.id(), widget.defaultPlacement());
        }
    }

    public void attach() {
        if (attached) return;
        attached = true;
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, LAYER_ID,
                (graphics, delta) -> extract(graphics));
    }

    private void extract(GuiGraphicsExtractor graphics) {
        Minecraft client = Minecraft.getInstance();
        Diagnostics.sampleFrame(client);
        if (client.options != null) {
            InputTracker.update(client.options.keyAttack.isDown(), client.options.keyUse.isDown());
        }
        TflSettings settings = store.get();
        if (!settings.hud.enabled || editorActive) {
            hitBoxes.clear();
            return;
        }
        if (client.player == null || client.gui.hud.isHidden()) {
            hitBoxes.clear();
            return;
        }
        if (settings.hud.hideInScreens && client.gui.screen() != null) {
            hitBoxes.clear();
            return;
        }
        renderAll(graphics, settings, false);
    }

    /** Renders every widget. Also used by the editor to draw its live preview. */
    public void renderAll(GuiGraphicsExtractor graphics, TflSettings settings, boolean editor) {
        Minecraft client = Minecraft.getInstance();
        TflTheme theme = TflCoreClient.theme();
        hitBoxes.clear();
        if (client.player == null) return;
        long now = System.nanoTime();
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        for (HudWidget widget : widgets.values()) {
            WidgetPlacement placement = settings.widget(widget.id(), widget.defaultPlacement());
            if (!placement.enabled && !editor) continue;
            boolean available = widget.isAvailable(client, settings);
            if (!available && !editor) continue;
            widget.updateIfNeeded(client, settings, now);
            int unscaledWidth = available ? widget.width() : Math.max(84, widget.width());
            int unscaledHeight = available ? widget.height() : Math.max(18, widget.height());
            if (unscaledWidth <= 0 || unscaledHeight <= 0) continue;

            float scale = placement.scale * settings.hud.globalScale;
            int width = Math.max(1, Math.round(unscaledWidth * scale));
            int height = Math.max(1, Math.round(unscaledHeight * scale));
            int[] position = position(placement, width, height, screenWidth, screenHeight);

            if (placement.enabled && available) {
                graphics.pose().pushMatrix();
                graphics.pose().translate(position[0], position[1]);
                graphics.pose().scale(scale, scale);
                if (placement.background) {
                    int alpha = Math.round(255 * placement.opacity);
                    graphics.fill(0, 0, unscaledWidth, unscaledHeight,
                            TflTheme.argb(alpha, theme.surface()));
                    graphics.fill(0, 0, 2, unscaledHeight, theme.accent());
                    graphics.outline(0, 0, unscaledWidth, unscaledHeight, theme.border());
                }
                widget.render(graphics, client, theme, settings, placement, unscaledWidth, unscaledHeight);
                graphics.pose().popMatrix();
            } else if (editor) {
                // Placeholders keep every widget selectable, even when disabled or unavailable here.
                graphics.fill(position[0], position[1], position[0] + width, position[1] + height,
                        TflTheme.withAlpha(theme.surface(), available ? 0.40F : 0.22F));
                graphics.outline(position[0], position[1], width, height,
                        placement.enabled ? theme.border() : theme.borderStrong());
                String label = widget.id() + (available ? "" : " (n/a)") + (placement.enabled ? "" : " [off]");
                graphics.text(client.font, TflDraw.trim(client.font, label, Math.max(0, width - 6)),
                        position[0] + 3, position[1] + Math.max(2, (height - client.font.lineHeight) / 2),
                        placement.enabled ? theme.muted() : theme.borderStrong(), false);
            }
            hitBoxes.add(new HitBox(widget.id(), position[0], position[1], width, height, scale));
        }
    }

    public static int[] position(WidgetPlacement placement, int width, int height, int screenWidth, int screenHeight) {
        int x;
        int y;
        switch (placement.anchor) {
            case WidgetPlacement.TOP_RIGHT -> {
                x = screenWidth - width - placement.x;
                y = placement.y;
            }
            case WidgetPlacement.BOTTOM_LEFT -> {
                x = placement.x;
                y = screenHeight - height - placement.y;
            }
            case WidgetPlacement.BOTTOM_RIGHT -> {
                x = screenWidth - width - placement.x;
                y = screenHeight - height - placement.y;
            }
            case WidgetPlacement.CENTER -> {
                x = (screenWidth - width) / 2 + placement.x;
                y = (screenHeight - height) / 2 + placement.y;
            }
            case WidgetPlacement.TOP_CENTER -> {
                x = (screenWidth - width) / 2 + placement.x;
                y = placement.y;
            }
            case WidgetPlacement.BOTTOM_CENTER -> {
                x = (screenWidth - width) / 2 + placement.x;
                y = screenHeight - height - placement.y;
            }
            default -> {
                x = placement.x;
                y = placement.y;
            }
        }
        x = Math.max(0, Math.min(x, Math.max(0, screenWidth - width)));
        y = Math.max(0, Math.min(y, Math.max(0, screenHeight - height)));
        return new int[]{x, y};
    }

    /** Converts an absolute screen position back into anchor offsets (used by drag & drop). */
    public static void moveTo(WidgetPlacement placement, int screenX, int screenY,
                              int width, int height, int screenWidth, int screenHeight) {
        int clampedX = Math.max(0, Math.min(screenX, Math.max(0, screenWidth - width)));
        int clampedY = Math.max(0, Math.min(screenY, Math.max(0, screenHeight - height)));
        placement.x = switch (placement.anchor) {
            case WidgetPlacement.TOP_RIGHT, WidgetPlacement.BOTTOM_RIGHT -> screenWidth - width - clampedX;
            case WidgetPlacement.CENTER, WidgetPlacement.TOP_CENTER, WidgetPlacement.BOTTOM_CENTER ->
                    clampedX - (screenWidth - width) / 2;
            default -> clampedX;
        };
        placement.y = switch (placement.anchor) {
            case WidgetPlacement.BOTTOM_LEFT, WidgetPlacement.BOTTOM_RIGHT, WidgetPlacement.BOTTOM_CENTER ->
                    screenHeight - height - clampedY;
            case WidgetPlacement.CENTER -> clampedY - (screenHeight - height) / 2;
            default -> clampedY;
        };
    }

    public List<HitBox> hitBoxes() {
        return List.copyOf(hitBoxes);
    }

    public HitBox hitTest(double mouseX, double mouseY) {
        for (int i = hitBoxes.size() - 1; i >= 0; i--) {
            HitBox box = hitBoxes.get(i);
            if (box.contains(mouseX, mouseY)) return box;
        }
        return null;
    }

    public void setEditorActive(boolean active) {
        this.editorActive = active;
        if (!active) hitBoxes.clear();
    }

    public boolean isEditorActive() {
        return editorActive;
    }

    public HudWidget widget(String id) {
        return widgets.get(id);
    }

    public List<HudWidget> widgets() {
        return List.copyOf(widgets.values());
    }

    public WidgetPlacement placement(String id) {
        HudWidget widget = widgets.get(id);
        if (widget == null) return null;
        return store.get().widget(id, widget.defaultPlacement());
    }

    public void applyWidgetSet(Set<String> enabled) {
        TflSettings settings = store.get();
        for (HudWidget widget : widgets.values()) {
            WidgetPlacement placement = settings.widget(widget.id(), widget.defaultPlacement());
            placement.enabled = enabled.contains(widget.id());
        }
        store.requestSave();
    }

    public void setWidgetEnabled(String id, boolean enabled) {
        WidgetPlacement placement = placement(id);
        if (placement != null) {
            placement.enabled = enabled;
            store.requestSave();
        }
    }

    public void cycleAnchor(String id) {
        WidgetPlacement placement = placement(id);
        if (placement == null) return;
        String next = switch (placement.anchor) {
            case WidgetPlacement.TOP_LEFT -> WidgetPlacement.TOP_RIGHT;
            case WidgetPlacement.TOP_RIGHT -> WidgetPlacement.BOTTOM_RIGHT;
            case WidgetPlacement.BOTTOM_RIGHT -> WidgetPlacement.BOTTOM_LEFT;
            case WidgetPlacement.BOTTOM_LEFT -> WidgetPlacement.CENTER;
            case WidgetPlacement.CENTER -> WidgetPlacement.TOP_CENTER;
            case WidgetPlacement.TOP_CENTER -> WidgetPlacement.BOTTOM_CENTER;
            default -> WidgetPlacement.TOP_LEFT;
        };
        placement.anchor = next;
        placement.x = 6;
        placement.y = 6;
        store.requestSave();
    }

    public void resetLayout() {
        TflSettings settings = store.get();
        for (HudWidget widget : widgets.values()) {
            settings.hud.widgets.put(widget.id(), widget.defaultPlacement());
        }
        store.requestSave();
    }

    public void resetWidget(String id) {
        HudWidget widget = widgets.get(id);
        if (widget == null) return;
        store.get().hud.widgets.put(id, widget.defaultPlacement());
        store.requestSave();
    }

    public int enabledCount() {
        int count = 0;
        TflSettings settings = store.get();
        for (HudWidget widget : widgets.values()) {
            WidgetPlacement placement = settings.widget(widget.id(), widget.defaultPlacement());
            if (placement.enabled) count++;
        }
        return count;
    }

    public SettingsStore store() {
        return store;
    }
}
