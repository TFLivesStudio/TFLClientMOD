package dev.theredstonee.trsclient.core.ui.menu;

import dev.theredstonee.trsclient.core.i18n.I18n;
import dev.theredstonee.trsclient.core.hud.HudLayout;
import dev.theredstonee.trsclient.core.hud.HudHistory;
import dev.theredstonee.trsclient.core.hud.HudPresets;
import dev.theredstonee.trsclient.core.hud.HudProfiles;
import dev.theredstonee.trsclient.core.hud.HudSnap;
import dev.theredstonee.trsclient.core.module.HudModule;
import dev.theredstonee.trsclient.core.module.Setting;
import dev.theredstonee.trsclient.core.touch.SafeArea;
import dev.theredstonee.trsclient.core.touch.TouchLayout;
import dev.theredstonee.trsclient.core.touch.TouchMode;
import dev.theredstonee.trsclient.core.ui.Anim;
import dev.theredstonee.trsclient.core.ui.Canvas;
import dev.theredstonee.trsclient.core.ui.ColorMath;
import dev.theredstonee.trsclient.core.ui.FadeCanvas;
import dev.theredstonee.trsclient.core.ui.Icons;
import dev.theredstonee.trsclient.core.ui.Paint;
import dev.theredstonee.trsclient.core.ui.Redstone;
import dev.theredstonee.trsclient.core.ui.Theme;
import dev.theredstonee.trsclient.core.ui.UiKey;
import dev.theredstonee.trsclient.core.ui.UiScreen;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD-Editor: Elemente mit der Maus verschieben (rastet an Bildschirmrändern, Bildschirmmitte und
 * den anderen Elementen ein – mit Hilfslinien), Größe, Hintergrund-Deckkraft, Textschatten und
 * Chroma je Modul einstellen, Profile wechseln. Shift = frei schieben, Rechtsklick = zurücksetzen.
 *
 * <p>Touch-Modus: Finger zieht direkt, größere Greifflächen und Knöpfe, Einrasten an der sicheren Fläche
 * ({@code -Dtrs.safeInsets}) und der Knopf „Touch-Layout“ (HUD weg von den Overlay-Knöpfen).
 */
public final class HudEditor extends UiScreen {
	private static final int PANEL_W = 174;
	/** Zusätzliche Greiffläche um ein Element (Maus / Finger). */
	private static final int GRAB_PAD = 2;
	private static final int GRAB_PAD_TOUCH = 8;

	private final MenuHost host;
	private final SettingsPanel panel = new SettingsPanel();
	private final HudHistory history;
	private boolean showPresets;
	private boolean safeZones = true;
	private String presetError;

	private HudItem dragging;
	private HudItem selected;
	private double grabX;
	private double grabY;
	private int guideX = HudSnap.NO_GUIDE;
	private int guideY = HudSnap.NO_GUIDE;
	private float panelIn;

	public HudEditor(MenuHost host) {
		this.host = host;
		this.history = new HudHistory(host.modules().registry);
		panel.setKeyLabel(new SettingsPanel.KeyLabel() {
			@Override
			public String label(String keyName) {
				return HudEditor.this.host.keyLabel(keyName);
			}
		});
	}

	@Override
	protected void onClosed() {
		host.save();
		host.closeScreen();
	}

	// --- Zeichnen ---

