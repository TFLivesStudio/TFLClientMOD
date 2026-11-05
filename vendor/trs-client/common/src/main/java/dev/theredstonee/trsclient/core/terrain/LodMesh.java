package dev.theredstonee.trsclient.core.terrain;

import java.util.Map;

/** Real 3D top faces and cliff walls, in a 64-block region's local coordinates. */
public final class LodMesh {
    public final int regionX,regionZ,step;
    public final long revision;
    public final float[] vertices; // xyz + rgb + sky/block light, four vertices per quad
    public final int minY,maxY;
    private LodMesh(int x,int z,int step,long revision,float[] vertices,int min,int max){regionX=x;regionZ=z;this.step=step;this.revision=revision;this.vertices=vertices;minY=min;maxY=max;}
    public static LodMesh build(int rx,int rz,int step,long revision,Map<Long,TerrainColumns> chunks) throws InterruptedException {
        if(step!=1&&step!=2&&step!=4&&step!=8)throw new IllegalArgumentException("LOD step");
        int count=64/step;float[] heights=new float[(count+2)*(count+2)],sky=new float[heights.length],block=new float[heights.length];int[] colors=new int[heights.length];
        java.util.Arrays.fill(heights,-1);int min=256,max=0;
        for(int z=-1;z<=count;z++)for(int x=-1;x<=count;x++){
            if(Thread.currentThread().isInterrupted())throw new InterruptedException();
            int sum=0,r=0,g=0,b=0,n=0,sl=0,bl=0;boolean known=true;
            for(int dz=0;dz<step && known;dz++)for(int dx=0;dx<step;dx++){
                int wx=rx*64+x*step+dx,wz=rz*64+z*step+dz;
                TerrainColumns c=chunks.get(TerrainColumns.key(wx>>4,wz>>4));if(c==null){known=false;break;}
                int col=(wz&15)*16+(wx&15),height=c.height(col);if(height==0){known=false;break;}
                int color=c.color(col);
                sum+=height;r+=(color>>16)&255;g+=(color>>8)&255;b+=color&255;sl+=c.skyLight(col);bl+=c.blockLight(col);n++;
            }
            if(known&&n>0){int at=(z+1)*(count+2)+x+1;heights[at]=(float)sum/n;colors[at]=(r/n)<<16|(g/n)<<8|b/n;sky[at]=sl/(n*15f);block[at]=bl/(n*15f);}
        }
        Floats out=new Floats(count*count*32);
        for(int z=0;z<count;z++)for(int x=0;x<count;x++){
            int at=(z+1)*(count+2)+x+1;float y=heights[at];if(y<0)continue;
            min=Math.min(min,(int)y);max=Math.max(max,(int)Math.ceil(y));int color=colors[at];float x0=x*step,x1=x0+step,z0=z*step,z1=z0+step;
            out.sky=sky[at];out.block=block[at];
            quad(out,color,1f,x0,y,z0,x0,y,z1,x1,y,z1,x1,y,z0);
            float north=lower(heights[at-count-2],y,step),south=lower(heights[at+count+2],y,step),west=lower(heights[at-1],y,step),east=lower(heights[at+1],y,step);
            if(north<y)quad(out,color,.8f,x1,north,z0,x0,north,z0,x0,y,z0,x1,y,z0);
            if(south<y)quad(out,color,.8f,x0,south,z1,x1,south,z1,x1,y,z1,x0,y,z1);
            if(west<y)quad(out,color,.65f,x0,west,z0,x0,west,z1,x0,y,z1,x0,y,z0);
            if(east<y)quad(out,color,.65f,x1,east,z1,x1,east,z0,x1,y,z0,x1,y,z1);
            min=Math.min(min,(int)Math.min(Math.min(north,south),Math.min(west,east)));
        }
        return new LodMesh(rx,rz,step,revision,out.array(),min,max);
    }
    private static float lower(float neighbor,float y,int step){return neighbor<0?Math.max(0,y-step*4):Math.min(y,neighbor);}
    private static void quad(Floats out,int color,float shade,float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz,float dx,float dy,float dz){
        float r=((color>>16)&255)/255f*shade,g=((color>>8)&255)/255f*shade,b=(color&255)/255f*shade;
        vertex(out,ax,ay,az,r,g,b);vertex(out,bx,by,bz,r,g,b);vertex(out,cx,cy,cz,r,g,b);vertex(out,dx,dy,dz,r,g,b);
    }
    private static void vertex(Floats out,float x,float y,float z,float r,float g,float b){out.add(x);out.add(y);out.add(z);out.add(r);out.add(g);out.add(b);out.add(out.sky);out.add(out.block);}
    private static final class Floats {float[] values;int size;float sky,block;Floats(int capacity){values=new float[Math.max(32,capacity)];}void add(float v){if(size==values.length)values=java.util.Arrays.copyOf(values,size+size/2);values[size++]=v;}float[] array(){return java.util.Arrays.copyOf(values,size);}}
}
