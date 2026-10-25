package dev.theredstonee.trsclient.core.map;

import dev.theredstonee.trsclient.core.ui.*;

/** Static, softly lit cloud underlay. Known opaque map pixels cover it; never reads world data.
 * One 256-square texture per resource generation, no per-frame pixel generation or animation. */
public final class CloudFog {
    private static final int SIZE=256;
    private static TextureRef texture;
    private static Textures.Store owner;
    private static int generation=-1;
    private CloudFog() {}
    static double noise(int x,int y){
        int h=x*374761393+y*668265263;h=(h^(h>>>13))*1274126177;
        return ((h^(h>>>16))&65535)/65535.0;
    }
    static double density(double x,double y){
        double sum=0,weight=.5;
        for(int octave=0;octave<5;octave++){
            int ix=(int)Math.floor(x),iy=(int)Math.floor(y);
            double u=x-ix,v=y-iy;u=u*u*(3-2*u);v=v*v*(3-2*v);
            double a=noise(ix,iy)*(1-u)+noise(ix+1,iy)*u;
            double b=noise(ix,iy+1)*(1-u)+noise(ix+1,iy+1)*u;
            sum+=(a*(1-v)+b*v)*weight;x*=2;y*=2;weight*=.5;
        }
        return sum;
    }
    static int[] pixels(){
        int[] pixels=new int[SIZE*SIZE];
        for(int y=0;y<SIZE;y++)for(int x=0;x<SIZE;x++){
            double d=density(x/64.0,y/64.0);
            double light=d-density((x-2)/64.0,(y-3)/64.0);
            int value=Math.max(28,Math.min(230,(int)(48+155*d+220*light)));
            pixels[y*SIZE+x]=0xFF000000|(value<<16)|(Math.min(255,value+15)<<8)|Math.min(255,value+30);
        }
        return pixels;
    }
    public static void draw(Canvas c,float x,float y,float w,float h,float u0,float v0,float u1,float v1){
        Textures.Store store=Textures.store();
        if(!c.images()||store==null||w<=0||h<=0)return;
        int gen=store.resourceGeneration();
        if(store!=owner||gen!=generation||texture==null){
            if(texture!=null&&owner!=null)owner.release(texture);
            owner=store;generation=gen;texture=store.upload("map/cloud-fog",SIZE,SIZE,pixels());
        }
        if(texture==null)return;
        int sw=Math.max(1,Math.round((u1-u0)*SIZE)),sh=Math.max(1,Math.round((v1-v0)*SIZE));
        c.push();try{c.translate(x,y);c.scale(w/sw,h/sh);c.image(texture,u0*SIZE,v0*SIZE,sw,sh,0xFFFFFFFF);}finally{c.pop();}
    }
}
