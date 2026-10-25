package dev.theredstonee.trsclient.core.ui.background;

import dev.theredstonee.trsclient.core.ui.*;
import dev.theredstonee.trsclient.core.module.ChoiceSetting;

/** Local-media-only background controller. No decoder/download is silently installed. */
public final class MenuBackground implements AutoCloseable {
    public enum Mode implements ChoiceSetting.Option {
        HIGH("Quality · local video",1920,1080,24), BALANCED("Balanced · local video",1280,720,24),
        PERFORMANCE("Performance · local video",854,480,15), STATIC("Static image",0,0,0), OFF("Disabled",0,0,0);
        private final String label; public final int width,height,fps;
        Mode(String label,int width,int height,int fps){this.label=label;this.width=width;this.height=height;this.fps=fps;}
        public String label(){return label;}
    }
    /** Implementations decode off the render thread; latest() must never block or allocate a frame queue. */
    public interface Player extends AutoCloseable {
        void start(Mode mode) throws Exception;
        TextureRef latest();
        void close();
        default long uploadedSequence(){return 0;}
        default PlaybackMetrics metrics(){return new PlaybackMetrics();}
    }
    private static final MenuBackground SHARED=new MenuBackground();
    public static MenuBackground shared(){return SHARED;}
    private Mode mode=Mode.STATIC;
    private Player player;
    private boolean started, failed, presented;
    private long presentedFrames,lastPresentedSequence=-1,startedAt;
    private String playbackError="";
    private String fallbackReason="Static mode selected",animationReason="";
    public void animationAllowed(boolean allowed){animationReason=allowed?"":"Animation disabled / reduced motion";}
    public PlaybackMetrics metrics(){PlaybackMetrics m=player==null?new PlaybackMetrics():player.metrics();m.mode=mode.name();m.framesPresented=presentedFrames;m.fallbackReason=fallbackReason;m.presentationFps=!videoActive()||startedAt==0?0:presentedFrames*1e9/Math.max(1,System.nanoTime()-startedAt);return m;}
    public String status(){PlaybackMetrics m=metrics();if(!m.fallbackReason.isEmpty())return m.fallbackReason;return "Video · "+Math.round(m.presentationFps)+" fps";}
    private TextureRef still;
    public void setMode(Mode value) {
        Mode next=value==null?Mode.STATIC:value;
        if(next!=mode){close();mode=next;failed=false;}
    }
    public Mode mode(){return mode;}
    public boolean videoActive(){return started&&!failed;}
    public boolean videoFramePresented(){return presented;}
    public void install(Player value){close();player=value;failed=false;}
    public void draw(Canvas c,int width,int height,boolean enabled) {
        c.fill(0,0,width,height,0xFF080D18);
        if(!enabled||mode==Mode.OFF){fallbackReason=!enabled?"Background disabled":"Off mode selected";close();return;}
        fallbackReason=!animationReason.isEmpty()?animationReason:mode==Mode.STATIC?"Static mode selected":player==null?"Video player not installed":"Waiting for decoded frame";
        TextureRef frame=null;
        if(mode!=Mode.STATIC&&player!=null&&!failed) {
            try {if(!started){started=true;presentedFrames=0;lastPresentedSequence=-1;startedAt=System.nanoTime();player.start(mode);}frame=player.latest();presented=frame!=null;}
            catch(Exception | LinkageError e){playbackError="Video error: "+e.getClass().getSimpleName()+" · "+e.getMessage();fallbackReason=playbackError;failed=true;close();}
        }
        if(failed)fallbackReason=playbackError;
        if(frame==null&&c.images()&&Textures.store()!=null) {
            try {if(still==null)still=Textures.store().game("trsclient:textures/menu/black-hole.png",1920,1080);frame=still;}
            catch(RuntimeException | LinkageError e){frame=null;}
        }
        if(frame!=null&&c.images()) {
            try {
                float scale=Math.max(width/(float)frame.width,height/(float)frame.height);
                c.push();
                try {c.translate((width-frame.width*scale)/2,(height-frame.height*scale)/2);c.scale(scale);c.image(frame,0,0,frame.width,frame.height,0xFFFFFFFF);
                    if(presented&&player!=null){fallbackReason="";long sequence=player.uploadedSequence();if(sequence!=lastPresentedSequence){lastPresentedSequence=sequence;presentedFrames++;}}}
                finally {c.pop();}
                c.fill(0,0,width,height,0x60060B14);
            } catch(RuntimeException | LinkageError e) {playbackError="Video composition error: "+e.getClass().getSimpleName();fallbackReason=playbackError;failed=true;close();c.fill(0,0,width,height,0xFF080D18);}
        }
    }
    /** Call when the title screen leaves; live video resources never survive into gameplay. */
    public void close(){if(started&&player!=null){try{player.close();}catch(RuntimeException | LinkageError ignored){}}started=false;presented=false;}
}
