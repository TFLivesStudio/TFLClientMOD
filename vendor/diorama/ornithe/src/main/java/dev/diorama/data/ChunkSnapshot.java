package dev.diorama.data;
/** Immutable received-client data. Never reads Minecraft on the bake thread. */
public final class ChunkSnapshot {
 public final int chunkX,chunkZ,minSectionY=0,maxY,floorY;
 private final int[][] states;private final byte[][] block,sky;
 public final Tints tints;
 private volatile long fingerprint;
 public long fingerprint(){long h=fingerprint;if(h!=0)return h;h=(0xcbf29ce484222325L^maxY)*0x100000001b3L;h=(h^floorY)*0x100000001b3L;for(int i=0;i<states.length;i++){int[] a=states[i];h=(h^(a==null?0:i+1))*0x100000001b3L;if(a!=null)for(int v:a)h=(h^v)*0x100000001b3L;}for(byte[][] light:new byte[][][]{block,sky})for(int i=0;i<light.length;i++){byte[] a=light[i];h=(h^(a==null?0:i+1))*0x100000001b3L;if(a!=null)for(byte v:a)h=(h^(v&255))*0x100000001b3L;}for(int[] colors:new int[][]{tints.grass(),tints.foliage(),tints.dryFoliage(),tints.water()})for(int v:colors)h=(h^v)*0x100000001b3L;fingerprint=h==0?1:h;return fingerprint;}
 public ChunkSnapshot rotated(int q){
  if((q&3)==0)return this;
  int[][] state=new int[states.length][];byte[][] bl=new byte[block.length][],sl=new byte[sky.length][];
  for(int i=0;i<states.length;i++)if(states[i]!=null){state[i]=new int[4096];bl[i]=new byte[4096];sl[i]=new byte[4096];for(int y=0;y<16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++){
   int ox=dev.theredstonee.trsclient.core.map.QuarterTurn.blockX(x,z,q)&15,oz=dev.theredstonee.trsclient.core.map.QuarterTurn.blockZ(x,z,q)&15,at=y<<8|z<<4|x;
   state[i][at]=states[i][y<<8|oz<<4|ox];bl[i][at]=(byte)blockLight(i,ox,y,oz);sl[i][at]=(byte)skyLight(i,ox,y,oz);
  }}
  int[][] colors=new int[4][256];int[][] original={tints.grass(),tints.foliage(),tints.dryFoliage(),tints.water()};
  for(int z=0;z<16;z++)for(int x=0;x<16;x++){int ox=dev.theredstonee.trsclient.core.map.QuarterTurn.blockX(x,z,q)&15,oz=dev.theredstonee.trsclient.core.map.QuarterTurn.blockZ(x,z,q)&15;for(int c=0;c<4;c++)colors[c][z*16+x]=original[c][oz*16+ox];}
  return new ChunkSnapshot(dev.theredstonee.trsclient.core.map.QuarterTurn.blockU(chunkX,chunkZ,q),dev.theredstonee.trsclient.core.map.QuarterTurn.blockDepth(chunkX,chunkZ,q),state,bl,sl,maxY,floorY,new Tints(colors[0],colors[1],colors[2],colors[3]));
 }
 public ChunkSnapshot(int x,int z,int[][] states,byte[][] block,byte[][] sky,int top,int floor,Tints tints){this.chunkX=x;this.chunkZ=z;this.states=states;this.block=block;this.sky=sky;maxY=top;floorY=floor;this.tints=tints;}
 public boolean sameContent(ChunkSnapshot other){return other!=null&&chunkX==other.chunkX&&chunkZ==other.chunkZ&&maxY==other.maxY&&floorY==other.floorY&&java.util.Arrays.deepEquals(states,other.states)&&java.util.Arrays.deepEquals(block,other.block)&&java.util.Arrays.deepEquals(sky,other.sky)&&java.util.Arrays.equals(tints.grass(),other.tints.grass())&&java.util.Arrays.equals(tints.water(),other.tints.water());}
 public static long key(int x,int z){return (long)x<<32 | z&0xffffffffL;}
 public int sectionCount(){return states.length;}
 public int[] section(int i){return states[i];}
 public int blockLight(int i,int x,int y,int z){return i<0||i>=block.length||block[i]==null?0:block[i][y<<8|z<<4|x]&15;}
 public int skyLight(int i,int x,int y,int z){return i<0||i>=sky.length||sky[i]==null?15:sky[i][y<<8|z<<4|x]&15;}
 public record Tints(int[] grass,int[] foliage,int[] dryFoliage,int[] water){}
}
