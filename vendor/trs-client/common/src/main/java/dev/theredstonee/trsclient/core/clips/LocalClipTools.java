package dev.theredstonee.trsclient.core.clips;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
/** Local FFmpeg edits only after an explicit export click. Originals are never overwritten. */
public final class LocalClipTools implements AutoCloseable {
 public static final class Edit {
  public double start,end,speed=1;public boolean mute,portrait,rotate;
  public Edit(double end){this.end=end;}
  public void validate(){if(!Double.isFinite(start)||!Double.isFinite(end)||start<0||end<=start||end-start>6*3600||!(speed==.5||speed==1||speed==2))throw new IllegalArgumentException("Invalid clip range or speed");}
 }
 private volatile Process process;private volatile boolean cancelled;
 public static List<String> command(Path input,Path output,Edit edit){
  edit.validate();List<String> args=new ArrayList<>(Arrays.asList("ffmpeg","-hide_banner","-nostdin","-v","error","-n","-ss",decimal(edit.start),"-t",decimal(edit.end-edit.start),"-i",input.toAbsolutePath().toString()));
  List<String> filters=new ArrayList<>();if(edit.rotate)filters.add("transpose=1");if(edit.portrait)filters.add("crop=trunc(min(iw\\,ih*9/16)/2)*2:trunc(min(ih\\,iw*16/9)/2)*2");if(edit.speed!=1)filters.add("setpts=PTS/"+decimal(edit.speed));
  if(!filters.isEmpty()){args.add("-vf");args.add(String.join(",",filters));}
  if(edit.mute)args.add("-an");else if(edit.speed!=1){args.add("-af");args.add("atempo="+decimal(edit.speed));}
  args.addAll(Arrays.asList("-c:v","libx264","-preset","fast","-threads","2","-crf","18","-pix_fmt","yuv420p","-c:a","aac","-movflags","+faststart",output.toAbsolutePath().toString()));return args;
 }
 private static String decimal(double value){return String.format(Locale.ROOT,"%.6f",value);}
 public Path export(Path input,Edit edit) throws IOException,InterruptedException {
  if(!Files.isRegularFile(input,LinkOption.NOFOLLOW_LINKS)||!input.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".mp4"))throw new IOException("Select a local MP4");
  edit.validate();if(cancelled)throw new IOException("Editor closed");Path dir=input.toAbsolutePath().getParent(),temp=dir.resolve(".tfl-export-"+UUID.randomUUID()+".mp4");
  String name=input.getFileName().toString();name=name.substring(0,name.length()-4)+"-edit-"+System.currentTimeMillis()+".mp4";Path result=dir.resolve(name);
  Path log=Files.createTempFile("tfl-clip-",".log");
  try {
   Process job=new ProcessBuilder(command(input,temp,edit)).redirectErrorStream(true).redirectOutput(log.toFile()).start();process=job;
   if(cancelled){job.destroyForcibly();throw new IOException("Export cancelled");}
   if(!job.waitFor(10,TimeUnit.MINUTES)){job.destroyForcibly();throw new IOException("Export timed out");}
   if(cancelled)throw new IOException("Export cancelled");
   if(job.exitValue()!=0||!Files.isRegularFile(temp)){
    String reason="FFmpeg export failed";if(Files.size(log)<65536)reason=new String(Files.readAllBytes(log),java.nio.charset.StandardCharsets.UTF_8);throw new IOException(reason);
   }
   Files.move(temp,result);return result;
  }finally{process=null;Files.deleteIfExists(temp);Files.deleteIfExists(log);}
 }
 public static ClipPreview.Sheet preview(Path input) throws IOException,InterruptedException {
  if(!Files.isRegularFile(input,LinkOption.NOFOLLOW_LINKS))throw new IOException("Invalid local clip");
  double duration=duration(input),rate=Math.min(30,64/Math.max(.1,duration));int frames=Math.min(64,Math.max(1,(int)Math.ceil(duration*rate)));
  Path image=Files.createTempFile("tfl-clip-preview-",".png"),log=Files.createTempFile("tfl-clip-preview-",".log");
  try {
   Process job=new ProcessBuilder("ffmpeg","-hide_banner","-nostdin","-v","error","-y","-threads","2","-i",input.toAbsolutePath().toString(),"-an","-vf","fps="+decimal(rate)+",scale=320:180:force_original_aspect_ratio=decrease,pad=320:180:(ow-iw)/2:(oh-ih)/2,tile=8x8","-frames:v","1",image.toString()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
   try{if(!job.waitFor(45,TimeUnit.SECONDS)){job.destroyForcibly();throw new IOException("Preview timed out");}if(job.exitValue()!=0||Files.size(image)>32*1024*1024)throw new IOException("FFmpeg could not read the clip");}
   finally{if(job.isAlive())job.destroyForcibly();}
   dev.theredstonee.trsclient.core.cape.PngDecoder.Image decoded=dev.theredstonee.trsclient.core.cape.PngDecoder.decode(Files.readAllBytes(image),4096L*4096);
   return new ClipPreview.Sheet(decoded.width,decoded.height,decoded.argb,frames,8,8,320,180,Math.max(1,(int)Math.round(1000/rate)),(long)(duration*1000));
  }finally{Files.deleteIfExists(image);Files.deleteIfExists(log);}
 }
 public static double duration(Path input) throws IOException,InterruptedException {
  Path log=Files.createTempFile("tfl-clip-duration-",".txt");
  try{Process job=new ProcessBuilder("ffprobe","-v","error","-show_entries","format=duration","-of","default=noprint_wrappers=1:nokey=1",input.toAbsolutePath().toString()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
   try{if(!job.waitFor(15,TimeUnit.SECONDS)||job.exitValue()!=0||Files.size(log)>4096)throw new IOException("Cannot read clip duration");double value=Double.parseDouble(new String(Files.readAllBytes(log),java.nio.charset.StandardCharsets.UTF_8).trim());if(!Double.isFinite(value)||value<=0||value>21600)throw new IOException("Clip duration exceeds supported range");return value;}finally{if(job.isAlive())job.destroyForcibly();}
  }finally{Files.deleteIfExists(log);}
 }
 public void close(){cancelled=true;Process p=process;if(p!=null)p.destroyForcibly();}
}
