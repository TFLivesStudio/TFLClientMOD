package dev.theredstonee.trsclient.core.ui;

/**
 * Einzeiliges Eingabefeld (Suche, Profilnamen) – eigener Code, weil Vanilla-Textfelder
 * in jeder Minecraft-Version anders angelegt und gezeichnet werden.
 */
public final class TextInput implements dev.theredstonee.trsclient.core.touch.TouchKeyboard.Field {
	private final StringBuilder text = new StringBuilder();
	private final int maxLength;
	private int cursor;
	private boolean focused;
	private boolean selectedAll;

	public TextInput(int maxLength) {
		this.maxLength = Math.max(1, maxLength);
	}

	public String text() {
		return text.toString();
	}

	public void setText(String value) {
		text.setLength(0);
		selectedAll = false;
		if (value != null) {
			for (int i = 0; i < value.length() && text.length() < maxLength; i++) {
				char c = value.charAt(i);
				if (allowed(c)) text.append(c);
			}
		}
		cursor = text.length();
	}

	public boolean isEmpty() {
		return text.length() == 0;
	}

	public int cursor() {
		return cursor;
	}

	@Override
	public boolean focused() {
		return focused;
	}

	public void setFocused(boolean focused) {
		this.focused = focused;
		if (!focused) selectedAll = false;
		if (focused) cursor = text.length();
		// Touch-Modus: Bildschirmtastatur anfordern (sonst nichts).
		dev.theredstonee.trsclient.core.touch.TouchKeyboard.focus(this, focused, false);
	}

	public void clear() {
		text.setLength(0);
		selectedAll = false;
		cursor = 0;
	}

	/** Zeichen einfügen; liefert true, wenn es übernommen wurde. */
	public boolean type(char c) {
		if (!allowed(c)) return false;
		if (selectedAll) clear();
		if (text.length() >= maxLength) return false;
		text.insert(Math.min(cursor, text.length()), c);
		cursor = Math.min(cursor + 1, text.length());
		return true;
	}

	/** Taste verarbeiten; liefert true, wenn sie verbraucht wurde. */
	public boolean key(UiKey key) {
		switch (key) {
			case SELECT_ALL:
				selectedAll = true;
				return true;
			case BACKSPACE:
				if (selectedAll) { clear(); return true; }
				if (cursor > 0) {
					text.deleteCharAt(cursor - 1);
					cursor--;
				}
				return true;
			case DELETE:
				if (selectedAll) { clear(); return true; }
				if (cursor < text.length()) text.deleteCharAt(cursor);
				return true;
			case LEFT:
				selectedAll = false;
				cursor = Math.max(0, cursor - 1);
				return true;
			case RIGHT:
				selectedAll = false;
				cursor = Math.min(text.length(), cursor + 1);
				return true;
			case HOME:
				selectedAll = false;
				cursor = 0;
				return true;
			case END:
				selectedAll = false;
				cursor = text.length();
				return true;
			default:
				return false;
		}
	}

	/** Druckbares Zeichen? (Steuerzeichen und Formatierungszeichen werden abgelehnt.) */
	public static boolean allowed(char c) {
		return c >= ' ' && c != 127 && c != '§';
	}
}
