import dev.theredstonee.trsclient.core.ui.*;
import dev.theredstonee.trsclient.core.ui.title.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Software layout preview only; never represents a Minecraft runtime screenshot. */
public final class RenderSelection {
    static final class Surface implements Canvas {
        final BufferedImage image; Graphics2D g; final Font font;
        final Deque<Graphics2D> stack=new ArrayDeque<>();
        Surface(int w,int h,Font font) {
            image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);g=image.createGraphics();this.font=font;
            g.setFont(font);g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
        public void fill(int a,int b,int c,int d,int color){g.setColor(new Color(color,true));g.fillRect(a,b,c-a,d-b);}
        public void text(String s,int x,int y,int color,boolean shadow){g.setColor(new Color(color,true));g.drawString(s,x,y+g.getFontMetrics().getAscent());}
        public int textWidth(String s){return g.getFontMetrics(font).stringWidth(s);}
        public int lineHeight(){return 10;}
        public String clip(String s,int width){int end=s.length();while(end>0&&textWidth(s.substring(0,end))>width)end--;return s.substring(0,end);}
        public void flush(){}public void scissor(int a,int b,int c,int d){g.setClip(a,b,c-a,d-b);}public void noScissor(){g.setClip(null);}public void raise(float z){}
        public void push(){stack.push(g);g=(Graphics2D)g.create();}public void pop(){g.dispose();g=stack.pop();}
        public void translate(float x,float y){g.translate(x,y);}public void scale(float value){g.scale(value,value);}
        public boolean images(){return true;}
        public void image(TextureRef t,float u,float v,int w,int h,int color){g.drawImage((BufferedImage)t.id,0,0,w,h,(int)u,(int)v,(int)u+w,(int)v+h,null);}
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=2)throw new IllegalArgumentException("Repository and output directory required");
        Path root=Paths.get(args[0]),out=Paths.get(args[1]);Files.createDirectories(out);
        Path assets=root.resolve("vendor/trs-client/common/src/main/resources/assets/trsclient");
        Font font=Font.createFont(Font.TRUETYPE_FONT,assets.resolve("font/inter.ttf").toFile()).deriveFont(10f);
        final BufferedImage background=ImageIO.read(assets.resolve("textures/menu/black-hole.png").toFile());
        Textures.replaceForTests(new Textures.Store(){
            public TextureRef upload(String name,int w,int h,int[] data){BufferedImage b=new BufferedImage(w,h,2);b.setRGB(0,0,w,h,data,0,w);return new TextureRef(b,w,h);}
            public void release(TextureRef texture){}public TextureRef game(String id,int w,int h){return new TextureRef(background,background.getWidth(),background.getHeight());}
            public Textures.DefaultSkin defaultSkin(java.util.UUID id){return null;}
        });
        dev.theredstonee.trsclient.core.i18n.I18n.use("en");
        TitleHost host=new TitleHost(){
            public void singleplayer(){}public void multiplayer(){}public void options(){}public void trsMenu(){}
            public boolean hasMods(){return false;}public void mods(){}public void quit(){}public void classicTitle(){}
            public String versionLine(){return "Minecraft 26.3 · TFL Core 0.4.0-dev.1";}public void playClick(){}public void narrate(String s){}
            public boolean animated(){return false;}public boolean simpleAnimation(){return true;}
        };
        for(int[] size:new int[][]{{854,480},{427,240},{213,120}}) {
            Surface surface=new Surface(size[0],size[1],font);TitleUi ui=new TitleUi(host);
            for(int i=0;i<30;i++){ui.render(surface,size[0],size[1],-1,-1);Thread.sleep(12);}
            ImageIO.write(surface.image,"png",out.resolve("software-selection-"+size[0]+"x"+size[1]+".png").toFile());surface.g.dispose();
        }
        System.out.println("Software Canvas previews written; these are not Minecraft screenshots.");
    }
}
