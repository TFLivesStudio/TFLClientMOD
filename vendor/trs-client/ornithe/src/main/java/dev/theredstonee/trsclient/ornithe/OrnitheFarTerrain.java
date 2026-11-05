package dev.theredstonee.trsclient.ornithe;

import dev.diorama.bake.BlockLooks;
import dev.diorama.data.ChunkSnapshot;
import dev.theredstonee.trsclient.core.terrain.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.render.platform.GlStateManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Client-only 3D LOD renderer, submitted after sky and before the first vanilla world geometry. */
public final class OrnitheFarTerrain implements AutoCloseable {
    private final OrnitheClient c;
    private final MapGlState state=new MapGlState();
    private final FloatBuffer projection=BufferUtils.createFloatBuffer(16),model=BufferUtils.createFloatBuffer(16),fog=BufferUtils.createFloatBuffer(16);
    private FloatBuffer staging=BufferUtils.createFloatBuffer(1024);
    private final float[] p=new float[16],v=new float[16];
    private final LinkedHashMap<Long,GpuMesh> gpu=new LinkedHashMap<>(64,.75f,true);
    private final Map<Long,String> pending=new HashMap<>();
    private FarTerrainEngine engine;
    private String identity="";
    private Object world;
    private java.lang.ref.WeakReference<Object> localServer=new java.lang.ref.WeakReference<>(null);
    private String saveId="";
    private int program,originUniform,nearUniform,farUniform,fogUniform,sunUniform;
    private long gpuBytes;
    public long uploads,draws,verticesDrawn,uploadNanos,drawNanos;
    public int maxPassesPerFrame;
    private int framePasses;
    public boolean projectionAligned;
    public void compareNativeProjection(){
        if(!Boolean.getBoolean("tflcore.audit5.smoke")||projectionAligned||draws<8)return;
        projection.clear();model.clear();GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX,projection);GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX,model);
        boolean same=true;
        for(int i=0;i<16;i++){if(i!=10 && i!=14)same&=Math.abs(projection.get(i)-p[i])<.00001f;same&=Math.abs(model.get(i)-v[i])<.00001f;}
        projectionAligned=same;
    }
    public void beginFrame(){framePasses=0;}
    public String failure="";
    private record GpuMesh(int buffer,int count,int bytes,int step,long revision,int minY,int maxY){}
    public OrnitheFarTerrain(OrnitheClient c){this.c=c;}
    public FarTerrainEngine engine(){return engine;}
    public long gpuBytes(){return gpuBytes;}
    private void ensure() {
        if(!c.modules.farTerrain.isEnabled() || c.mc.world==null || c.mc.player==null){close();return;}
        String key=worldIdentity();
        if(engine!=null && world==c.mc.world && key.equals(identity))return;
        close();if(c.diorama==null || c.diorama.lodPalette()==null)return;
        world=c.mc.world;identity=key;int[] palette=c.diorama.lodPalette().clone();
        engine=new FarTerrainEngine(c.mc.gameDir.toPath().resolve("tfl-far-terrain"),key,(long)c.modules.farTerrain.diskBudget.getInt()*1024*1024,raw->raw.recolored(palette));
    }
    private String worldIdentity(){
        var server=c.mc.getServer();
        if(server!=localServer.get()){localServer=new java.lang.ref.WeakReference<>(server);saveId="";
            if(server!=null)try {
                var saves=c.mc.gameDir.toPath().resolve("saves").toAbsolutePath().normalize();var save=saves.resolve(server.getWorldSaveName()).normalize();
                if(!save.startsWith(saves))throw new java.io.IOException("Invalid save folder");
                saveId=TerrainStore.localSaveIdentity(save);
            }catch(java.io.IOException e){saveId="unsaved-"+UUID.randomUUID();OrnitheClient.LOG.warn("Far Terrain local identity unavailable; using an isolated temporary namespace");}
        }
        return c.mapPlatform.worldKey()+"/"+saveId+"/"+c.mapPlatform.dimension();
    }
    public void tick() {
        ensure();if(engine==null)return;
        engine.diskBudget((long)c.modules.farTerrain.diskBudget.getInt()*1024*1024);
        engine.tick((int)Math.floor(c.mc.player.x)>>4,(int)Math.floor(c.mc.player.z)>>4,c.modules.farTerrain.distance.getInt());
    }
    /** Both input objects are immutable for the lifetime of this job. Minecraft is never read on workers. */
    public void received(ChunkSnapshot snapshot,BlockLooks looks) {
        ensure();if(engine==null)return;
        boolean sky=!c.mapPlatform.dimension().equals("dim-1");
        engine.capture(snapshot.chunkX,snapshot.chunkZ,()->columns(snapshot,looks,sky));
    }
    private static TerrainColumns columns(ChunkSnapshot s,BlockLooks looks,boolean hasSky) {
        short[] height=new short[256];int[] colors=new int[256],states=new int[256],tints=new int[256];byte[] light=new byte[256];
        Arrays.fill(states,-1);Arrays.fill(tints,-1);
        for(int z=0;z<16;z++)for(int x=0;x<16;x++){
            int col=z*16+x;
            for(int y=Math.min(255,s.maxY+16);y>=0;y--){
                int[] section=s.section(y>>4);if(section==null)continue;
                int id=section[(y&15)<<8|z<<4|x];if(id<0||id>=looks.kind.length || looks.kind[id]==BlockLooks.AIR || looks.kind[id]==BlockLooks.PLANT)continue;
                height[col]=(short)(y+1);int rgb=looks.top[id];states[col]=id;
                int block=id>>4;
                if(block==2 || block==18 || block==161 || block==106){int tint=(block==2?s.tints.grass():s.tints.foliage())[col];tints[col]=tint;rgb=TerrainColumns.multiply(rgb,tint);}
                int above=y+1,sl=hasSky?(above>=256?15:s.skyLight(above>>4,x,above&15,z)):0;
                int bl=Math.max(s.blockLight(y>>4,x,y&15,z),Math.max(above>=256?0:s.blockLight(above>>4,x,above&15,z),looks.emission[id]));
                colors[col]=rgb;light[col]=(byte)(sl<<4|bl);break;
            }
        }
        return new TerrainColumns(s.chunkX,s.chunkZ,height,colors,light,states,tints);
    }
    public void fog(int mode) {
        if(mode!=0 || !c.modules.farTerrain.isEnabled() || c.mc.world==null || GL11.glGetInteger(GL11.GL_FOG_MODE)!=GL11.GL_LINEAR || GL11.glGetFloat(GL11.GL_FOG_END)<16)return;
        float far=c.modules.farTerrain.distance.getInt()*16f;
        GL11.glFogf(GL11.GL_FOG_START,far*.65f);GL11.glFogf(GL11.GL_FOG_END,far*.98f);
    }
    public void draw(float delta) {
        if(engine==null || !c.modules.farTerrain.isEnabled() || c.mc.world!=world || c.mc.player==null || !failure.isEmpty())return;
        if(!identity.equals(worldIdentity()))return;
        if(GL11.glGetInteger(GL11.GL_FOG_MODE)!=GL11.GL_LINEAR || GL11.glGetFloat(GL11.GL_FOG_END)<16)return;
        long start=System.nanoTime();
        var camera=c.mc.getCamera();if(camera==null)return;
        double cx=camera.prevX+(camera.x-camera.prevX)*delta,cy=camera.prevY+(camera.y-camera.prevY)*delta,cz=camera.prevZ+(camera.z-camera.prevZ)*delta;
        float near=Math.max(16,(c.mc.options.viewDistance-1)*16),far=c.modules.farTerrain.distance.getInt()*16f;
        if(near>=far)return;
        maxPassesPerFrame=Math.max(maxPassesPerFrame,++framePasses);
        projection.clear();model.clear();fog.clear();
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX,projection);projection.get(p).rewind();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX,model);model.get(v).rewind();GL11.glGetFloat(GL11.GL_FOG_COLOR,fog);
        float clipNear=p[14]/(p[10]-1);if(!(clipNear>0))return;
        float clipFar=far*1.8f;p[10]=-(clipFar+clipNear)/(clipFar-clipNear);p[14]=-2*clipFar*clipNear/(clipFar-clipNear);
        projection.clear();projection.put(p).flip();
        try(var scope=state.begin()) {
            upload();
            if(program==0)program();
            GL20.glUseProgram(program);
            GL20.glUniform1f(nearUniform,near);GL20.glUniform1f(farUniform,far);
            GL20.glUniform1f(sunUniform,Math.max(0,Math.min(1,c.mc.world.calculateAmbientLight(delta))));
            GL20.glUniform3f(fogUniform,fog.get(0),fog.get(1),fog.get(2));
            GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glLoadMatrix(projection);GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glEnable(GL11.GL_DEPTH_TEST);GlStateManager.enableDepthTest();GL11.glDepthMask(true);GlStateManager.depthMask(true);
            GL11.glDisable(GL11.GL_BLEND);GlStateManager.disableBlend();
            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
            GL13.glClientActiveTexture(GL13.GL_TEXTURE0);GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            var regions=new ArrayList<>(engine.regions().entrySet());
            regions.sort(Comparator.comparingDouble(e->distance(e.getKey(),cx,cz)));
            for(var entry:regions){
                long key=entry.getKey();int rx=(int)(key>>32),rz=(int)key;
                double dx=rx*64+32-cx,dz=rz*64+32-cz,d=Math.max(Math.abs(dx),Math.abs(dz));
                if(d+32<near || d-32>far)continue;
                int step=d<256?1:d<512?2:d<1024?4:8;
                GpuMesh m=gpu.get(key);String request=step+":"+entry.getValue();
                if((m==null||m.step!=step||m.revision!=entry.getValue())&&!request.equals(pending.get(key))){if(engine.request(rx,rz,step))pending.put(key,request);}
                if(m==null||m.count==0 || !visible(rx*64-cx,m.minY-cy,rz*64-cz,rx*64+64-cx,m.maxY-cy,rz*64+64-cz))continue;
                GL11.glLoadMatrix(model);GL11.glTranslated(rx*64-cx,-cy,rz*64-cz);
                GL20.glUniform2f(originUniform,(float)(rx*64-cx),(float)(rz*64-cz));
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,m.buffer);
                GL11.glVertexPointer(3,GL11.GL_FLOAT,32,0L);GL11.glColorPointer(3,GL11.GL_FLOAT,32,12L);GL11.glTexCoordPointer(2,GL11.GL_FLOAT,32,24L);
                GL11.glDrawArrays(GL11.GL_QUADS,0,m.count);draws++;verticesDrawn+=m.count;
            }
            // LOD self-occlusion used an extended projection. Vanilla starts with a clean depth buffer,
            // so ordinary terrain/entities/clouds always occlude LOD without mismatched depth formulas.
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            trim();
        } catch(RuntimeException e) {failure=e.getClass().getSimpleName();OrnitheClient.LOG.error("Far Terrain renderer stopped safely",e);}
        drawNanos+=System.nanoTime()-start;
    }
    private static double distance(long key,double x,double z){double dx=(int)(key>>32)*64+32-x,dz=(int)key*64+32-z;return dx*dx+dz*dz;}
    private boolean visible(double x0,double y0,double z0,double x1,double y1,double z1) {
        int outside=63;
        for(int i=0;i<8;i++){
            double x=(i&1)==0?x0:x1,y=(i&2)==0?y0:y1,z=(i&4)==0?z0:z1;
            double vx=v[0]*x+v[4]*y+v[8]*z+v[12],vy=v[1]*x+v[5]*y+v[9]*z+v[13],vz=v[2]*x+v[6]*y+v[10]*z+v[14],vw=v[3]*x+v[7]*y+v[11]*z+v[15];
            double a=p[0]*vx+p[4]*vy+p[8]*vz+p[12]*vw,b=p[1]*vx+p[5]*vy+p[9]*vz+p[13]*vw,d=p[2]*vx+p[6]*vy+p[10]*vz+p[14]*vw,w=p[3]*vx+p[7]*vy+p[11]*vz+p[15]*vw;
            int code=(a<-w?1:0)|(a>w?2:0)|(b<-w?4:0)|(b>w?8:0)|(d<-w?16:0)|(d>w?32:0);outside&=code;
        }return outside==0;
    }
    private void upload() {
        long start=System.nanoTime();int bytes=0;LodMesh mesh;
        while(bytes<2*1024*1024 && (mesh=engine.pollMesh())!=null){
            int size=mesh.vertices.length*4;if(size>c.modules.farTerrain.gpuBudget.getInt()*1024*1024)continue;
            long key=TerrainColumns.key(mesh.regionX,mesh.regionZ);GpuMesh old=gpu.remove(key);if(old!=null)release(old);
            int buffer=0;
            if(size>0){if(staging.capacity()<mesh.vertices.length)staging=BufferUtils.createFloatBuffer(mesh.vertices.length);staging.clear();staging.put(mesh.vertices).flip();buffer=GL15.glGenBuffers();GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,buffer);GL15.glBufferData(GL15.GL_ARRAY_BUFFER,staging,GL15.GL_STATIC_DRAW);}
            gpu.put(key,new GpuMesh(buffer,mesh.vertices.length/8,size,mesh.step,mesh.revision,mesh.minY,mesh.maxY));gpuBytes+=size;bytes+=size;uploads++;pending.remove(key);
        }uploadNanos+=System.nanoTime()-start;
    }
    private void trim() {
        var it=gpu.entrySet().iterator();while(it.hasNext()){var e=it.next();if(!engine.regions().containsKey(e.getKey())){release(e.getValue());pending.remove(e.getKey());it.remove();}}
        long limit=(long)c.modules.farTerrain.gpuBudget.getInt()*1024*1024;
        it=gpu.entrySet().iterator();while(gpuBytes>limit&&it.hasNext()){var e=it.next();release(e.getValue());it.remove();}
    }
    private void release(GpuMesh mesh){if(mesh.buffer!=0)GL15.glDeleteBuffers(mesh.buffer);gpuBytes-=mesh.bytes;}
    private void program() {
        int vertex=0,fragment=0,linked=0;
        try {
            vertex=shader(GL20.GL_VERTEX_SHADER,"/assets/tflcore/shaders/far-terrain.vert");fragment=shader(GL20.GL_FRAGMENT_SHADER,"/assets/tflcore/shaders/far-terrain.frag");
            linked=GL20.glCreateProgram();GL20.glAttachShader(linked,vertex);GL20.glAttachShader(linked,fragment);GL20.glLinkProgram(linked);
            if(GL20.glGetProgrami(linked,GL20.GL_LINK_STATUS)==0)throw new IllegalStateException(GL20.glGetProgramInfoLog(linked,4096));
            program=linked;linked=0;originUniform=GL20.glGetUniformLocation(program,"origin");nearUniform=GL20.glGetUniformLocation(program,"nearEdge");farUniform=GL20.glGetUniformLocation(program,"farEdge");fogUniform=GL20.glGetUniformLocation(program,"fogColor");sunUniform=GL20.glGetUniformLocation(program,"sunBrightness");
        } finally {if(vertex!=0)GL20.glDeleteShader(vertex);if(fragment!=0)GL20.glDeleteShader(fragment);if(linked!=0)GL20.glDeleteProgram(linked);}
    }
    private int shader(int type,String path) {
        int shader=GL20.glCreateShader(type);
        try(var input=OrnitheFarTerrain.class.getResourceAsStream(path)) {
            if(input==null)throw new IllegalStateException("Missing Far Terrain shader");
            GL20.glShaderSource(shader,new String(input.readAllBytes(),StandardCharsets.UTF_8));GL20.glCompileShader(shader);
            if(GL20.glGetShaderi(shader,GL20.GL_COMPILE_STATUS)==0)throw new IllegalStateException(GL20.glGetShaderInfoLog(shader,4096));return shader;
        } catch(java.io.IOException|RuntimeException e){GL20.glDeleteShader(shader);throw new IllegalStateException(e);}
    }
    public void close() {
        if(engine!=null)engine.close();engine=null;world=null;identity="";
        for(var mesh:gpu.values())release(mesh);gpu.clear();pending.clear();gpuBytes=0;
        if(program!=0)GL20.glDeleteProgram(program);program=0;
        if(staging.capacity()>1024)staging=BufferUtils.createFloatBuffer(1024);
    }
    public void reload(){close();failure="";}
    public void shutdown(){FarTerrainEngine old=engine;close();if(old!=null)try{if(!old.awaitStopped(2000))OrnitheClient.LOG.warn("Far Terrain cache shutdown exceeded its bounded wait");}catch(InterruptedException e){Thread.currentThread().interrupt();}}
}
