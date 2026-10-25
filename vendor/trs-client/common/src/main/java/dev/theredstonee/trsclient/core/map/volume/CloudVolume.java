package dev.theredstonee.trsclient.core.map.volume;

/** Periodic 3D density and directional lighting, built once. No noise generation in the frame loop. */
public final class CloudVolume {
    private static final int N=64,MASK=N-1;
    private final float[] density=new float[N*N*N],light=new float[N*N*N];
    public CloudVolume(){
        for(int y=0;y<N;y++)for(int z=0;z<N;z++)for(int x=0;x<N;x++){
            double v=0,weight=.57;int frequency=4;
            for(int o=0;o<4;o++){v+=value(x/(double)N*frequency,y/(double)N*frequency,z/(double)N*frequency,frequency)*weight;weight*=.5;frequency*=2;}
            double envelope=Math.sin(Math.PI*(y+.5)/N);
            density[index(x,y,z)]=(float)(Math.max(0,v-.48)*8*envelope*envelope);
        }
        for(int y=0;y<N;y++)for(int z=0;z<N;z++)for(int x=0;x<N;x++){
            float extinction=0;
            for(int i=1;i<=6;i++)if(y+i*2<N)extinction+=density[index(x+i,y+i*2,z-i)];
            light[index(x,y,z)]=(float)(.38+.62*Math.exp(-extinction*.48));
        }
    }
    private static int index(int x,int y,int z){return ((y&MASK)*N+(z&MASK))*N+(x&MASK);}
    private static double hash(int x,int y,int z){int h=x*374761393+y*668265263+z*1442695041;h=(h^(h>>>13))*1274126177;return ((h^(h>>>16))&65535)/65535.;}
    private static double smooth(double a){return a*a*(3-2*a);}
    private static double value(double x,double y,double z,int period){
        int ix=(int)Math.floor(x),iy=(int)Math.floor(y),iz=(int)Math.floor(z);double u=smooth(x-ix),v=smooth(y-iy),w=smooth(z-iz),sum=0;
        for(int dy=0;dy<2;dy++)for(int dz=0;dz<2;dz++)for(int dx=0;dx<2;dx++)sum+=hash(Math.floorMod(ix+dx,period),Math.floorMod(iy+dy,period),Math.floorMod(iz+dz,period))*(dx==0?1-u:u)*(dy==0?1-v:v)*(dz==0?1-w:w);
        return sum;
    }
    public float density(double x,double normalizedY,double z){return sample(density,x,normalizedY,z);}
    public float light(double x,double normalizedY,double z){return sample(light,x,normalizedY,z);}
    private static float sample(float[] a,double x,double y,double z){
        if(y<=0||y>=1)return 0;
        double xx=x*N,yy=y*(N-1),zz=z*N;int ix=(int)Math.floor(xx),iy=(int)yy,iz=(int)Math.floor(zz);
        float u=(float)(xx-ix),v=(float)(yy-iy),w=(float)(zz-iz),sum=0;
        for(int dy=0;dy<2;dy++)for(int dz=0;dz<2;dz++)for(int dx=0;dx<2;dx++)sum+=a[index(ix+dx,iy+dy,iz+dz)]*(dx==0?1-u:u)*(dy==0?1-v:v)*(dz==0?1-w:w);
        return sum;
    }
}
