package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.hud.miniplayer.*;
import dev.theredstonee.trsclient.core.module.HudModule;
import dev.theredstonee.trsclient.core.ui.*;
import dev.theredstonee.trsclient.core.ui.menu.*;
import net.minecraft.client.render.pipeline.RenderTarget;
import net.minecraft.client.render.platform.*;
import org.lwjgl.opengl.GL11;

/** Live native player into a private, cached depth framebuffer; never creates or ticks an entity. */
public final class OrnitheMiniPlayer implements HudItem,AutoCloseable {
    private final MiniPlayerLightingState lightingState=new MiniPlayerLightingState();
    private final OrnitheClient c;private OrnitheCanvas guard;private RenderTarget target;private TextureRef texture;
    public final MiniPlayerState state=new MiniPlayerState();
    private final java.nio.IntBuffer viewport=org.lwjgl.BufferUtils.createIntBuffer(16);
    private final java.nio.FloatBuffer clear=org.lwjgl.BufferUtils.createFloatBuffer(16),light=org.lwjgl.BufferUtils.createFloatBuffer(16);
    private Object world,player;private int resourceGeneration=-1;
    public long composites;
    public OrnitheMiniPlayer(OrnitheClient c){this.c=c;}
    public HudModule module(){return c.modules.miniPlayer;}
    public int width(){return 44;}public int height(){return 74;}
    public void draw(Canvas canvas){
        MiniPlayerModule m=c.modules.miniPlayer;var p=c.mc.player;
        boolean editor=c.mc.screen instanceof CoreScreen cs&&cs.ui instanceof HudEditor;
        if(p==null){if(editor)canvas.text("Mini Player",0,24,0xFFB8C8DC,false);close();return;}
        if(world!=c.mc.world||player!=p||resourceGeneration!=c.canvas.resourceGeneration()){close();world=c.mc.world;player=p;resourceGeneration=c.canvas.resourceGeneration();}
        boolean moving=Math.abs(p.x-p.lastX)+Math.abs(p.y-p.lastY)+Math.abs(p.z-p.lastZ)>.001||p.armSwinging||!p.onGround;
        boolean emote=c.emotes!=null&&c.emotes.controller.selfPlaying(System.currentTimeMillis());
        if(!m.shouldDraw(editor,true,moving||emote,c.mc.options.hideGui,c.mc.screen!=null,true))return;
        if(!GLX.useFbo()){state.failure="Native framebuffer unavailable";return;}
        int fw=Math.max(64,Math.min(304,Math.round(width()*c.window().getScale()*m.scale.getFloat()*2)));
        int fh=Math.max(96,Math.min(512,Math.round(height()*c.window().getScale()*m.scale.getFloat()*2)));
        long key=MiniPlayerState.optionsKey(m);key=key*31+p.ticks/20;key=key*31+(p.isSneaking()?1:0);key=key*31+System.identityHashCode(p.inventory.getSelectedItem());key=key*31+p.getSkinTextureLocation().hashCode();key=key*31+java.util.Arrays.hashCode(p.inventory.armor);
        if(target==null||target.width!=fw||target.height!=fh){state.invalidate();key++;}
        if(state.update(c.mc.world,p,key,true,System.nanoTime(),m)){
            long start=System.nanoTime();try{render(fw,fh,m,state.yaw());state.failure="";}catch(RuntimeException|LinkageError e){state.failure=e.toString();}finally{state.nanos+=System.nanoTime()-start;}
        }
        if(texture==null)return;
        if(m.background.get())SelectionStyle.panel(canvas,0,0,width(),height(),m.backgroundArgb(),0x404C7296);
        boolean cull=GL11.glIsEnabled(GL11.GL_CULL_FACE);GlStateManager.disableCull();
        canvas.push();try{canvas.translate(0,height());canvas.scale(width()/(float)texture.width,-height()/(float)texture.height);canvas.image(texture,0,0,texture.width,texture.height,Math.round(m.opacity.getFloat()*2.55f)<<24|0xFFFFFF);}finally{canvas.pop();if(cull)GlStateManager.enableCull();}
        composites++;
    }
    private void render(int fw,int fh,MiniPlayerModule settings,float angle){
        if(guard==null)guard=new OrnitheCanvas(c.mc);
        lightingState.capture();int oldActive=GL11.glGetInteger(0x84E0);
        int oldFbo=GL11.glGetInteger(0x8CA6),oldRbo=GL11.glGetInteger(0x8CA7);viewport.clear();GL11.glGetInteger(GL11.GL_VIEWPORT,viewport);clear.clear();GL11.glGetFloat(GL11.GL_COLOR_CLEAR_VALUE,clear);
        double oldDepthClear=GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
        GlStateManager.activeTexture(GLX.GL_TEXTURE1);int lightTex=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);boolean lightEnabled=GL11.glIsEnabled(GL11.GL_TEXTURE_2D);light.clear();GL11.glGetFloat(GL11.GL_CURRENT_TEXTURE_COORDS,light);GlStateManager.activeTexture(GLX.GL_TEXTURE0);
        guard.begin(c.window().getWidth(),c.window().getHeight(),c.window().getScale());boolean projection=false;
        var dispatcher=c.mc.getEntityRenderDispatcher();float cameraYaw=dispatcher.cameraYaw,cameraPitch=dispatcher.cameraPitch;boolean shadow=dispatcher.shouldRenderShadow(),hitboxes=dispatcher.shouldRenderHitboxes();
        var p=c.mc.player;float by=p.bodyYaw,lby=p.lastBodyYaw,head=p.headYaw,lastHead=p.lastHeadYaw,yaw=p.yaw,lastYaw=p.lastYaw;
        MiniPlayerModule previous=MiniPlayerRenderScope.enter(settings);
        try{
            if(target==null||target.width!=fw||target.height!=fh){if(target!=null)target.destroyBuffers();target=new RenderTarget(fw,fh,true);target.setFilterMode(GL11.GL_LINEAR);texture=new TextureRef(Integer.valueOf(target.colorTextureId),fw,fh);}
            GL11.glDisable(GL11.GL_SCISSOR_TEST);target.setClearColor(0,0,0,0);target.clear();target.bindWrite(true);
            GlStateManager.matrixMode(GL11.GL_PROJECTION);GlStateManager.pushMatrix();projection=true;GlStateManager.loadIdentity();GlStateManager.ortho(0,fw,fh,0,-1000,1000);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);GlStateManager.loadIdentity();GlStateManager.enableDepthTest();GlStateManager.depthMask(true);GlStateManager.enableColorMaterial();
            GlStateManager.translatef(fw/2f,fh*.92f,0);float size=fh*.83f/Math.max(.5f,p.height);GlStateManager.scalef(-size,size,size);GlStateManager.rotatef(180,0,0,1);
            GlStateManager.rotatef(135,0,1,0);Lighting.turnOn();GlStateManager.rotatef(-135,0,1,0);
            float relative=wrap(head-by),relativeOld=wrap(lastHead-lby);
            p.bodyYaw=p.lastBodyYaw=p.yaw=p.lastYaw=angle;p.headYaw=angle+relative;p.lastHeadYaw=angle+relativeOld;
            dispatcher.setCameraYaw(180);dispatcher.cameraPitch=0;dispatcher.setRenderShadow(false);dispatcher.setRenderHitboxes(false);
            dispatcher.render(p,0,0,0,0,settings.smoothing.get()?c.hudPartialTick:1);
        }finally{
            p.bodyYaw=by;p.lastBodyYaw=lby;p.headYaw=head;p.lastHeadYaw=lastHead;p.yaw=yaw;p.lastYaw=lastYaw;
            dispatcher.cameraYaw=cameraYaw;dispatcher.cameraPitch=cameraPitch;dispatcher.setRenderShadow(shadow);dispatcher.setRenderHitboxes(hitboxes);MiniPlayerRenderScope.restore(previous);
            if(projection){GlStateManager.matrixMode(GL11.GL_PROJECTION);GlStateManager.popMatrix();}GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GLX.bindFramebuffer(GLX.GL_FRAMEBUFFER,oldFbo);GLX.bindRenderbuffer(GLX.GL_RENDERBUFFER,oldRbo);GlStateManager.viewport(viewport.get(0),viewport.get(1),viewport.get(2),viewport.get(3));GlStateManager.clearColor(clear.get(0),clear.get(1),clear.get(2),clear.get(3));GlStateManager.clearDepth(oldDepthClear);
            GlStateManager.activeTexture(GLX.GL_TEXTURE1);GlStateManager.bindTexture(lightTex);if(lightEnabled)GlStateManager.enableTexture();else GlStateManager.disableTexture();GLX.multiTexCoord2f(GLX.GL_TEXTURE1,light.get(0),light.get(1));GlStateManager.activeTexture(GLX.GL_TEXTURE0);
            guard.end();lightingState.restore();GlStateManager.activeTexture(oldActive);
        }
    }
    private static float wrap(float f){f%=360;return f>180?f-360:f< -180?f+360:f;}
    /** Development diagnostic: read only the private cached target, never the world framebuffer. */
    public int visiblePixels(){
        if(target==null)return 0;int previous=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        java.nio.ByteBuffer bytes=org.lwjgl.BufferUtils.createByteBuffer(target.width*target.height*4);
        try{GlStateManager.bindTexture(target.colorTextureId);GL11.glGetTexImage(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,bytes);int count=0;for(int i=3;i<bytes.capacity();i+=4)if((bytes.get(i)&255)>32)count++;return count;}finally{GlStateManager.bindTexture(previous);}
    }
    public void close(){if(target!=null){target.destroyBuffers();target=null;}texture=null;world=player=null;state.invalidate();}
}
