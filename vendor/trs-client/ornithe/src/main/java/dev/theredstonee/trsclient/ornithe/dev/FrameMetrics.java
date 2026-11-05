package dev.theredstonee.trsclient.ornithe.dev;

import com.google.gson.JsonObject;
import java.lang.management.ManagementFactory;
import java.util.Arrays;

/** Bounded, native frame measurements from a separately packaged development mixin. */
public final class FrameMetrics {
    private static final long[] frames=new long[8192];
    private static int count;private static long last,started,cpuStart;private static boolean active;
    private static jdk.jfr.Recording recording;
    public static void begin(){if(Boolean.getBoolean("tflcore.audit5.jfr")){recording=new jdk.jfr.Recording();recording.enable("jdk.ExecutionSample").withPeriod(java.time.Duration.ofMillis(10));recording.enable("jdk.NativeMethodSample").withPeriod(java.time.Duration.ofMillis(10));recording.setMaxSize(16*1024*1024);recording.start();}count=0;last=0;started=System.nanoTime();cpuStart=cpu();active=true;}
    public static void frame(){if(!active)return;long now=System.nanoTime();if(last!=0&&count<frames.length)frames[count++]=now-last;last=now;}
    private static long cpu(){return ((com.sun.management.OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean()).getProcessCpuTime();}
    public static JsonObject finish(){active=false;if(recording!=null){recording.stop();try{recording.dump(java.nio.file.Paths.get(System.getProperty("tflcore.ornithe.report")).resolve("frame-profile.jfr"));}catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}finally{recording.close();recording=null;}}if(count<30)throw new AssertionError("Insufficient actual rendered frames: "+count);long elapsed=System.nanoTime()-started,used=cpu()-cpuStart;long[] sorted=Arrays.copyOf(frames,count);Arrays.sort(sorted);JsonObject result=new JsonObject();result.addProperty("frames",count);result.addProperty("seconds",elapsed/1e9);result.addProperty("frameMsP50",sorted[(int)((count-1)*.50)]/1e6);result.addProperty("frameMsP95",sorted[(int)((count-1)*.95)]/1e6);result.addProperty("frameMsP99",sorted[(int)((count-1)*.99)]/1e6);result.addProperty("processCpuSeconds",used/1e9);result.addProperty("heapMiB",ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed()/1048576d);result.addProperty("physicalGpu",false);return result;}
}
