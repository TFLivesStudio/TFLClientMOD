package dev.theredstonee.trsclient.core.map;

import dev.theredstonee.trsclient.core.map.volume.*;
import dev.theredstonee.trsclient.core.ui.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/** One map-local bounded software volume backend. Graphics upload/composition uses the loader's native Store.
 * The worker sees immutable received-map snapshots only, never a World/Chunk/MapLayer reference. */
public final class VolumeMapRenderer implements AutoCloseable {
    private static final class VolumeHolder {static final CloudVolume VOLUME=new CloudVolume();}
    public static CloudVolume sharedVolume(){return VolumeHolder.VOLUME;}
    private static final class Request {
        final MapProjection camera;final TerrainSnapshot terrain;final CloudQuality quality;final double clock,density,height,distance;
        Request(MapProjection c,TerrainSnapshot t,CloudQuality q,double clock,double d,double h,double range){camera=c;terrain=t;quality=q;this.clock=clock;density=d;height=h;distance=range;}
    }
    private static final class Frame {
        final Request request;final int w,h;final int[] pixels;final long nanos;
        Frame(Request r,int w,int h,int[] pixels,long nanos){request=r;this.w=w;this.h=h;this.pixels=pixels;this.nanos=nanos;}
    }
    private final AtomicReference<Request> request=new AtomicReference<Request>();
    private final AtomicReference<Frame> pending=new AtomicReference<Frame>();
    private final ArrayDeque<int[]> pool=new ArrayDeque<int[]>();
    private volatile long epoch;
    private volatile boolean running;private Thread worker;private volatile Throwable failure;
    private TextureRef texture;private Textures.Store owner;private int generation=-1;
    private MapProjection displayed;private long requestedAt,lastDraw;private volatile long renders,renderNanos;private long uploads,uploadNanos;
    private Request lastRequest;
    private TerrainSnapshot cachedTerrain;private MapProjection cachedCamera;private long terrainAt;private MapLayer layerOf;
    private final LinkedHashMap<Long,Long> seen=new LinkedHashMap<Long,Long>();
    public MapProjection displayed(){return displayed;}
    public String backend(){return "bounded CPU 3D ray marcher / native texture composition";}
    public long renderedFrames(){return renders;}public long renderNanos(){return renderNanos;}public long uploadedFrames(){return uploads;}public long uploadNanos(){return uploadNanos;}
    public String failure(){return failure==null?"":failure.toString();}
    public boolean draw(Canvas c,MapEngine engine,MapLayer layer,MapProjection camera,CloudQuality quality,boolean motion,double density,double height,double range){
        if(quality==CloudQuality.OFF||!c.images()||Textures.store()==null){close();return false;}
        long now=System.nanoTime();lastDraw=now;
        if(layerOf!=layer){close();layerOf=layer;}
        if(!running&&failure==null)start();
        Frame next=pending.getAndSet(null);
        Textures.Store store=Textures.store();int gen=store.resourceGeneration();
        if(store!=owner||gen!=generation){if(texture!=null&&owner!=null)owner.release(texture);texture=null;owner=store;generation=gen;displayed=null;}
        if(next!=null){
            long t=System.nanoTime();texture=store.upload("map/volume",next.w,next.h,next.pixels);uploadNanos+=System.nanoTime()-t;uploads++;
            if(texture!=null)displayed=next.request.camera;
            recycle(next.pixels);
        }
        if(now-requestedAt>=1_000_000_000L/quality.fps&&failure==null){
            if(cachedTerrain==null||!camera.sameView(cachedCamera)||now-terrainAt>300_000_000L){
                cachedTerrain=capture(layer,camera,quality,height,System.currentTimeMillis());cachedCamera=camera;terrainAt=now;
            }
            boolean changed=motion||texture==null||lastRequest==null||!camera.sameView(lastRequest.camera)||lastRequest.quality!=quality||lastRequest.terrain.fingerprint!=cachedTerrain.fingerprint||lastRequest.density!=density||lastRequest.height!=height||lastRequest.distance!=range;
            if(changed){lastRequest=new Request(camera,cachedTerrain,quality,motion?now/1e9:0,density,height,range);request.set(lastRequest);LockSupport.unpark(worker);}
            requestedAt=now;
        }
        if(texture==null)return false;
        c.push();try{c.scale(camera.width/(float)texture.width,camera.height/(float)texture.height);c.image(texture,0,0,texture.width,texture.height,0xFFFFFFFF);}finally{c.pop();}
        return true;
    }
    private TerrainSnapshot capture(MapLayer layer,MapProjection camera,CloudQuality quality,double cloudHeight,long now){
        double[] p=new double[3];double x0=Double.POSITIVE_INFINITY,z0=x0,x1=Double.NEGATIVE_INFINITY,z1=x1;
        for(int y=0;y<2;y++)for(int x=0;x<2;x++){camera.ground(x*camera.width,y*camera.height,p);x0=Math.min(x0,p[0]);x1=Math.max(x1,p[0]);z0=Math.min(z0,p[1]);z1=Math.max(z1,p[1]);}
        double margin=Math.min(cloudHeight,camera.eyeY*.55)*2;x0-=margin;x1+=margin;z0-=margin;z1+=margin;
        int size=quality==CloudQuality.HIGH?768:quality==CloudQuality.BALANCED?512:384;
        int[] colors=new int[size*size];float[] fog=new float[colors.length];
        int lastRx=Integer.MIN_VALUE,lastRz=Integer.MIN_VALUE;MapRegion region=null;
        for(int z=0;z<size;z++)for(int x=0;x<size;x++){
            int wx=(int)Math.floor(x0+(x+.5)*(x1-x0)/size),wz=(int)Math.floor(z0+(z+.5)*(z1-z0)/size),rx=wx>>MapRegion.SHIFT,rz=wz>>MapRegion.SHIFT;
            if(rx!=lastRx||rz!=lastRz){region=layer.known(MapRegion.key(rx,rz))?layer.get(rx,rz,now):layer.peek(rx,rz);lastRx=rx;lastRz=rz;}
            int at=(wz&(MapRegion.SIZE-1))*MapRegion.SIZE+(wx&(MapRegion.SIZE-1));int raw=region==null?0:region.pixels[at];boolean known=(raw&MapColors.KNOWN)!=0;
            int i=z*size+x;
            if(known){int h=region.heights[at];colors[i]=MapColors.compose(raw,h,h,h,layer.caveRef);}else colors[i]=0xFF253340;
            long key=MapRegion.key(wx>>4,wz>>4);Long stamp=seen.get(key);
            if(!known){seen.put(key,Long.MIN_VALUE);fog[i]=1;}
            else if(stamp!=null){if(stamp==Long.MIN_VALUE){stamp=now;seen.put(key,stamp);}fog[i]=(float)Math.max(0,1-(now-stamp)/900.0);}
            if(seen.size()>16384){Iterator<Long> it=seen.keySet().iterator();it.next();it.remove();}
        }
        return new TerrainSnapshot(x0,z0,x1,z1,size,colors,fog);
    }
    private void start(){
        final long token=++epoch;running=true;failure=null;worker=new Thread(()->{
            VolumeRaymarcher renderer=new VolumeRaymarcher(sharedVolume());int[] clouds=null;
            try{while(running&&token==epoch){
                Request r=request.getAndSet(null);if(r==null){LockSupport.parkNanos(20_000_000L);continue;}
                int cw=r.quality.width,ch=Math.max(16,(int)Math.round(cw*r.camera.height/(double)r.camera.width));
                int w=Math.min(1280,Math.max(r.camera.width,cw*3)),h=Math.max(16,(int)Math.round(w*r.camera.height/(double)r.camera.width));
                if(clouds==null||clouds.length!=cw*ch)clouds=new int[cw*ch];
                int[] pixels=acquire(w*h);long t=System.nanoTime();
                renderer.renderComposite(r.camera,r.terrain,r.quality,cw,ch,clouds,w,h,pixels,r.clock,r.density,r.height,r.distance);
                long elapsed=System.nanoTime()-t;renderNanos+=elapsed;renders++;
                publish(new Frame(r,w,h,pixels,elapsed),token);
            }}catch(Throwable e){if(token==epoch)failure=e;}finally{if(token==epoch)running=false;}
        },"TFL-cloud-volume");worker.setDaemon(true);worker.setPriority(Thread.MIN_PRIORITY);worker.start();
    }
    private synchronized void publish(Frame frame,long token){
        if(!running||token!=epoch){recycle(frame.pixels);return;}
        Frame old=pending.getAndSet(frame);if(old!=null)recycle(old.pixels);
    }
    private synchronized int[] acquire(int count){while(!pool.isEmpty()){int[] a=pool.removeFirst();if(a.length==count)return a;}return new int[count];}
    private synchronized void recycle(int[] pixels){if(pool.size()<3)pool.addLast(pixels);}
    public synchronized void close(){epoch++;running=false;failure=null;request.set(null);if(worker!=null)worker.interrupt();pending.set(null);if(texture!=null&&owner!=null)owner.release(texture);texture=null;displayed=null;cachedTerrain=null;cachedCamera=null;lastRequest=null;seen.clear();synchronized(this){pool.clear();}}
}
