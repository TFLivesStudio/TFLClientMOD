import dev.theredstonee.trsclient.core.map.volume.*;
import java.util.Arrays;
/** Deterministic CPU-only fixture: no claim about native frames or physical GPU throughput. */
public final class VolumeBenchmark {
 public static void main(String[] args){
  CloudVolume volume=new CloudVolume();VolumeRaymarcher renderer=new VolumeRaymarcher(volume);
  int[] terrain=new int[512*512];float[] mask=new float[terrain.length];Arrays.fill(terrain,0xFF253340);Arrays.fill(mask,1);
  TerrainSnapshot snapshot=new TerrainSnapshot(-30000,-30000,30000,30000,512,terrain,mask);
  System.out.println("{\"scope\":\"deterministic CPU fixture, no native/GPU FPS claim\",\"results\":[");boolean first=true;
  for(CloudQuality quality:new CloudQuality[]{CloudQuality.LOW,CloudQuality.BALANCED,CloudQuality.HIGH}){
   int cw=quality.width,ch=cw*9/16,w=cw*3,h=w*9/16;int[] clouds=new int[cw*ch],out=new int[w*h];
   for(int i=0;i<3;i++)renderer.renderComposite(new MapProjection(0,0,1,1920,1080,30,0),snapshot,quality,cw,ch,clouds,w,h,out,0,1,48,4096);
   long[] times=new long[7];long hash=0;
   for(int i=0;i<times.length;i++){long t=System.nanoTime();renderer.renderComposite(new MapProjection(0,0,1,1920,1080,30,0),snapshot,quality,cw,ch,clouds,w,h,out,0,1,48,4096);times[i]=System.nanoTime()-t;hash=Arrays.hashCode(out);}
   long sum=0;for(long t:times)sum+=t;Arrays.sort(times);
   if(!first)System.out.println(",");first=false;
   System.out.print("{\"quality\":\""+quality+"\",\"meanMs\":"+sum/1e6/times.length+",\"p95Ms\":"+times[times.length-1]/1e6+",\"workingPixelBytes\":"+(long)(clouds.length+out.length)*4+",\"hash\":"+hash+"}");
  }
  System.out.println("]}");
 }
}
