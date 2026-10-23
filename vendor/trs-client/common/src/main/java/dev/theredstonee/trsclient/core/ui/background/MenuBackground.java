package dev.theredstonee.trsclient.core.ui.background;

import dev.theredstonee.trsclient.core.ui.*;
import dev.theredstonee.trsclient.core.module.ChoiceSetting;

/** Local-media-only background controller. No decoder/download is silently installed. */
public final class MenuBackground implements AutoCloseable {
    public enum Mode implements ChoiceSetting.Option {
        HIGH("1080p video (static fallback)",1920,1080,30), BALANCED("Balanced (static fallback)",1280,720,24),
        PERFORMANCE("Performance (static fallback)",854,480,15), STATIC("Static image",0,0,0), OFF("Disabled",0,0,0);
        private final String label; public final int width,height,fps;
        Mode(String label,int width,int height,int fps){this.label=label;this.width=width;this.height=height;this.fps=fps;}
        public String label(){return label;}
    }
    /** Implementations decode off the render thread; latest() must never block or allocate a frame queue. */
    public interface Player extends AutoCloseable {
        void start(Mode mode) throws Exception;
        TextureRef latest();
        void close();
    }
    private static final MenuBackground SHARED=new MenuBackground();
    public static MenuBackground shared(){return SHARED;}
    private Mode mode=Mode.STATIC;
    private Player player;
    private boolean started, failed;
    private TextureRef still;
    public void setMode(Mode value) {
        Mode next=value==null?Mode.STATIC:value;
        if(next!=mode){close();mode=next;failed=false;}
    }
    public Mode mode(){return mode;}
    public boolean videoActive(){return started&&!failed;}
    public void install(Player value){close();player=value;failed=false;}
    public void draw(Canvas c,int width,int height,boolean enabled) {
        c.fill(0,0,width,height,0xFF080D18);
        if(!enabled||mode==Mode.OFF){close();return;}
        TextureRef frame=null;
        if(mode!=Mode.STATIC&&player!=null&&!failed) {
            try {if(!started){started=true;player.start(mode);}frame=player.latest();}
            catch(Exception | LinkageError e){failed=true;close();}
        }
        if(frame==null&&c.images()&&Textures.store()!=null) {
            try {if(still==null)still=Textures.store().game("trsclient:textures/menu/black-hole.png",1672,941);frame=still;}
            catch(RuntimeException | LinkageError e){frame=null;}
        }
        if(frame!=null&&c.images()) {
            try {
                float scale=Math.max(width/(float)frame.width,height/(float)frame.height);
                c.push();
                try {c.translate((width-frame.width*scale)/2,(height-frame.height*scale)/2);c.scale(scale);c.image(frame,0,0,frame.width,frame.height,0xFFFFFFFF);}
                finally {c.pop();}
                c.fill(0,0,width,height,0x60060B14);
            } catch(RuntimeException | LinkageError e) {c.fill(0,0,width,height,0xFF080D18);}
        }
    }
    /** Call when the title screen leaves; live video resources never survive into gameplay. */
    public void close(){if(started&&player!=null){try{player.close();}catch(RuntimeException | LinkageError ignored){}}started=false;}
}
