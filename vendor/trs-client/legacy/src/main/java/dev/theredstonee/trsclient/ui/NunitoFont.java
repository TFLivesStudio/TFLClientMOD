package dev.theredstonee.trsclient.ui;

import dev.theredstonee.trsclient.core.ui.TextureRef;
import dev.theredstonee.trsclient.core.ui.Textures;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

/** TFL Client: bounded, supersampled local Nunito atlas for the Java 8 renderer. */
final class NunitoFont {
    private static final int FIRST=32, LAST=384, CELL=52, COLS=24;
    private TextureRef atlas;
    private final int[] advances=new int[LAST];
    private int generation=-1;
    private boolean failed;
    boolean ready() {
        int current=GfxImage.Store.INSTANCE.resourceGeneration();
        if(current!=generation) { if(atlas!=null)GfxImage.Store.INSTANCE.release(atlas);atlas=null;failed=false;generation=current; }
        if(atlas!=null)return true;
        if(failed)return false;
        try {
            byte[] bytes=GfxImage.Store.INSTANCE.gameBytes("trsclient:font/nunito-regular.ttf");
            Font font=Font.createFont(Font.TRUETYPE_FONT,new ByteArrayInputStream(bytes)).deriveFont(36f);
            int rows=(LAST-FIRST+COLS-1)/COLS;
            BufferedImage image=new BufferedImage(COLS*CELL,rows*CELL,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=image.createGraphics();
            try {
                g.setFont(font);g.setColor(java.awt.Color.WHITE);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                for(int ch=FIRST;ch<LAST;ch++) {
                    int n=ch-FIRST,x=n%COLS*CELL,y=n/COLS*CELL;
                    advances[ch]=(int)Math.ceil(font.getStringBounds(String.valueOf((char)ch),g.getFontRenderContext()).getWidth());
                    g.drawString(String.valueOf((char)ch),x+2,y+2+g.getFontMetrics().getAscent());
                }
            } finally {g.dispose();}
            atlas=GfxImage.Store.INSTANCE.upload("nunito-font",image.getWidth(),image.getHeight(),image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth()));
            return atlas!=null;
        } catch(Exception | LinkageError e) {failed=true;return false;}
    }
    int width(String text) {
        float x=0;
        for(int i=0;i<text.length();i++) {
            char ch=text.charAt(i);
            if(ch=='\u00a7'&&i+1<text.length()){i++;continue;}
            x+=advances[ch>=FIRST&&ch<LAST?ch:'?']/4f;
        }
        return (int)Math.ceil(x);
    }
    void draw(GfxCanvas c,String text,int x,int y,int color) {
        float cursor=x;
        for(int i=0;i<text.length();i++) {
            char ch=text.charAt(i);
            if(ch=='\u00a7'&&i+1<text.length()){i++;continue;}
            if(ch<FIRST||ch>=LAST)ch='?';
            int n=ch-FIRST;
            c.push();c.translate(cursor-0.5f,y-0.5f);c.scale(0.25f);
            c.image(atlas,n%COLS*CELL,n/COLS*CELL,CELL,CELL,color);c.pop();
            cursor+=advances[ch]/4f;
        }
    }
}