	@Override
	protected void draw(Canvas raw, int width, int height, int mouseX, int mouseY, float dt) {
		Theme t = Theme.get();
		setScreenSize(width, height);
		Canvas c = FadeCanvas.of(raw, alpha());
		if (!host.inWorld()) c.fill(0, 0, width, height, t.background);
		c.fill(0, 0, width, height, ColorMath.withAlpha(t.background, 110));

		// Hilfslinien: Bildschirmmitte immer dezent, aktive Einrastung in Akzentfarbe
		c.fill(width / 2, 0, width / 2 + 1, height, ColorMath.withAlpha(t.text, 26));
		c.fill(0, height / 2, width, height / 2 + 1, ColorMath.withAlpha(t.text, 26));
		// Aktive Hilfslinie = bestromter Staub (mit Leuchten)
		if (dragging != null && guideX != HudSnap.NO_GUIDE) {
			c.fill(guideX - 1, 0, guideX + 2, height, ColorMath.withAlpha(t.glow, 50));
			c.fill(guideX, 0, guideX + 1, height, t.dustOn);
		}
		if (dragging != null && guideY != HudSnap.NO_GUIDE) {
			c.fill(0, guideY - 1, width, guideY + 2, ColorMath.withAlpha(t.glow, 50));
			c.fill(0, guideY, width, guideY + 1, t.dustOn);
		}

		if (TouchMode.enabled()) safeArea(c, t, width, height);
        if (safeZones) {
            Redstone.frame(c, 8, 8, Math.max(0, width - 16), Math.max(0, height - 16), ColorMath.withAlpha(t.textDim, 40));
            // Vanilla hotbar reserve is advisory: layouts may intentionally use this area.
            int hotbarW = Math.min(190, width);
            Redstone.frame(c, (width - hotbarW) / 2, Math.max(0, height - 24), hotbarW, 24,
                    ColorMath.withAlpha(t.textDim, 65));
        }

		List<HudItem> items = enabled();
		HudItem hovered = dragging != null ? dragging : itemAt(mouseX, mouseY, width, height);
		for (int i = 0; i < items.size(); i++) {
			HudItem item = items.get(i);
			int[] b = bounds(item, width, height);
			c.push();
			c.translate(b[0], b[1]);
			c.scale(item.module().scale.getFloat());
			item.draw(c);
			c.pop();
			boolean collision = false;
            if (item == selected || item == dragging) {
                for (HudItem other : items) {
                    if (other != item && HudLayout.overlaps(b, bounds(other, width, height))) collision = true;
                }
            }
            int color = collision ? 0xFFE0AA67 : item == dragging ? t.dustOn : (item == selected ? t.lampOn : (item == hovered ? 0xE0FFFFFF : 0x60FFFFFF));
			if (item == dragging || item == selected) Redstone.glow(c, b[0] - 2, b[1] - 2, b[2] + 4, b[3] + 4, item == dragging ? t.glow : t.lampGlow, 0.6f);
			Redstone.frame(c, b[0] - 2, b[1] - 2, b[2] + 4, b[3] + 4, color);
			if (TouchMode.enabled()) handles(c, b, item == dragging || item == selected ? t.dustOn : 0xA0FFFFFF);
			if (item == hovered || item == selected) {
				String label = item.module().name();
				int ly = b[1] - 11 >= 0 ? b[1] - 11 : b[1] + b[3] + 3;
				Paint.textClipped(c, label, b[0] - 1, ly, Math.max(40, b[2] + 40), t.text, true);
			}
			// Klickflächen für die Elemente gibt es nicht: ein Klick zieht sie (siehe mouseClicked).
		}

		// Halloween: ein versteckter Kürbis in der linken unteren Ecke des Rasters (bis er gefunden ist).
		dev.theredstonee.trsclient.core.halloween.Pumpkins.draw(c, hits, dev.theredstonee.trsclient.core.halloween.Pumpkins.CLIENT_HUD,
				10, height - 10, mouseX, mouseY);

		// Leiste und Feld liegen über den Vorschauen (Text hat in Minecraft eine eigene Tiefe).
		c.flush();
		c.push();
		c.raise(300f);
		topBar(c, width, mouseX, mouseY);
		if (dragging == null) hint(c, width, height);
		tools(c, width, height, mouseX, mouseY);
		panelIn = Anim.approach(panelIn, selected != null ? 1f : 0f, dt, 0.07f);
		if (panelIn > 0.02f) sidePanel(c, width, height, mouseX, mouseY);
		c.pop();
	}

