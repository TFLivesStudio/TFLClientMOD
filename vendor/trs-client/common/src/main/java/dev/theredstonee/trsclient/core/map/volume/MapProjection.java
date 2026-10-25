package dev.theredstonee.trsclient.core.map.volume;

/** Immutable pinhole camera. Ground is world X/Z, height is world Y; positive screen Y is down. */
public final class MapProjection {
    public final double cx,cz,scale,tilt,yaw,focal,distance,eyeX,eyeY,eyeZ;
    public final int width,height;
    private final double sy,cy,st,ct;
    public MapProjection(double cx,double cz,double scale,int width,int height,double tilt,double yaw){
        if(!Double.isFinite(tilt)||!Double.isFinite(yaw)||!Double.isFinite(cx)||!Double.isFinite(cz)||!Double.isFinite(scale)||scale<=0||width<=0||height<=0)throw new IllegalArgumentException("Camera bounds");
        this.cx=cx;this.cz=cz;this.scale=scale;this.width=width;this.height=height;
        this.tilt=Math.max(0,Math.min(55,tilt));this.yaw=yaw;
        sy=Math.sin(Math.toRadians(yaw));cy=Math.cos(Math.toRadians(yaw));st=Math.sin(Math.toRadians(this.tilt));ct=Math.cos(Math.toRadians(this.tilt));
        focal=height/(2*Math.tan(Math.toRadians(22.5)));distance=focal/scale;
        eyeX=cx+sy*st*distance;eyeY=ct*distance;eyeZ=cz+cy*st*distance;
    }
    /** Returns a normalized ray in caller-owned out[3]. */
    public void ray(double screenX,double screenY,double[] out){
        double u=(screenX-width*.5)/focal,v=-(screenY-height*.5)/focal;
        double x=-sy*st+cy*u-sy*ct*v,y=-ct+st*v,z=-cy*st-sy*u-cy*ct*v;
        double inv=1/Math.sqrt(x*x+y*y+z*z);out[0]=x*inv;out[1]=y*inv;out[2]=z*inv;
    }
    public void ground(double sx,double sy,double[] out){
        ray(sx,sy,out);double t=-eyeY/out[1];double x=eyeX+t*out[0],z=eyeZ+t*out[2];out[0]=x;out[1]=z;
    }
    public void project(double x,double y,double z,double[] out){
        double dx=x-eyeX,dy=y-eyeY,dz=z-eyeZ;
        double depth=-sy*st*dx-ct*dy-cy*st*dz;
        out[0]=width*.5+focal*(cy*dx-sy*dz)/depth;
        out[1]=height*.5-focal*(-sy*ct*dx+st*dy-cy*ct*dz)/depth;
    }
    public boolean sameView(MapProjection p){return p!=null&&width==p.width&&height==p.height&&Math.abs(cx-p.cx)<.01&&Math.abs(cz-p.cz)<.01&&Math.abs(scale-p.scale)<1e-5&&Math.abs(tilt-p.tilt)<.02&&Math.abs(yaw-p.yaw)<.02;}
}
