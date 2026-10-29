package dev.theredstonee.trsclient.core.performance;
import java.lang.management.*;
import java.util.*;
/** Opt-in measurements. Sorting occurs only when exporting a report. */
public final class UiProfiler {
 private static final ThreadMXBean THREADS=ManagementFactory.getThreadMXBean();
 private static final long[] frames=new long[4096],cpu=new long[4096];
 private static final long[] world=new long[4096];private static int worldCount;private static long lastWorld;
 private static int count;private static long last;private static boolean active;
 private UiProfiler(){}
 public static void start(){count=0;last=0;worldCount=0;lastWorld=0;active=true;}
 public static void worldFrame(){if(!active)return;long now=System.nanoTime();if(lastWorld!=0)world[worldCount++%world.length]=now-lastWorld;lastWorld=now;}
 public static boolean active(){return active;}
 public static long cpuTime(){return active&&THREADS.isCurrentThreadCpuTimeSupported()?THREADS.getCurrentThreadCpuTime():0;}
 public static void frame(long started,long cpuStart){if(!active)return;long now=System.nanoTime();int i=count%frames.length;frames[i]=last==0?0:started-last;cpu[i]=Math.max(0,cpuTime()-cpuStart);last=started;count++;}
 public static Map<String,Object> stop(){active=false;Map<String,Object> out=new LinkedHashMap<String,Object>();int size=Math.min(count,frames.length);long[] a=Arrays.copyOf(frames,size),b=Arrays.copyOf(cpu,size);Arrays.sort(a);Arrays.sort(b);out.put("samples",count);out.put("renderCallbackIntervalP95Ms",percentile(a,.95));out.put("renderCallbackIntervalP99Ms",percentile(a,.99));out.put("uiThreadCpuP95Ms",percentile(b,.95));out.put("uiThreadCpuP99Ms",percentile(b,.99));long[] w=Arrays.copyOf(world,Math.min(worldCount,world.length));Arrays.sort(w);out.put("worldFrames",worldCount);out.put("worldCallbackP95Ms",percentile(w,.95));out.put("worldCallbackP99Ms",percentile(w,.99));out.put("gpuTimingMeasured",false);return out;}
 private static double percentile(long[] values,double quantile){return values.length==0?0:values[Math.min(values.length-1,(int)Math.ceil(values.length*quantile)-1)]/1e6;}
}