	private void topBar(Canvas c, int width, int mx, int my) {
		Theme t = Theme.get();
		boolean touch = TouchMode.enabled();
		// Touch: höhere Leiste und Knöpfe (Fingerbreite); die Leiste beginnt unter dem sicheren Rand oben.
		int top = touch ? TouchMode.insetsGui().top : 0;
		int bh = touch ? 24 : 16;
		int h = top + bh + 6;
		int by = top + 3;
		barHeight = h;
		c.fill(0, 0, width, h, ColorMath.withAlpha(t.surfaceHigh, 235));
		c.fill(0, 0, width, 1, ColorMath.lerp(t.surfaceHigh, t.bevelLight, 0.6f));
		c.fill(0, h - 1, width, h, t.border);
		// Staubleitung unter der Leiste – ein Hinweis, dass hier "Strom" (Bearbeiten) anliegt.
		c.fill(0, h, width, h + 1, ColorMath.withAlpha(t.dustOn, 160));
		int ty = by + (bh - 8) / 2;
		Redstone.pip(c, 8, ty, 8, 1f);
		String title = I18n.tr("editor.title");
		c.text(title, 22, ty, t.text, false);

		// Profilwechsel
		final HudProfiles profiles = host.modules().profiles;
		String label = I18n.tr("editor.profile", profiles.activeName());
		int pw = Math.min(150, c.textWidth(label) + 16);
        int px = 22 + c.textWidth(title) + 12;
        int py = by;
        int actionReserve = Math.max(58,c.textWidth(I18n.tr("common.done"))+16)
                + Math.max(58,c.textWidth(I18n.tr("editor.menu"))+16) + 28;
        if(px+pw>width-actionReserve) {
            px=8;py=top+bh+8;h=py+bh+5;barHeight=h;
            c.fill(0,top+bh+5,width,h,ColorMath.withAlpha(t.surfaceHigh,235));
            c.fill(0,h-1,width,h,t.border);
        }
        boolean pHover=inside(mx,my,px,py,pw,bh);
        Paint.button(c,px,py,pw,bh,label,false,pHover);
		hits.add(px, py, pw, bh, new Runnable() {
			@Override
			public void run() {
				host.playClick();
				profiles.cycle();
				history.clear();
				selected = null;
			}
		});

		String doneLabel = I18n.tr("common.done");
		String menuLabel = I18n.tr("editor.menu");
		int doneW = Math.max(58, c.textWidth(doneLabel) + 16);
		int doneX = width - doneW - 8;
		boolean doneHover = inside(mx, my, doneX, by, doneW, bh);
		Paint.button(c, doneX, by, doneW, bh, doneLabel, true, doneHover);
		hits.add(doneX, by, doneW, bh, new Runnable() {
			@Override
			public void run() {
				host.playClick();
				requestClose();
			}
		});
		int menuW = Math.max(58, c.textWidth(menuLabel) + 16);
		int menuX = doneX - menuW - 6;
		boolean menuHover = inside(mx, my, menuX, by, menuW, bh);
		Paint.button(c, menuX, by, menuW, bh, menuLabel, false, menuHover);
		hits.add(menuX, by, menuW, bh, new Runnable() {
			@Override
			public void run() {
				host.playClick();
				host.save();
				host.openMenu();
			}
		});
		if (touch) {
			// Touch-Layout: eigenes Profil, HUD weg von den Standard-Knöpfen des Overlays.
			String touchLabel = I18n.tr("editor.touchLayout");
			int tw = Math.max(58, c.textWidth(touchLabel) + 16);
			int tx = menuX - tw - 6;
			if (tx > px + pw + 6) {
				boolean tHover = inside(mx, my, tx, by, tw, bh);
				Paint.button(c, tx, by, tw, bh, touchLabel, false, tHover);
				hits.add(tx, by, tw, bh, new Runnable() {
					@Override
					public void run() {
						host.playClick();
						applyTouchLayout();
					}
				});
			}
		}

	}

