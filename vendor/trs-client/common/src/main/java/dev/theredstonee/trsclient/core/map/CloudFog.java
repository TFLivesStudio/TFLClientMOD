package dev.theredstonee.trsclient.core.map;

import dev.theredstonee.trsclient.core.ui.*;

/** Minimap-only simplified top-down integration of the shared 3D density/light volume.
 * Built once on a worker, cached across frames. Known map pixels mask this underlay. */
public final class CloudFog {
    private static final int SIZE=256;
    private static TextureRef texture;
    private static Textures.Store owner;
    private static int generation=-1;
    private CloudFog() {}
    public static final String MODE="Simplified 3D top integral";
    private static final java.util.concurrent.FutureTask<int[]> PREPARED=new java.util.concurrent.FutureTask<int[]>(CloudFog::pixels);
    private static boolean started;
    static int[] pixels(){
        dev.theredstonee.trsclient.core.map.volume.CloudVolume volume=VolumeMapRenderer.sharedVolume();
        int[] result=new int[SIZE*SIZE];
        for(int z=0;z<SIZE;z++)for(int x=0;x<SIZE;x++){
            double trans=1,r=0,g=0,b=0;
            for(int j=15;j>=0;j--){
                double y=(j+.5)/16.,d=volume.density(x/(double)SIZE,y,z/(double)SIZE);
                double alpha=1-Math.exp(-d*.45),light=volume.light(x/(double)SIZE,y,z/(double)SIZE),weight=trans*alpha;
                r+=weight*(142+107*light);g+=weight*(160+91*light);b+=weight*(180+73*light);trans*=1-alpha;
            }
            result[z*SIZE+x]=0xFF000000|((int)(r+trans*37)<<16)|((int)(g+trans*51)<<8)|(int)(b+trans*64);
        }
        return result;
    }
    private static synchronized void prepare(){
        if(!started){started=true;Thread t=new Thread(PREPARED,"TFL-minimap-cloud-integral");t.setDaemon(true);t.setPriority(Thread.MIN_PRIORITY);t.start();}
    }
    public static void draw(Canvas c,float x,float y,float w,float h,float u0,float v0,float u1,float v1){
        Textures.Store store=Textures.store();
        if(!c.images()||store==null||w<=0||h<=0)return;
        prepare();if(!PREPARED.isDone())return;
        int[] prepared;try{prepared=PREPARED.get();}catch(Exception failure){return;}
        int gen=store.resourceGeneration();
        if(store!=owner||gen!=generation||texture==null){
            if(texture!=null&&owner!=null)owner.release(texture);
            owner=store;generation=gen;texture=store.upload("map/cloud-fog",SIZE,SIZE,prepared);
        }
        if(texture==null)return;
        int sw=Math.max(1,Math.round((u1-u0)*SIZE)),sh=Math.max(1,Math.round((v1-v0)*SIZE));
        c.push();try{c.translate(x,y);c.scale(w/sw,h/sh);c.image(texture,u0*SIZE,v0*SIZE,sw,sh,0xFFFFFFFF);}finally{c.pop();}
    }
}
