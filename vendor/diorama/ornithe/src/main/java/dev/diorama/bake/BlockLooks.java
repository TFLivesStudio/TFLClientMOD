package dev.diorama.bake;
public final class BlockLooks {
 public static final byte AIR=0,SOLID=1,WATER=2,LAVA=3,GLASS=4,PLANT=5;
 public static final byte TINT_NONE=0,TINT_GRASS=1,TINT_FOLIAGE=2,TINT_DRY_FOLIAGE=3,TINT_WATER=4;
 public final byte[] kind=new byte[65536],tint=new byte[65536],emission=new byte[65536];
 public final int[] top=new int[65536],side=new int[65536],topTinted=new int[65536],sideTinted=new int[65536],constantTint=new int[65536],shape=new int[65536],wetShape=new int[65536];
 public final float[] topTintWeight=new float[65536],sideTintWeight=new float[65536];
 public final boolean[] spawnOn=new boolean[65536],spawnIn=new boolean[65536];
 public float[][] shapes={new float[]{0,0,0,1,1,1}};
 public BlockLooks(){java.util.Arrays.fill(constantTint,-1);}
 public BlockLooks rotated(int q){if((q&3)==0)return this;BlockLooks copy=new BlockLooks();
  System.arraycopy(kind,0,copy.kind,0,kind.length);System.arraycopy(tint,0,copy.tint,0,tint.length);System.arraycopy(emission,0,copy.emission,0,emission.length);
  int[][] from={top,side,topTinted,sideTinted,constantTint,shape,wetShape},to={copy.top,copy.side,copy.topTinted,copy.sideTinted,copy.constantTint,copy.shape,copy.wetShape};for(int i=0;i<from.length;i++)System.arraycopy(from[i],0,to[i],0,from[i].length);
  System.arraycopy(topTintWeight,0,copy.topTintWeight,0,topTintWeight.length);System.arraycopy(sideTintWeight,0,copy.sideTintWeight,0,sideTintWeight.length);System.arraycopy(spawnOn,0,copy.spawnOn,0,spawnOn.length);System.arraycopy(spawnIn,0,copy.spawnIn,0,spawnIn.length);
  copy.shapes=new float[shapes.length][];for(int i=0;i<shapes.length;i++){copy.shapes[i]=shapes[i].clone();for(int b=0;b<shapes[i].length;b+=6){float x0=shapes[i][b],z0=shapes[i][b+2],x1=shapes[i][b+3],z1=shapes[i][b+5];for(int turn=0;turn<(q&3);turn++){float n0=1-z1,n1=1-z0;z0=x0;z1=x1;x0=n0;x1=n1;}copy.shapes[i][b]=x0;copy.shapes[i][b+2]=z0;copy.shapes[i][b+3]=x1;copy.shapes[i][b+5]=z1;}}
  return copy;
 }
}