    private void tools(Canvas c, int width, int height, int mx, int my) {
        int y = barHeight + 5;
        tool(c, 8, y, 56, I18n.trOr("editor.presets", "Presets"), true, mx, my, new Runnable() {
            public void run() { showPresets = !showPresets; if (showPresets) selected = null; }
        });
        tool(c, 68, y, 40, I18n.trOr("editor.undo", "Undo"), history.canUndo(), mx, my, new Runnable() {
            public void run() { history.undo(); }
        });
        tool(c, 112, y, 40, I18n.trOr("editor.redo", "Redo"), history.canRedo(), mx, my, new Runnable() {
            public void run() { history.redo(); }
        });
        if (width >= 230) tool(c, 156, y, 66, safeZones ? I18n.trOr("editor.guidesOn", "Guides: on") : I18n.trOr("editor.guidesOff", "Guides: off"), true, mx, my, new Runnable() {
            public void run() { safeZones = !safeZones; }
        });
        if (!showPresets) return;
        int columns = Math.max(1, Math.min(3, (width - 16) / 84));
        int rows = (HudPresets.Preset.values().length + columns - 1) / columns;
        int pw = columns * 84 + 8;
        Redstone.window(c, 8, y + 21, pw, rows * 22 + 30);
        hits.add(8, y + 21, pw, rows * 22 + 30, new Runnable() { public void run() { } });
        int index = 0;
        for (final HudPresets.Preset preset : HudPresets.Preset.values()) {
            tool(c, 12 + (index % columns) * 84, y + 26 + (index / columns) * 22, 80,
                    preset.label, host.modules().profiles.canCreate(), mx, my, new Runnable() {
                public void run() {
                    presetError = HudPresets.create(host.modules().profiles, host.modules().registry, preset);
                    if (presetError == null) {
                        selected = null;
                        if (preset != HudPresets.Preset.CUSTOM) arrangePreset();
                        history.clear();
                        showPresets = false;
                        host.save();
                    }
                }
            });
            index++;
        }
        String note = presetError == null ? (host.modules().profiles.canCreate() ? I18n.trOr("editor.presetCreates", "Creates a new profile") : I18n.trOr("editor.profileLimit", "Profile limit reached")) : presetError;
        Paint.textClipped(c, note, 16, y + 29 + rows * 22, pw - 16, Theme.get().textDim, false);
    }

    /** Reuse the existing collision solver only after an explicit preset action. */
    private void arrangePreset() {
        if (lastWidth <= 0 || lastHeight <= 0) return;
        List<HudItem> items = enabled();
        List<int[]> rects = new ArrayList<int[]>();
        for (HudItem item : items) rects.add(bounds(item, lastWidth, lastHeight));
        List<int[]> zones = new ArrayList<int[]>();
        zones.add(new int[]{lastWidth / 2 - 24, lastHeight / 2 - 24, 48, 48});
        zones.add(new int[]{Math.max(0, (lastWidth - 190) / 2), Math.max(0, lastHeight - 24), Math.min(190, lastWidth), 24});
        SafeArea.Insets in = TouchMode.insetsGui();
        int[][] placed = TouchLayout.place(rects, lastWidth, lastHeight, zones,
                new SafeArea.Insets(Math.max(8, in.left), Math.max(barHeight + 27, in.top), Math.max(8, in.right), Math.max(8, in.bottom)));
        for (int i = 0; i < items.size(); i++) {
            int[] b = rects.get(i);
            items.get(i).module().position().set(HudLayout.fromPixels(placed[i][0], placed[i][1], b[2], b[3], lastWidth, lastHeight));
        }
    }

    private void tool(Canvas c, int x, int y, int w, String label, final boolean enabled, int mx, int my, final Runnable action) {
        Paint.button(c, x, y, w, 18, label, false, enabled && inside(mx, my, x, y, w, 18));
        if (!enabled) c.fill(x, y, x + w, y + 18, ColorMath.withAlpha(Theme.get().background, 110));
        hits.add(x, y, w, 18, new Runnable() {
            public void run() { if (enabled) { host.playClick(); action.run(); } }
        });
    }

	/** Bedienhinweis unten in der Mitte (weicht keinem Knopf der Leiste). */
	private static void hint(Canvas c, int width, int height) {
		Theme t = Theme.get();
		String hint = TouchMode.enabled() ? I18n.tr("editor.hintTouch") : I18n.trOr("editor.precisionHint", "Tab: select / Arrows: 1 px / Shift: 10 px or free drag / Right click: reset");
		String text = c.textWidth(hint) + 16 <= width ? hint : c.clip(hint, width - 24) + "…";
		int tw = c.textWidth(text);
		int x = (width - tw) / 2 - 7;
		int y = height - 20 - TouchMode.insetsGui().bottom;
		Redstone.block(c, x, y, tw + 14, 15, ColorMath.withAlpha(t.surfaceHigh, 225));
		Redstone.frame(c, x, y, tw + 14, 15, t.border);
		c.text(text, x + 7, y + 4, t.textDim, false);
	}

