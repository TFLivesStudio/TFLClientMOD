package dev.theredstonee.trsclient.core.map.volume;

/** Immutable pinhole camera. Ground is world X/Z, height is world Y; positive screen Y is down. */
public final class MapProjection {
    public final double cx,cz,scale,tilt,yaw,focal,distance,eyeX,eyeY,eyeZ;
    public final int width,height;
    private final double sy,cy,st,ct;
    private final double[] corners=new double[8];
    public final double minX,maxX,minZ,maxZ;
    public MapProjection(double cx,double cz,double scale,int width,int height,double tilt,double yaw){
        if(!Double.isFinite(tilt)||!Double.isFinite(yaw)||!Double.isFinite(cx)||!Double.isFinite(cz)||!Double.isFinite(scale)||scale<=0||width<=0||height<=0)throw new IllegalArgumentException("Camera bounds");
        this.cx=cx;this.cz=cz;this.scale=scale;this.width=width;this.height=height;
        this.tilt=Math.max(0,Math.min(55,tilt));this.yaw=yaw;
        sy=Math.sin(Math.toRadians(yaw));cy=Math.cos(Math.toRadians(yaw));st=Math.sin(Math.toRadians(this.tilt));ct=Math.cos(Math.toRadians(this.tilt));
        focal=height/(2*Math.tan(Math.toRadians(22.5)));distance=focal/scale;
        eyeX=cx+sy*st*distance;eyeY=ct*distance;eyeZ=cz+cy*st*distance;
        double[] point=new double[3];double x0=Double.POSITIVE_INFINITY,z0=x0,x1=Double.NEGATIVE_INFINITY,z1=x1;
        for(int i=0;i<4;i++){ground(i==1||i==2?width:0,i>=2?height:0,point);corners[i*2]=point[0];corners[i*2+1]=point[1];x0=Math.min(x0,point[0]);x1=Math.max(x1,point[0]);z0=Math.min(z0,point[1]);z1=Math.max(z1,point[1]);}
        minX=x0;maxX=x1;minZ=z0;maxZ=z1;
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
    /** Clip a constant-world-X or Z grid line against the actual ground frustum. No per-line allocation. */
    public boolean groundGridSegment(boolean constantX,double value,double[] out){
        double firstX=0,firstY=0;boolean found=false;int axis=constantX?0:1;
        for(int i=0;i<4;i++){
            int j=(i+1)%4;double a=corners[i*2+axis],b=corners[j*2+axis];
            if(Math.abs(b-a)<1e-10||value<Math.min(a,b)||value>Math.max(a,b))continue;
            double t=(value-a)/(b-a),x=corners[i*2]+t*(corners[j*2]-corners[i*2]),z=corners[i*2+1]+t*(corners[j*2+1]-corners[i*2+1]);
            project(x,0,z,out);
            if(!found){firstX=out[0];firstY=out[1];found=true;}
            else if(Math.abs(out[0]-firstX)+Math.abs(out[1]-firstY)>1e-5){out[2]=out[0];out[3]=out[1];out[0]=firstX;out[1]=firstY;return true;}
        }
        return false;
    }
    public boolean sameView(MapProjection p){return p!=null&&width==p.width&&height==p.height&&Math.abs(cx-p.cx)<.01&&Math.abs(cz-p.cz)<.01&&Math.abs(scale-p.scale)<1e-5&&Math.abs(tilt-p.tilt)<.02&&Math.abs(yaw-p.yaw)<.02;}
}
