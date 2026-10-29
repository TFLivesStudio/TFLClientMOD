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
}
