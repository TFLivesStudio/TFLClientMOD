package dev.theredstonee.trsclient.core.map.volume;

/** Real front-to-back integration through a height-bearing 3D density field, independent of graphics APIs. */
public final class VolumeRaymarcher {
    private final CloudVolume volume;private final double[] ray=new double[3],ground=new double[3];
    public VolumeRaymarcher(CloudVolume volume){this.volume=volume;}
    public void render(MapProjection camera,TerrainSnapshot terrain,CloudQuality quality,int w,int h,int[] out,double clock,double density,double layerHeight,double distance){
        render(camera,terrain,quality,w,h,out,clock,density,layerHeight,distance,false);
    }
    private void render(MapProjection camera,TerrainSnapshot terrain,CloudQuality quality,int w,int h,int[] out,double clock,double density,double layerHeight,double distance,boolean cloudsOnly){
        if(w<=0||h<=0||out.length<w*h||quality==CloudQuality.OFF)throw new IllegalArgumentException("Volume frame bounds");
        double ceiling=Math.min(layerHeight,camera.eyeY*.55),bottom=ceiling*.15,wind=clock*.002;
        double period=Math.max(640,layerHeight*16);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            double sx=(x+.5)*camera.width/w,sy=(y+.5)*camera.height/h;
            camera.ray(sx,sy,ray);double groundT=-camera.eyeY/ray[1];
            double gx=camera.eyeX+ray[0]*groundT,gz=camera.eyeZ+ray[2]*groundT;
            int base=cloudsOnly?0:terrain.color(gx,gz);double red=(base>>16)&255,green=(base>>8)&255,blue=base&255;
            double entry=Math.max(0,(ceiling-camera.eyeY)/ray[1]),exit=Math.min(groundT,(bottom-camera.eyeY)/ray[1]);
            double step=Math.max(.01,(exit-entry)/quality.steps),t=entry+step*.5,trans=1,r=0,g=0,b=0;
            for(int i=0;density>0&&i<quality.steps&&t<exit&&trans>.015;i++,t+=step){
                double wx=camera.eyeX+ray[0]*t,wy=camera.eyeY+ray[1]*t,wz=camera.eyeZ+ray[2]*t;
                double mask=terrain.fog(wx,wz);if(mask<=.001)continue;
                double dx=wx-camera.cx,dz=wz-camera.cz;if(dx*dx+dz*dz>distance*distance)continue;
                double ny=(wy-bottom)/(ceiling-bottom);
                double rangeFade=Math.max(0,Math.min(1,(distance*distance-dx*dx-dz*dz)/(distance*distance*.3)));
                double d=volume.density(wx/period+wind,ny,wz/period-wind*.6)*density*mask*rangeFade;
                if(d<=.002)continue;
                double alpha=1-Math.exp(-d*step*7/Math.max(1,ceiling));
                double lighting=volume.light(wx/period+wind,ny,wz/period-wind*.6);
                double amount=trans*alpha;
                r+=amount*(142+107*lighting);g+=amount*(160+91*lighting);b+=amount*(180+73*lighting);trans*=1-alpha;
            }
            // Ground shadows are sourced only from the visual cloud field; hidden terrain stays uniform.
            if(density>0&&terrain.fog(gx,gz)<.01){
                double shadow=0;
                for(int i=1;i<=3;i++){double wx=gx+i*ceiling*.22,wz=gz-i*ceiling*.18;shadow+=volume.density(wx/period+wind,i/4.,wz/period-wind*.6)*terrain.fog(wx,wz);}
                double shade=Math.exp(-shadow*.15*density);red*=shade;green*=shade;blue*=shade;if(cloudsOnly)trans*=shade;
            }
            int rr=(int)Math.max(0,Math.min(255,r+trans*red)),gg=(int)Math.max(0,Math.min(255,g+trans*green)),bb=(int)Math.max(0,Math.min(255,b+trans*blue));
            out[y*w+x]=(cloudsOnly?(int)Math.round((1-trans)*255)<<24:0xFF000000)|(rr<<16)|(gg<<8)|bb;
        }
    }
    /** Integrate the volume at its bounded quality resolution, then compose over independently
     * sampled terrain. Raising cloud quality never reduces the known map to cloud-sized pixels. */
    public void renderComposite(MapProjection camera,TerrainSnapshot terrain,CloudQuality quality,int cw,int ch,int[] clouds,int w,int h,int[] out,double clock,double density,double height,double distance){
        render(camera,terrain,quality,cw,ch,clouds,clock,density,height,distance,true);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            camera.ground((x+.5)*camera.width/w,(y+.5)*camera.height/h,ground);
            int base=terrain.color(ground[0],ground[1]);
            double u=Math.max(0,Math.min(cw-1,(x+.5)*cw/w-.5)),v=Math.max(0,Math.min(ch-1,(y+.5)*ch/h-.5));
            int ix=(int)u,iy=(int)v,jx=Math.min(cw-1,ix+1),jy=Math.min(ch-1,iy+1);
            double fu=u-ix,fv=v-iy;int a=clouds[iy*cw+ix],b=clouds[iy*cw+jx],c=clouds[jy*cw+ix],d=clouds[jy*cw+jx];
            double alpha=channel(a,b,c,d,24,fu,fv)/255.;
            int red=(int)Math.min(255,channel(a,b,c,d,16,fu,fv)+((base>>16)&255)*(1-alpha));
            int green=(int)Math.min(255,channel(a,b,c,d,8,fu,fv)+((base>>8)&255)*(1-alpha));
            int blue=(int)Math.min(255,channel(a,b,c,d,0,fu,fv)+(base&255)*(1-alpha));
            out[y*w+x]=0xFF000000|red<<16|green<<8|blue;
        }
    }
    private static double channel(int a,int b,int c,int d,int shift,double u,double v){return ((((a>>>shift)&255)*(1-u)+((b>>>shift)&255)*u)*(1-v)+(((c>>>shift)&255)*(1-u)+((d>>>shift)&255)*u)*v);}

}
