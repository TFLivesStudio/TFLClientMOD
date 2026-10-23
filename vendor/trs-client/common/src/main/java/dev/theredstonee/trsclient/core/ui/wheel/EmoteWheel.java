package dev.theredstonee.trsclient.core.ui.wheel;

import dev.theredstonee.trsclient.core.emote.EmoteController;
import dev.theredstonee.trsclient.core.emote.EmoteDef;
import dev.theredstonee.trsclient.core.emote.WheelMath;
import dev.theredstonee.trsclient.core.i18n.I18n;
import dev.theredstonee.trsclient.core.touch.TouchMode;
import dev.theredstonee.trsclient.core.ui.Anim;
import dev.theredstonee.trsclient.core.ui.Canvas;
import dev.theredstonee.trsclient.core.ui.ColorMath;
import dev.theredstonee.trsclient.core.ui.FadeCanvas;
import dev.theredstonee.trsclient.core.ui.Icons;
import dev.theredstonee.trsclient.core.ui.Paint;
import dev.theredstonee.trsclient.core.ui.Theme;
import dev.theredstonee.trsclient.core.ui.SelectionStyle;
import dev.theredstonee.trsclient.core.ui.UiKey;
import dev.theredstonee.trsclient.core.ui.UiScreen;

import java.util.ArrayList;
import java.util.List;

/** Native local emote picker. Hold and release, click, and touch all use the same radial selection. */
public final class EmoteWheel extends UiScreen {
	/** So kurz gedrückt = angetippt (Rad bleibt offen). */
	static final long TAP_MS = 250;

	/** Was das Rad vom Spiel braucht. */
	public interface Host {
		/** Ist die Rad-Taste noch gedrückt? null = nicht abfragbar (z. B. Maustaste) → Klick-Modus. */
		Boolean keyHeld();

		/** Anzeigename der Rad-Taste (für den Hinweis). */
		String keyName();

		/** Minecraft-Bildschirm schließen. */
		void close();

		/** Kurze Meldung über der Schnellleiste (Wartezeit, gesperrt). */
		void actionBar(String text);

		/** Klick-Ton. */
		void click();
	}

	private final EmoteController emotes;
	private final Host host;
	private final long openedAt = System.currentTimeMillis();
	private final List<EmoteDef> defs;
	private final float[] lit;
	private float hubLit;
	private int hovered = -1;
	/** Klick-Modus (Taste nur angetippt oder nicht abfragbar). */
	private boolean sticky;
	private boolean pressedAgain;
	private boolean decided;
	/** Selbsttest hat ein Emote vorgegeben (Maus zählt dann nicht). */
	private boolean forced;
	/** Letzte Geometrie (für Klicks). */
	private int centerX;
	private int centerY;
	private int deadZone = 18;
	private int[] slotX = new int[0];
	private int[] slotY = new int[0];
	private int slotSize;

	private final List<Object[]> texts = new ArrayList<>();

	public EmoteWheel(EmoteController emotes, Host host) {
		this.emotes = emotes;
		this.host = host;
		this.defs = dev.theredstonee.trsclient.core.wardrobe.EmoteSlots.forWheel(emotes.all());
		this.lit = new float[defs.size()];
		emotes.wheelOpened(openedAt);
	}

	// --- Eingabe ---

	private void checkKey(long now) {
		if (decided || isClosing()) return;
		// Touch: keine Taste zum Halten – das Rad bleibt offen, der Finger wählt.
		Boolean held = TouchMode.enabled() ? null : host.keyHeld();
		if (held == null) {
			sticky = true;
			return;
		}
		if (!sticky) {
			if (held) return;
			if (now - openedAt < TAP_MS) sticky = true;
			else confirm(now);
			return;
		}
		// Klick-Modus: zweiter Druck + Loslassen wählt.
		if (held) pressedAgain = true;
		else if (pressedAgain) confirm(now);
	}

	/** Das gezeigte Emote abspielen (falls eines gewählt ist) und schließen. */
	private void confirm(long now) {
		decided = true;
		if (hovered >= 0) play(hovered, now);
		requestClose();
	}

	private void play(int index, long now) {
		EmoteDef def = defs.get(index);
		EmoteController.Result r = emotes.play(def.id(), now);
		switch (r) {
			case PLAYED:
				host.click();
				break;
			case COOLDOWN:
				host.actionBar(I18n.tr("wheel.cooldown", Long.valueOf((emotes.cooldownLeft(now) + 999) / 1000)));
				break;
			case LOCKED:
				host.actionBar(I18n.tr("wheel.lockedToast"));
				break;
			default:
				break;
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (decided || isClosing()) return true;
		if (TouchMode.enabled() && button == 0) {
			// Finger aufgesetzt: erst beim Loslassen wählen (Ziehen zum Emote möglich).
			fingerDown = true;
			return true;
		}
		long now = System.currentTimeMillis();
		int i = pick(mouseX, mouseY);
		if (button == 0 && i >= 0) {
			if (!emotes.unlocked(defs.get(i).id())) return true;
			hovered = i;
			confirm(now);
			return true;
		}
		decided = true;
		requestClose();
		return true;
	}

	/** Touch: Finger liegt auf dem Rad (Auswahl beim Loslassen). */
	private boolean fingerDown;

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (!fingerDown || button != 0) return super.mouseReleased(mouseX, mouseY, button);
		fingerDown = false;
		if (decided || isClosing()) return true;
		releaseAt(mouseX, mouseY, System.currentTimeMillis());
		return true;
	}

