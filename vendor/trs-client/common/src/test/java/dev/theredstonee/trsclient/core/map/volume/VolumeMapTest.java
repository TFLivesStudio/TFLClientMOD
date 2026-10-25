package dev.theredstonee.trsclient.core.map.volume;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
class VolumeMapTest {
 private static final CloudVolume FIELD=new CloudVolume();
 @Test void inversePickingAtEverySupportedTiltYawAndNegativeCoordinate(){
  double[] ground=new double[3],pixel=new double[2];
  for(double tilt:new double[]{0,15,30,55})for(double yaw:new double[]{-180,-90,-30,0,90,180}){
   MapProjection p=new MapProjection(-1027,318,.7,1280,720,tilt,yaw);
   for(int y=0;y<=720;y+=60)for(int x=0;x<=1280;x+=80){p.ground(x,y,ground);p.project(ground[0],0,ground[1],pixel);assertEquals(x,pixel[0],1e-8);assertEquals(y,pixel[1],1e-8);}
  }
 }
 @Test void topViewMatchesExistingMapCoordinates(){MapProjection p=new MapProjection(-17,-33,2,800,600,0,0);double[] out=new double[3];p.ground(500,400,out);assertEquals(33,out[0],1e-10);assertEquals(17,out[1],1e-10);}
 @Test void heightProducesParallaxAndAllAnglesAreFinite(){double[] a=new double[2],b=new double[2];MapProjection p=new MapProjection(0,0,1,800,600,45,0);p.project(0,0,0,a);p.project(0,48,0,b);assertTrue(Math.abs(a[1]-b[1])>10);assertThrows(IllegalArgumentException.class,()->new MapProjection(0,0,1,800,600,Double.NaN,0));}
 @Test void volumeVariesVerticallyAndIsPeriodic(){assertEquals(0,FIELD.density(.3,0,.5));assertEquals(0,FIELD.density(.3,1,.5));double sum=0,change=0;for(int i=0;i<100;i++){float a=FIELD.density(i*.013,.3,.4);sum+=a;change+=Math.abs(a-FIELD.density(i*.013,.7,.4));assertEquals(a,FIELD.density(i*.013+1,.3,1.4),1e-5);}assertTrue(sum>0);assertTrue(change>1);}
 private TerrainSnapshot snapshot(float fog,int color){int[] c=new int[16];float[] f=new float[16];Arrays.fill(c,color);Arrays.fill(f,fog);return new TerrainSnapshot(-10000,-10000,10000,10000,4,c,f);}
 @Test void fullyKnownTerrainGetsNoCloudsOrShadows(){int[] out=new int[128*72];new VolumeRaymarcher(FIELD).render(new MapProjection(0,0,1,1280,720,30,0),snapshot(0,0xFF568934),CloudQuality.LOW,128,72,out,0,1,48,4096);for(int color:out)assertEquals(0xFF568934,color);}
 @Test void perspectivesRevealDifferentVolumeSilhouettesAndOffRejects(){int[] a=new int[128*72],b=a.clone();VolumeRaymarcher r=new VolumeRaymarcher(FIELD);TerrainSnapshot t=snapshot(1,0xFF253340);r.render(new MapProjection(0,0,1,1280,720,0,0),t,CloudQuality.LOW,128,72,a,0,1,48,4096);r.render(new MapProjection(0,0,1,1280,720,55,0),t,CloudQuality.LOW,128,72,b,0,1,48,4096);assertFalse(Arrays.equals(a,b));assertTrue(Arrays.stream(a).distinct().count()>100);assertThrows(IllegalArgumentException.class,()->r.render(new MapProjection(0,0,1,800,600,0,0),t,CloudQuality.OFF,128,72,a,0,1,48,4096));}
 @Test void outsideSnapshotNeverRevealsTerrain(){TerrainSnapshot t=snapshot(0,0xFF00FF00);assertEquals(1,t.fog(20000,0));assertEquals(0xFF253340,t.color(20000,0));}
 @Test void rotatedGridClipsToTheActualFrustum(){
  double[] line=new double[4],world=new double[3];
  for(double tilt:new double[]{0,30,55})for(double yaw:new double[]{-120,-30,0,45,170}){
   MapProjection camera=new MapProjection(-17,-33,6,1280,720,tilt,yaw);
   for(int axis=0;axis<2;axis++)for(int j=-5;j<=5;j++){
    double at=(axis==0?camera.cx:camera.cz)+j*16;
    if(!camera.groundGridSegment(axis==0,at,line))continue;
    for(int k=0;k<2;k++){double x=line[k*2],y=line[k*2+1];assertTrue(x>=-1e-6&&x<=1280+1e-6&&y>=-1e-6&&y<=720+1e-6);camera.ground(x,y,world);assertEquals(at,world[axis],1e-7);}
   }
  }
 }

}
