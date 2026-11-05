package dev.theredstonee.trsclient.core.terrain;

import java.util.Arrays;

/** Immutable surface columns derived exclusively from a received chunk, never a server world. */
public final class TerrainColumns {
    public final int x,z;
    private final short[] heights;
    private final int[] colors;
    private final byte[] lights;
    private final int[] states,tints;
    public TerrainColumns(int x,int z,short[] heights,int[] colors,byte[] lights) {
        this(x,z,heights,colors,lights,unknown(),unknown());
    }
    private static int[] unknown(){int[] out=new int[256];Arrays.fill(out,-1);return out;}
    public TerrainColumns(int x,int z,short[] heights,int[] colors,byte[] lights,int[] states,int[] tints) {
        if(Math.abs((long)x)>2000000 || Math.abs((long)z)>2000000 || heights.length!=256 || colors.length!=256 || lights.length!=256)throw new IllegalArgumentException("Invalid LOD columns");
        if(states.length!=256 || tints.length!=256)throw new IllegalArgumentException("Invalid LOD palette");
        for(int i=0;i<256;i++)if(heights[i]<0 || heights[i]>256 || states[i]<-1 || states[i]>65535)throw new IllegalArgumentException("Invalid height/state");
        this.x=x;this.z=z;this.heights=heights.clone();this.colors=colors.clone();this.lights=lights.clone();this.states=states.clone();this.tints=tints.clone();
    }
    public static long key(int x,int z){return ((long)x<<32)|(z&0xffffffffL);}
    public long key(){return key(x,z);}
    public int height(int column){return heights[column];}
    public int color(int column){return colors[column]&0xffffff;}
    public int light(int column){return Math.max(blockLight(column),skyLight(column));}
    public int blockLight(int column){return lights[column]&15;}
    public int skyLight(int column){return (lights[column]>>4)&15;}
    public int packedLight(int column){return lights[column]&255;}
    public int state(int column){return states[column];}
    public int tint(int column){return tints[column];}
    public TerrainColumns recolored(int[] palette){
        int[] rgb=colors.clone();for(int i=0;i<256;i++)if(states[i]>=0&&states[i]<palette.length){rgb[i]=palette[states[i]];if(tints[i]!=-1)rgb[i]=multiply(rgb[i],tints[i]);}
        return new TerrainColumns(x,z,heights,rgb,lights,states,tints);
    }
    public static int multiply(int a,int b){return ((((a>>16)&255)*((b>>16)&255)/255)<<16)|((((a>>8)&255)*((b>>8)&255)/255)<<8)|((a&255)*(b&255)/255);}
    public boolean same(TerrainColumns c){return c!=null && x==c.x && z==c.z && Arrays.equals(heights,c.heights)&&Arrays.equals(colors,c.colors)&&Arrays.equals(lights,c.lights)&&Arrays.equals(states,c.states)&&Arrays.equals(tints,c.tints);}
}