	/** Touch: Finger losgelassen – Emote dort abspielen; gesperrt = Hinweis, Mitte/außerhalb = schließen. */
	private void releaseAt(double x, double y, long now) {
		int i = pick(x, y);
		if (i < 0) {
			decided = true;
			requestClose();
			return;
		}
		if (!emotes.unlocked(defs.get(i).id())) {
			host.actionBar(I18n.tr("wheel.lockedToast"));
			return;
		}
		hovered = i;
		confirm(now);
	}

	@Override
	protected boolean touchDirect(double x, double y) {
		return true;
	}

	/** Kein Textfeld – keine Texteingabe (IME bliebe sonst unnötig an). */
	@Override
	public boolean wantsTextInput() {
		return false;
	}

	@Override
	public boolean keyPressed(int rawKey, UiKey key, boolean shift) {
		if (key == UiKey.ESCAPE) {
			decided = true;
			requestClose();
			return true;
		}
		return false;
	}

	private int pick(double mx, double my) {
		if (emotes.state() != EmoteController.State.READY) return -1;
		return WheelMath.select(mx - centerX, my - centerY, defs.size(), deadZone);
	}

	@Override
	protected void onClosed() {
		host.close();
	}

	/** Welches Emote gerade gezeigt wird (−1 = keines) – für den Selbsttest. */
	public int hovered() {
		return hovered;
	}

	/** Selbsttest: Emote mit dieser ID zeigen (als stünde die Maus darauf). */
	public void hover(String id) {
		for (int i = 0; i < defs.size(); i++) {
			if (defs.get(i).id().equals(id)) {
				hovered = i;
				forced = true;
			}
		}
	}

	/** Selbsttest: gezeigtes Emote abspielen und schließen (wie Loslassen der Taste). */
	public void confirmNow() {
		if (!decided) confirm(System.currentTimeMillis());
	}

	// --- Zeichnen ---

	@Override
	protected void draw(Canvas raw, int width, int height, int mouseX, int mouseY, float dt) {
		long now = System.currentTimeMillis();
		Theme t = Theme.get();
		Canvas c = FadeCanvas.of(raw, alpha());
		texts.clear();
		c.fill(0, 0, width, height, 0x70000000);
		c.push();
		c.raise(300f);
		centerX = width / 2;
		centerY = height / 2;
		EmoteController.State state = emotes.state();
		if (state == EmoteController.State.READY || state == EmoteController.State.LOADING) {
			wheel(c, t, width, height, mouseX, mouseY, dt, now, state);
		} else {
			hint(c, t, width, height, state);
		}
		// Text zuletzt: Minecraft zeichnet ihn mit eigener Tiefe, später gezeichnete Flächen lägen darüber.
		for (Object[] tx : texts) {
			c.text((String) tx[0], (Integer) tx[1], (Integer) tx[2], (Integer) tx[3], (Boolean) tx[4]);
		}
		c.pop();
		checkKey(now);
	}

	private void text(String s, int x, int y, int argb, boolean shadow) {
		texts.add(new Object[] {s, Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(argb), Boolean.valueOf(shadow)});
	}

	private void centered(Canvas c, String s, int cx, int y, int argb, int maxWidth) {
		String clipped = c.textWidth(s) <= maxWidth ? s : c.clip(s, maxWidth);
		text(clipped, cx - c.textWidth(clipped) / 2, y, argb, false);
	}

