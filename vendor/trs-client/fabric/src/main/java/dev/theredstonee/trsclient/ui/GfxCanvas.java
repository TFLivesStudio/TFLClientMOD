package dev.theredstonee.trsclient.ui;

import dev.theredstonee.trsclient.core.ui.Canvas;
import dev.theredstonee.trsclient.core.ui.TextureRef;
import dev.theredstonee.trsclient.core.ui.Textures;
import net.minecraft.client.gui.Font;

/**
 * Verbindet die versionsunabhängige Oberfläche ({@link Canvas}) mit dem versionsabhängigen
 * Zeichnen ({@link Gfx}). Mehr braucht {@code core.ui} nicht von Minecraft.
 */
public final class GfxCanvas implements Canvas {
	private static final GfxCanvas INSTANCE = new GfxCanvas();

	static {
		// Texturen für core.ui (Skins, Umhänge, Vorschaubilder) – ab dem ersten Zeichnen verfügbar.
		Textures.install(TextureStore.INSTANCE);
	}

	private Gfx g;
	/** Seit dem letzten Textur-Bild wurden Flächen/Text gesammelt (Reihenfolge 1.20–1.21.1). */
	private boolean pending = true;
	private Font font;
    // Native Minecraft font provider; no browser or separate font renderer.
    //? if >=1.16 {
    private final java.util.Map<String,net.minecraft.network.chat.Component> styledText = new java.util.LinkedHashMap<>();
    private net.minecraft.network.chat.Component selectionText(String value) {return selectionText(value,"ui");}
    private net.minecraft.network.chat.Component selectionText(String value,String face) {
        net.minecraft.network.chat.Component cached=styledText.get(face+"\n"+value);
        if(cached!=null)return cached;
        //? if >=1.19 {
        net.minecraft.network.chat.MutableComponent component=net.minecraft.network.chat.Component.literal(value);
        //?} else
        /*net.minecraft.network.chat.MutableComponent component=new net.minecraft.network.chat.TextComponent(value);*/
        //? if >=1.21.9 {
        /*component.withStyle(style -> style.withFont(new net.minecraft.network.chat.FontDescription.Resource(dev.theredstonee.trsclient.TrsClient.id(face))));
        *///?} else
        component.withStyle(style -> style.withFont(dev.theredstonee.trsclient.TrsClient.id(face)));
        if(styledText.size()>=512)styledText.clear();
        styledText.put(face+"\n"+value,component);
        return component;
    }
    //?}


	private GfxCanvas() {
	}

	/** Wiederverwendete Instanz für das aktuelle Frame (Rendern ist single-threaded). */
	public static GfxCanvas of(Gfx g, Font font) {
		INSTANCE.g = g;
		INSTANCE.font = font;
		INSTANCE.pending = true;
		return INSTANCE;
	}

	/** Instanz des laufenden Bilds (z. B. für die Spieler-Vorschau im Menü). */
	public static GfxCanvas current() {
		return INSTANCE;
	}

	/** Darunterliegende Zeichenfläche (für HUD-Elemente, die Minecraft-Dinge zeichnen). */
	public Gfx gfx() {
		return g;
	}

	public Font font() {
		return font;
	}

	@Override
	public void fill(int x1, int y1, int x2, int y2, int argb) {
		g.fill(x1, y1, x2, y2, argb);
		pending = true;
	}

	@Override
	public void text(String text, int x, int y, int argb, boolean shadow) {
		//? if >=1.16 {
        g.text(font, selectionText(text), x, y, argb, false);
        //?} else
        /*g.text(font,text,x,y,argb,shadow);*/
		pending = true;
	}

    @Override public void strongText(String text,int x,int y,int color) {
        //? if >=1.16 {
        g.text(font,selectionText(text,"ui-bold"),x,y,color,false);
        //?} else
        /*text(text,x,y,color,false);*/
        pending=true;
    }

	@Override
	public int textWidth(String text) {
		//? if >=1.16 {
        return font.width(selectionText(text));
        //?} else
        /*return font.width(text);*/
	}

	@Override
	public int lineHeight() {
		return 9;
	}

	@Override
	public String clip(String text, int maxWidth) {
		if(text==null)return "";
        int low=0,high=text.length();
        while(low<high){int mid=(low+high+1)/2;if(textWidth(text.substring(0,mid))<=Math.max(0,maxWidth))low=mid;else high=mid-1;}
        if(low>0&&low<text.length()&&Character.isHighSurrogate(text.charAt(low-1)))low--;
        return text.substring(0,low);
	}

	@Override
	public void flush() {
		g.flush();
	}

	@Override
	public void scissor(int x1, int y1, int x2, int y2) {
		g.scissor(x1, y1, x2, y2);
	}

	@Override
	public void noScissor() {
		g.noScissor();
	}

	@Override
	public void raise(float z) {
		g.raise(z);
	}

	@Override
	public void push() {
		g.push();
	}

	@Override
	public void translate(float x, float y) {
		g.translate(x, y);
	}

	@Override
	public void scale(float factor) {
		g.scale(factor);
	}

	@Override
	public void pop() {
		g.pop();
	}

	@Override
	public boolean images() {
		return true;
	}

	@Override
	public void image(TextureRef texture, float u, float v, int w, int h, int argb) {
		GfxImage.blit(g.raw(), texture, u, v, w, h, argb, pending);
		pending = false;
	}

	@Override
	public void item(Object stack, int x, int y) {
		if (!(stack instanceof net.minecraft.world.item.ItemStack)) return;
		g.item(font, (net.minecraft.world.item.ItemStack) stack, x, y);
		pending = true;
	}

	@Override
	public void rotate(float radians) {
		GfxImage.rotate(g.raw(), radians);
	}

	@Override
	public void scale(float sx, float sy) {
		GfxImage.scale(g.raw(), sx, sy);
	}
}