	private void sidePanel(Canvas c, int width, int height, int mx, int my) {
		Theme t = Theme.get();
		final HudItem item = selected;
		if (item == null) return;
		HudModule module = item.module();
		List<Setting> settings = new ArrayList<Setting>();
		settings.add(module.scale);
		settings.add(module.backgroundOpacity);
		settings.add(module.textShadow);
		settings.add(module.textColor);

		int h = 40 + panel.height(settings) + 22;
		int x = width - PANEL_W - 8 - TouchMode.insetsGui().right + Math.round((1 - Anim.easeOut(panelIn)) * 20);
		int y = barHeight + 30;
		Redstone.window(c, x, y, PANEL_W, h);

		Redstone.iconWell(c, x + 6, y + 5, 1, module.icon(), t.dustOn, 1f);
		Paint.textClipped(c, module.name(), x + 23, y + 8, PANEL_W - 48, t.text, false);
		boolean closeHover = inside(mx, my, x + PANEL_W - 21, y + 5, 14, 14);
		Paint.iconButton(c, x + PANEL_W - 21, y + 5, 14, "close", closeHover, false);
		hits.add(x + PANEL_W - 21, y + 5, 14, 14, new Runnable() {
			@Override
			public void run() {
				selected = null;
			}
		});
		c.fill(x + 6, y + 21, x + PANEL_W - 6, y + 22, t.border);

		int ry = y + 26;
		ry = panel.draw(c, hits, settings, x + 8, ry, PANEL_W - 16, mx, my);

		boolean resetHover = inside(mx, my, x + 8, ry + 2, PANEL_W - 16, 16);
		Paint.button(c, x + 8, ry + 2, PANEL_W - 16, 16, I18n.tr("common.reset"), false, resetHover);
		final HudModule resetTarget = module;
		hits.add(x + 8, ry + 2, PANEL_W - 16, 16, new Runnable() {
			@Override
			public void run() {
				host.playClick();
				resetTarget.resetLayout();
				history.commit();
			}
		});
	}