	private void wheel(Canvas c, Theme t, int width, int height, int mouseX, int mouseY, float dt, long now,
			EmoteController.State state) {
		int n = defs.size();
		boolean big = Math.min(width, height) >= 300;
		int s = big ? 28 : 22;
		slotSize = s;
		int hub = big ? 30 : 24;
		deadZone = hub / 2 + 4;
		float grow = 0.55f + 0.45f * Anim.easeOut(open);
		int r = Math.round(WheelMath.radius(width, height, n, s) * grow);
		if (slotX.length != n) {
			slotX = new int[n];
			slotY = new int[n];
		}
		for (int i = 0; i < n; i++) {
			slotX[i] = WheelMath.slotX(centerX, r, i, n);
			slotY[i] = WheelMath.slotY(centerY, r, i, n);
		}
		if (!decided && !forced && state == EmoteController.State.READY && (!TouchMode.enabled() || fingerDown)) {
			int pick = WheelMath.select(mouseX - centerX, mouseY - centerY, n, deadZone);
			if (pick >= 0 || !sticky) hovered = pick;
		}
		if (state != EmoteController.State.READY) hovered = -1;
		boolean hoveredUnlocked = hovered >= 0 && emotes.unlocked(defs.get(hovered).id());
		hubLit = Anim.approach(hubLit, hoveredUnlocked ? 1f : 0.15f, dt, 0.06f);

        // A quiet central anchor keeps the radial arrangement readable without pixel wiring.
        int hx = centerX - hub / 2;
        int hy = centerY - hub / 2;
        SelectionStyle.panel(c, hx, hy, hub, hub, t.surface, ColorMath.lerp(t.border, t.accent, hubLit));
        int hpx = Math.max(1, (hub - 10) / 8);
        Icons.draw(c, "wave", centerX - hpx * 4, centerY - hpx * 4, hpx, t.text);

		// Emotes: freigeschaltet = Lampe, gesperrt = dunkler Stein mit Schloss.
		int px = Math.max(1, (s - 8) / 8);
		for (int i = 0; i < n; i++) {
			EmoteDef def = defs.get(i);
			boolean unlocked = emotes.unlocked(def.id());
			boolean sel = i == hovered;
			lit[i] = Anim.approach(lit[i], sel && unlocked ? 1f : 0f, dt, 0.05f);
			int x = slotX[i] - s / 2;
			int y = slotY[i] - s / 2 - (sel ? 1 : 0);
			if (unlocked) {
				SelectionStyle.panel(c, x, y, s, s, ColorMath.lerp(t.surface, t.accent, lit[i]), ColorMath.lerp(t.border, t.accent, lit[i]));
				Icons.draw(c, def.icon(), slotX[i] - px * 4, y + (s - px * 8) / 2, px, t.text);
			} else {
				SelectionStyle.panel(c, x, y, s, s, sel ? t.surfaceHover : t.surface, t.border);
				Icons.draw(c, def.icon(), slotX[i] - px * 4, y + (s - px * 8) / 2, px, ColorMath.withAlpha(t.textDim, 110));
				Icons.draw(c, "lock", x + s - 9, y + s - 9, 1, t.textDim);
			}
		}

		// Beschriftung: Titel oben, gewähltes Emote unter der Nabe, Bedienhinweis unten.
		int inner = Math.max(40, 2 * r - s - 10);
		int top = centerY - r - s / 2 - 14;
		if (top >= 2) centered(c, I18n.tr("wheel.title"), centerX, top, t.text, width - 8);
		int labelY = centerY + hub / 2 + 5;
		long cooldown = emotes.cooldownLeft(now);
		if (state == EmoteController.State.LOADING) {
			centered(c, I18n.tr("wheel.loading"), centerX, labelY, t.textDim, inner);
		} else if (hovered >= 0) {
			EmoteDef def = defs.get(hovered);
			boolean unlocked = emotes.unlocked(def.id());
			centered(c, I18n.trOr(def.nameKey(), def.id()), centerX, labelY, unlocked ? t.text : t.textDim, inner);
			if (!unlocked) centered(c, I18n.tr("wheel.locked"), centerX, labelY + 10, t.textDim, inner);
			else if (cooldown > 0) centered(c, I18n.tr("wheel.cooldown", Long.valueOf((cooldown + 999) / 1000)),
					centerX, labelY + 10, t.textDim, inner);
		} else {
			centered(c, I18n.tr("wheel.pick"), centerX, labelY, t.textDim, inner);
		}
		int bottom = centerY + r + s / 2 + 6;
		if (bottom + 9 <= height - 2) {
			String hint = TouchMode.enabled() ? I18n.tr("wheel.hintTouch")
					: sticky ? I18n.tr("wheel.hintClick") : I18n.tr("wheel.hintHold", host.keyName());
			centered(c, hint, centerX, bottom, t.textDim, width - 8);
		}
	}

	/** Kein Rad möglich (Einwilligung, Konto, Verbindung): kleines Fenster mit Hinweis. */
	private void hint(Canvas c, Theme t, int width, int height, EmoteController.State state) {
		String message;
		switch (state) {
			case OFF_LAUNCHER:
				message = I18n.tr("wheel.offLauncher");
				break;
			case OFF_MODULE:
				message = I18n.tr("wheel.offModule");
				break;
			case NO_ACCOUNT:
				message = I18n.tr("wheel.noAccount");
				break;
			case BANNED:
				message = I18n.tr("wheel.banned");
				break;
			case RETRY:
				message = I18n.tr("wheel.retry");
				break;
			default:
				message = I18n.tr("wheel.connecting");
				break;
		}
		int w = Math.min(240, width - 16);
		List<String> lines = Paint.wrap(c, message, w - 20);
		int h = 44 + lines.size() * 10;
		int x = centerX - w / 2;
		int y = centerY - h / 2 + Math.round((1 - Anim.easeOut(open)) * 10);
		SelectionStyle.panel(c, x, y, w, h, t.surface, t.border);
		Icons.draw(c, "wave", x + 11, y + 10, 1, t.accent);
		text(I18n.tr("wheel.title"), x + 28, y + 10, t.text, false);
		c.fill(x + 10, y + 26, x + w - 10, y + 27, t.border);
		for (int i = 0; i < lines.size(); i++) text(lines.get(i), x + 10, y + 33 + i * 10, t.textDim, false);
	}
}
