package dev.diorama.data;
/** Immutable received-client data. Never reads Minecraft on the bake thread. */
public final class ChunkSnapshot {
 public final int chunkX,chunkZ,minSectionY=0,maxY,floorY;
 private final int[][] states;private final byte[][] block,sky;
 public final Tints tints;
 public ChunkSnapshot(int x,int z,int[][] states,byte[][] block,byte[][] sky,int top,int floor,Tints tints){this.chunkX=x;this.chunkZ=z;this.states=states;this.block=block;this.sky=sky;maxY=top;floorY=floor;this.tints=tints;}
 public static long key(int x,int z){return (long)x<<32 | z&0xffffffffL;}
 public int sectionCount(){return states.length;}
 public int[] section(int i){return states[i];}
 public int blockLight(int i,int x,int y,int z){return i<0||i>=block.length||block[i]==null?0:block[i][y<<8|z<<4|x]&15;}
 public int skyLight(int i,int x,int y,int z){return i<0||i>=sky.length||sky[i]==null?15:sky[i][y<<8|z<<4|x]&15;}
 public record Tints(int[] grass,int[] foliage,int[] dryFoliage,int[] water){}
}
