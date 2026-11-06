package dev.theredstonee.trsclient.core.terrain;

import java.util.Arrays;

/** Sort whole transparent triangles without changing winding, attributes or VBO layout. */
public final class WaterTriangleOrder {
    private WaterTriangleOrder() {}
    public static float[] sort(float[] vertices,double x,double y,double z) throws InterruptedException {
        if(vertices.length%39!=0)throw new IllegalArgumentException("Expected 13-float triangle vertices");
        int count=vertices.length/39;Integer[] order=new Integer[count];double[] distances=new double[count];
        for(int i=0;i<count;i++){
            if(Thread.currentThread().isInterrupted())throw new InterruptedException();
            order[i]=i;int at=i*39;
            double dx=(vertices[at]+vertices[at+13]+vertices[at+26])/3.0-x;
            double dy=(vertices[at+1]+vertices[at+14]+vertices[at+27])/3.0-y;
            double dz=(vertices[at+2]+vertices[at+15]+vertices[at+28])/3.0-z;
            distances[i]=dx*dx+dy*dy+dz*dz;
        }
        Arrays.sort(order,(a,b)->Double.compare(distances[b],distances[a]));
        float[] out=new float[vertices.length];
        for(int i=0;i<count;i++){
            if(Thread.currentThread().isInterrupted())throw new InterruptedException();
            System.arraycopy(vertices,order[i]*39,out,i*39,39);
        }
        return out;
    }
}
