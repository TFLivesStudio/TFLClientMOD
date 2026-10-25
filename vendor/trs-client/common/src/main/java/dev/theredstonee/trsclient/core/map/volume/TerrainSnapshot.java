package dev.theredstonee.trsclient.core.map.volume;

/** Immutable received-data snapshot, handed from client thread to the bounded volume worker. */
public final class TerrainSnapshot {
    public final double x0,z0,x1,z1;public final int size;public final int fingerprint;private final int[] colors;private final float[] fog;
    public TerrainSnapshot(double x0,double z0,double x1,double z1,int size,int[] colors,float[] fog){
        this.x0=x0;this.z0=z0;this.x1=x1;this.z1=z1;this.size=size;this.colors=colors;this.fog=fog;this.fingerprint=31*java.util.Arrays.hashCode(colors)+java.util.Arrays.hashCode(fog);
        if(size<2||colors.length!=size*size||fog.length!=colors.length||x1<=x0||z1<=z0)throw new IllegalArgumentException("Terrain snapshot bounds");
    }
    private int index(double x,double z){int px=(int)Math.floor((x-x0)/(x1-x0)*size),pz=(int)Math.floor((z-z0)/(z1-z0)*size);return px<0||pz<0||px>=size||pz>=size?-1:pz*size+px;}
    public int color(double x,double z){int i=index(x,z);return i<0?0xFF253340:colors[i];}
    public float fog(double x,double z){int i=index(x,z);return i<0?1:fog[i];}
}
