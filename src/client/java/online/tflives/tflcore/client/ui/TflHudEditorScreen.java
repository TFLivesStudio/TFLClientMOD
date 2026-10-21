package online.tflives.tflcore.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import online.tflives.tflcore.client.TflCoreClient;
import online.tflives.tflcore.client.config.TflSettings;
import online.tflives.tflcore.client.config.WidgetPlacement;
import online.tflives.tflcore.client.hud.HudManager;
import online.tflives.tflcore.client.hud.HudWidget;
import online.tflives.tflcore.client.ui.widget.TflButton;

import java.util.List;

/**
 * In-game HUD layout editor.
 *
 * <p>Drag with the left mouse button, right-click to toggle a widget, click empty space to
 * deselect. Keyboard: arrows nudge (Shift = grid), {@code +}/{@code -} scale, {@code A} anchor,
 * {@code B} background, {@code G} grid/snap, {@code R} reset, {@code Tab} cycle, {@code Del}
 * disable. Positions are stored as anchor offsets, so layouts survive resolution changes.</p>
 */
public final class TflHudEditorScreen extends Screen {
    private static final int TOP_BAR = 30;
    private static final int BOTTOM_BAR = 30;

    private final Screen parent;
    private String selectedId;
    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;

    public TflHudEditorScreen(Screen parent) {
        super(Component.translatable("tflcore.editor.title"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    protected void init() {
        TflCoreClient.hud().setEditorActive(true);
        int gap = 4;
        int count = 4;
        int doneWidth = 72;
        int buttonWidth = Math.max(44, Math.min(100,
                (this.width - 16 - gap * (count + 1) - doneWidth) / count));
        int x = 8;
        int y = 5;
        this.addRenderableWidget(new TflButton(x, y, buttonWidth, 20,
                gridLabel(), TflButton.Style.SECONDARY, TflCoreClient::theme, () -> {
            TflSettings settings = TflCoreClient.settings().get();
            settings.hud.snapToGrid = !settings.hud.snapToGrid;
            save();
            rebuildWidgets();
        }).selected(TflCoreClient.settings().get().hud.snapToGrid));
        x += buttonWidth + gap;
        this.addRenderableWidget(new TflButton(x, y, buttonWidth, 20,
                Component.translatable("tflcore.editor.anchor"), TflButton.Style.SECONDARY,
                TflCoreClient::theme, () -> {
            if (selectedId != null) {
                TflCoreClient.hud().cycleAnchor(selectedId);
                save();
            }
        }));
        x += buttonWidth + gap;
        this.addRenderableWidget(new TflButton(x, y, buttonWidth, 20,
                Component.translatable("tflcore.editor.background"), TflButton.Style.SECONDARY,
                TflCoreClient::theme, () -> {
            WidgetPlacement placement = TflCoreClient.hud().placement(selectedId);
            if (placement != null) {
                placement.background = !placement.background;
                save();
            }
        }));
        x += buttonWidth + gap;
        this.addRenderableWidget(new TflButton(x, y, buttonWidth, 20,
                Component.translatable("tflcore.editor.reset"), TflButton.Style.SECONDARY,
                TflCoreClient::theme, () -> {
            if (selectedId != null) {
                TflCoreClient.hud().resetWidget(selectedId);
            } else {
                TflCoreClient.hud().resetLayout();
            }
            save();
        }));
        this.addRenderableWidget(new TflButton(this.width - doneWidth - 8, y, doneWidth, 20,
                Component.translatable("gui.done"), TflButton.Style.PRIMARY,
                TflCoreClient::theme, this::onClose));
    }

    private Component gridLabel() {
        boolean snap = TflCoreClient.settings().get().hud.snapToGrid;
        return Component.translatable("tflcore.editor.grid", snap ? "ON" : "OFF");
    }

    private void cycleScale() {
        WidgetPlacement placement = TflCoreClient.hud().placement(selectedId);
        if (placement == null) return;
        float current = placement.scale;
        float next;
        if (current < 0.99F) {
            next = 1.0F;
        } else if (current < 1.24F) {
            next = 1.25F;
        } else if (current < 1.49F) {
            next = 1.5F;
        } else if (current < 1.99F) {
            next = 2.0F;
        } else {
            next = 0.75F;
        }
        placement.scale = next;
        save();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        TflTheme theme = TflCoreClient.theme();
        TflSettings settings = TflCoreClient.settings().get();
        graphics.fillGradient(0, 0, this.width, this.height, theme.bgTop(), theme.bgBottom());
        graphics.fill(0, 0, this.width, this.height, 0xB010141C);

        if (this.minecraft.player == null) {
            graphics.centeredText(this.font, Component.translatable("tflcore.notice.world_required").getString(),
                    this.width / 2, this.height / 2, theme.text());
            super.extractRenderState(graphics, mouseX, mouseY, delta);
            return;
        }

        if (settings.hud.snapToGrid) {
            int step = Math.max(6, settings.hud.gridSize * 2);
            int grid = TflTheme.withAlpha(theme.text(), 0.055F);
            for (int x = 0; x < this.width; x += step) {
                graphics.fill(x, TOP_BAR, x + 1, this.height - BOTTOM_BAR, grid);
            }
            for (int y = TOP_BAR; y < this.height - BOTTOM_BAR; y += step) {
                graphics.fill(0, y, this.width, y + 1, grid);
            }
        }

        HudManager hud = TflCoreClient.hud();
        hud.renderAll(graphics, settings, true);

        HudManager.HitBox selected = null;
        List<HudManager.HitBox> boxes = hud.hitBoxes();
        for (HudManager.HitBox box : boxes) {
            if (box.id().equals(selectedId)) {
                selected = box;
                break;
            }
        }
        if (selected == null && !boxes.isEmpty()) {
            selectedId = boxes.get(0).id();
            selected = boxes.get(0);
        }
        if (selected != null) {
            WidgetPlacement placement = hud.placement(selected.id());
            graphics.outline(selected.x() - 2, selected.y() - 2, selected.width() + 4, selected.height() + 4,
                    theme.accent());
            TflDraw.cornerBrackets(graphics, theme.accent(), selected.x() - 3, selected.y() - 3,
                    selected.width() + 6, selected.height() + 6, 6);
            if (placement != null) {
                String info = selected.id() + "  " + placement.anchor + "  "
                        + Math.round(placement.scale * 100F) + "%";
                int labelY = selected.y() - 12;
                if (labelY < TOP_BAR + 2) {
                    labelY = selected.y() + selected.height() + 3;
                }
                int labelWidth = this.font.width(info) + 6;
                graphics.fill(selected.x(), labelY - 1, selected.x() + labelWidth,
                        labelY + this.font.lineHeight + 1, theme.surfaceActive());
                graphics.outline(selected.x(), labelY - 1, labelWidth, this.font.lineHeight + 2, theme.accent());
                graphics.text(this.font, info, selected.x() + 3, labelY, theme.text(), false);
            }
        }

        graphics.fill(0, 0, this.width, TOP_BAR, theme.surface());
        graphics.fill(0, TOP_BAR - 1, this.width, TOP_BAR, theme.border());
        graphics.fill(0, this.height - BOTTOM_BAR, this.width, this.height, theme.surface());
        graphics.fill(0, this.height - BOTTOM_BAR, this.width, this.height - BOTTOM_BAR + 1, theme.border());

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        String hint = Component.translatable("tflcore.editor.hint").getString();
        graphics.text(this.font, TflDraw.trim(this.font, hint, this.width - 16),
                8, this.height - 20, theme.muted(), false);
        WidgetPlacement placement = hud.placement(selectedId);
        if (placement != null) {
            TflDraw.rightAligned(graphics, this.font,
                    Component.translatable("tflcore.editor.position",
                            placement.anchor, placement.x, placement.y).getString(),
                    this.width - 8, this.height - 20, theme.accent());
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.y() < TOP_BAR || event.y() > this.height - BOTTOM_BAR) {
            return super.mouseClicked(event, doubleClick);
        }
        HudManager hud = TflCoreClient.hud();
        HudManager.HitBox hit = hud.hitTest(event.x(), event.y());
        if (hit == null) {
            selectedId = null;
            return true;
        }
        selectedId = hit.id();
        WidgetPlacement placement = hud.placement(hit.id());
        if (placement == null) return true;
        if (event.button() == 1) {
            placement.enabled = !placement.enabled;
            save();
            return true;
        }
        if (event.button() != 0) return true;
        if (!placement.enabled) {
            placement.enabled = true;
            save();
        }
        dragging = true;
        dragOffsetX = (int) Math.round(event.x()) - hit.x();
        dragOffsetY = (int) Math.round(event.y()) - hit.y();
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (!dragging || selectedId == null) {
            return super.mouseDragged(event, dragX, dragY);
        }
        HudManager hud = TflCoreClient.hud();
        HudManager.HitBox hit = null;
        for (HudManager.HitBox box : hud.hitBoxes()) {
            if (box.id().equals(selectedId)) {
                hit = box;
                break;
            }
        }
        WidgetPlacement placement = hud.placement(selectedId);
        if (placement == null) return true;
        int width = hit == null ? 80 : hit.width();
        int height = hit == null ? 18 : hit.height();
        int x = (int) Math.round(event.x()) - dragOffsetX;
        int y = (int) Math.round(event.y()) - dragOffsetY;
        int step = Math.max(1, TflCoreClient.settings().get().hud.gridSize);
        if (TflCoreClient.settings().get().hud.snapToGrid) {
            x = Math.round((float) x / step) * step;
            y = Math.round((float) y / step) * step;
        }
        HudManager.moveTo(placement, x, y, width, height, this.width, this.height);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging) {
            dragging = false;
            save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == InputConstants.KEY_TAB) {
            cycleSelection(event.hasShiftDown() ? -1 : 1);
            return true;
        }
        if (key == InputConstants.KEY_G) {
            TflSettings settings = TflCoreClient.settings().get();
            settings.hud.snapToGrid = !settings.hud.snapToGrid;
            save();
            rebuildWidgets();
            return true;
        }
        if (key == InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (selectedId == null) {
            return super.keyPressed(event);
        }
        if (key == InputConstants.KEY_DELETE || key == InputConstants.KEY_BACKSPACE) {
            WidgetPlacement placement = TflCoreClient.hud().placement(selectedId);
            if (placement != null) {
                placement.enabled = false;
                save();
            }
            return true;
        }
        if (key == InputConstants.KEY_B) {
            WidgetPlacement placement = TflCoreClient.hud().placement(selectedId);
            if (placement != null) {
                placement.background = !placement.background;
                save();
            }
            return true;
        }
        if (key == InputConstants.KEY_A) {
            TflCoreClient.hud().cycleAnchor(selectedId);
            save();
            return true;
        }
        if (key == InputConstants.KEY_R) {
            TflCoreClient.hud().resetWidget(selectedId);
            save();
            return true;
        }
        if (key == InputConstants.KEY_EQUALS || key == InputConstants.KEY_ADD) {
            cycleScale();
            return true;
        }
        if (key == InputConstants.KEY_MINUS) {
            WidgetPlacement placement = TflCoreClient.hud().placement(selectedId);
            if (placement != null) {
                placement.scale = Math.max(0.5F, placement.scale - 0.25F);
                save();
            }
            return true;
        }
        int step = event.hasShiftDown()
                ? Math.max(4, TflCoreClient.settings().get().hud.gridSize * 2)
                : 1;
        return switch (key) {
            case InputConstants.KEY_LEFT -> {
                nudge(-step, 0);
                yield true;
            }
            case InputConstants.KEY_RIGHT -> {
                nudge(step, 0);
                yield true;
            }
            case InputConstants.KEY_UP -> {
                nudge(0, -step);
                yield true;
            }
            case InputConstants.KEY_DOWN -> {
                nudge(0, step);
                yield true;
            }
            default -> super.keyPressed(event);
        };
    }

    private void nudge(int dx, int dy) {
        HudManager hud = TflCoreClient.hud();
        WidgetPlacement placement = hud.placement(selectedId);
        if (placement == null) return;
        HudManager.HitBox hit = null;
        for (HudManager.HitBox box : hud.hitBoxes()) {
            if (box.id().equals(selectedId)) {
                hit = box;
                break;
            }
        }
        int width = hit == null ? 80 : hit.width();
        int height = hit == null ? 18 : hit.height();
        int[] position = HudManager.position(placement, width, height, this.width, this.height);
        HudManager.moveTo(placement, position[0] + dx, position[1] + dy, width, height,
                this.width, this.height);
        save();
    }

    private void cycleSelection(int direction) {
        List<HudWidget> widgets = TflCoreClient.hud().widgets();
        if (widgets.isEmpty()) return;
        int index = -1;
        for (int i = 0; i < widgets.size(); i++) {
            if (widgets.get(i).id().equals(selectedId)) {
                index = i;
                break;
            }
        }
        index = (index + direction + widgets.size()) % widgets.size();
        selectedId = widgets.get(index).id();
    }

    private void save() {
        TflCoreClient.settings().requestSave();
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public void removed() {
        TflCoreClient.hud().setEditorActive(false);
        TflCoreClient.settings().requestSave();
        super.removed();
    }
}