	// --- Eingaben ---

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 1 && !hits.hovers(mouseX, mouseY)) {
			HudItem item = itemAt(mouseX, mouseY, lastWidth, lastHeight);
			if (item != null) {
				item.module().resetLayout();
				history.commit();
				return true;
			}
		}
		boolean hit = super.mouseClicked(mouseX, mouseY, button);
		// Nur ziehen, wenn der Klick nicht schon auf der Leiste oder im Einstellungsfeld gelandet ist.
		if (button == 0 && !hit) {
			HudItem item = itemAt(mouseX, mouseY, lastWidth, lastHeight);
			if (item != null && !hits.dragging()) {
				int[] b = bounds(item, lastWidth, lastHeight);
				dragging = item;
				selected = item;
				grabX = mouseX - b[0];
				grabY = mouseY - b[1];
				return true;
			}
		}
		return hit;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button) {
		if (dragging == null) return super.mouseDragged(mouseX, mouseY, button);
		int[] b = bounds(dragging, lastWidth, lastHeight);
		int w = b[2];
		int h = b[3];
		int x = (int) Math.round(mouseX - grabX);
		int y = (int) Math.round(mouseY - grabY);
		if (TouchMode.enabled()) {
			// Finger: größere Einrast-Distanz, Ränder = sichere Fläche (Notch, Kamera-Loch).
			SafeArea.Snap snap = SafeArea.snap(x, y, w, h, lastWidth, lastHeight, others(dragging), TOUCH_SNAP,
					TouchMode.insetsGui());
			guideX = snap.guideX == SafeArea.NO_GUIDE ? HudSnap.NO_GUIDE : snap.guideX;
			guideY = snap.guideY == SafeArea.NO_GUIDE ? HudSnap.NO_GUIDE : snap.guideY;
			dragging.module().position().set(HudLayout.fromPixels(snap.x, snap.y, w, h, lastWidth, lastHeight));
			return true;
		}
		HudSnap.Result result = host.shiftDown()
				? HudSnap.clampOnly(x, y, w, h, lastWidth, lastHeight)
				: HudSnap.snap(x, y, w, h, lastWidth, lastHeight, others(dragging), HudSnap.DISTANCE);
		guideX = result.guideX;
		guideY = result.guideY;
		dragging.module().position().set(HudLayout.fromPixels(result.x, result.y, w, h, lastWidth, lastHeight));
		return true;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		boolean was = dragging != null;
		dragging = null;
		guideX = HudSnap.NO_GUIDE;
		guideY = HudSnap.NO_GUIDE;
		boolean released = super.mouseReleased(mouseX, mouseY, button);
		history.commit();
		return released || was;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		// Über der Leiste oder dem Einstellungsfeld ändert das Mausrad nichts am Element darunter.
		if (hits.hovers(mouseX, mouseY)) return false;
		HudItem item = itemAt(mouseX, mouseY, lastWidth, lastHeight);
		if (item == null || amount == 0) return false;
		item.module().scale.nudge(amount > 0 ? 1 : -1);
		selected = item;
		history.commit();
		return true;
	}

	/** Kein Textfeld – keine Texteingabe (IME bliebe sonst unnötig an). */
	@Override
	public boolean wantsTextInput() {
		return false;
	}

	@Override
	public boolean keyPressed(int rawKey, UiKey key, boolean shift) {
		if (panel.captureKey(rawKey, key, host)) return true;
        if (key == UiKey.TAB) {
            List<HudItem> items = enabled();
            if (!items.isEmpty()) {
                int index = items.indexOf(selected);
                selected = items.get((index + (shift ? items.size() - 1 : 1) + items.size()) % items.size());
            }
            return true;
        }
        if (selected != null && dragging == null) {
            int dx = key == UiKey.LEFT ? -1 : key == UiKey.RIGHT ? 1 : 0;
            int dy = key == UiKey.UP ? -1 : key == UiKey.DOWN ? 1 : 0;
            if (dx != 0 || dy != 0) {
                int[] b = bounds(selected, lastWidth, lastHeight);
                HudSnap.Result next = HudSnap.clampOnly(b[0] + dx * (shift ? 10 : 1), b[1] + dy * (shift ? 10 : 1),
                        b[2], b[3], lastWidth, lastHeight);
                selected.module().position().set(HudLayout.fromPixels(next.x, next.y, b[2], b[3], lastWidth, lastHeight));
                history.commit();
                return true;
            }
        }
		if (key == UiKey.ESCAPE) {
			if (panel.collapse()) return true;
			if (selected != null) {
				selected = null;
				return true;
			}
			requestClose();
			return true;
		}
		return false;
	}

	/** Wählt das erste aktive Element aus (Selbsttest: zeigt das Einstellungsfeld). */
	public void selectFirst() {
		List<HudItem> items = enabled();
		if (!items.isEmpty()) {
			selected = items.get(0);
			panelIn = 1f;
		}
	}

	// --- Hilfen ---

	private int lastWidth;
	private int lastHeight;

	private List<HudItem> enabled() {
		List<HudItem> out = new ArrayList<HudItem>();
		List<HudItem> all = host.hudItems();
		for (int i = 0; i < all.size(); i++) {
			if (all.get(i).module().isEnabled() && all.get(i).module().available()) out.add(all.get(i));
		}
		return out;
	}

	private List<int[]> others(HudItem except) {
		List<int[]> out = new ArrayList<int[]>();
		List<HudItem> items = enabled();
		for (int i = 0; i < items.size(); i++) {
			HudItem item = items.get(i);
			if (item == except) continue;
			out.add(bounds(item, lastWidth, lastHeight).clone());
		}
		return out;
	}

	/** {x, y, Breite, Höhe} des Elements auf dem Bildschirm. */
	private int[] bounds(HudItem item, int screenW, int screenH) {
		float scale = item.module().scale.getFloat();
		int w = (int) Math.ceil(item.width() * scale);
		int h = (int) Math.ceil(item.height() * scale);
		int x = HudLayout.resolveX(item.module().position(), w, screenW);
		int y = HudLayout.resolveY(item.module().position(), h, screenH);
		return new int[]{x, y, w, h};
	}

	/** Oberstes Element unter der Maus. */
	private HudItem itemAt(double mx, double my, int screenW, int screenH) {
		HudItem found = null;
		List<HudItem> items = enabled();
		for (int i = 0; i < items.size(); i++) {
			int[] b = bounds(items.get(i), screenW, screenH);
			int pad = TouchMode.enabled() ? GRAB_PAD_TOUCH : GRAB_PAD;
			if (inside(mx, my, b[0] - pad, b[1] - pad, b[2] + 2 * pad, b[3] + 2 * pad)) found = items.get(i);
		}
		return found;
	}

	// --- Touch-Modus ---

	/** Einrast-Distanz für den Finger (größer als mit der Maus). */
	private static final int TOUCH_SNAP = 10;
	/** Höhe der Leiste im letzten Frame (das Seitenfeld liegt darunter). */
	private int barHeight = 22;

	/** HUD-Editor zeigt echte Bildschirmpositionen – nie vergrößern. */
	@Override
	protected float touchScale(int width, int height) {
		return 1f;
	}

	/** Im Editor bedient der Finger immer direkt (Ziehen der Elemente statt Scrollen). */
	@Override
	protected boolean touchDirect(double x, double y) {
		return true;
	}

	/** Unsichere Ränder (Notch) abdunkeln und die sichere Fläche als Staublinie zeigen. */
	private static void safeArea(Canvas c, Theme t, int width, int height) {
		SafeArea.Insets in = TouchMode.insetsGui();
		if (in.isZero()) return;
		int shade = ColorMath.withAlpha(0xFF000000, 90);
		if (in.left > 0) c.fill(0, 0, in.left, height, shade);
		if (in.right > 0) c.fill(width - in.right, 0, width, height, shade);
		if (in.top > 0) c.fill(in.left, 0, width - in.right, in.top, shade);
		if (in.bottom > 0) c.fill(in.left, height - in.bottom, width - in.right, height, shade);
		Redstone.frame(c, in.left, in.top, width - in.left - in.right, height - in.top - in.bottom,
				ColorMath.withAlpha(t.dustOff, 160));
	}

	/** Griffe an den Ecken (Finger-Ziel; das ganze Element bleibt greifbar). */
	private static void handles(Canvas c, int[] b, int color) {
		int s = 4;
		int x1 = b[0] - 2 - s / 2;
		int y1 = b[1] - 2 - s / 2;
		int x2 = b[0] + b[2] + 2 - s / 2;
		int y2 = b[1] + b[3] + 2 - s / 2;
		c.fill(x1, y1, x1 + s, y1 + s, color);
		c.fill(x2, y1, x2 + s, y1 + s, color);
		c.fill(x1, y2, x1 + s, y2 + s, color);
		c.fill(x2, y2, x2 + s, y2 + s, color);
	}

	/**
	 * Knopf „Touch-Layout“: wechselt in das gleichnamige Profil (legt es beim ersten Mal als Kopie an) und verteilt
	 * die aktiven Elemente – ausgehend von ihrer Standardposition – außerhalb der Overlay-Knöpfe und der unsicheren
	 * Ränder.
	 */
	public void applyTouchLayout() {
		HudProfiles profiles = host.modules().profiles;
		int existing = -1;
		for (int i = 0; i < profiles.size(); i++) {
			if (TouchLayout.PROFILE_NAME.equalsIgnoreCase(profiles.name(i))) existing = i;
		}
		if (existing >= 0) profiles.switchTo(existing);
        else if (profiles.create(TouchLayout.PROFILE_NAME) != null) return;
        history.clear();
		selected = null;
		int w = lastWidth;
		int h = lastHeight;
		if (w <= 0 || h <= 0) return;
		List<HudItem> items = enabled();
		List<int[]> rects = new ArrayList<int[]>();
		for (int i = 0; i < items.size(); i++) {
			HudItem item = items.get(i);
			float scale = item.module().scale.getFloat();
			int iw = (int) Math.ceil(item.width() * scale);
			int ih = (int) Math.ceil(item.height() * scale);
			HudModule m = item.module();
			rects.add(new int[]{HudLayout.resolveX(m.defaultPosition(), iw, w), HudLayout.resolveY(m.defaultPosition(), ih, h), iw, ih});
		}
		int[][] placed = TouchLayout.place(rects, w, h, TouchLayout.zones(w, h), TouchMode.insetsGui());
		for (int i = 0; i < items.size(); i++) {
			int[] r = rects.get(i);
			items.get(i).module().position().set(HudLayout.fromPixels(placed[i][0], placed[i][1], r[2], r[3], w, h));
		}
		history.commit();
		host.save();
	}

	/** Merkt sich die Bildschirmgröße für die Eingaben (die kommen ohne Größe). */
	public void setScreenSize(int width, int height) {
		lastWidth = width;
		lastHeight = height;
	}

	private static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}
}
