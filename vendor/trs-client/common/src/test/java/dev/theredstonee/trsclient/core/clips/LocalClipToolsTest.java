package dev.theredstonee.trsclient.core.clips;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class LocalClipToolsTest {
 @Test void invalidRangeCannotStartAnExport(){LocalClipTools.Edit e=new LocalClipTools.Edit(10);e.start=11;assertThrows(IllegalArgumentException.class,e::validate);e.start=Double.NaN;assertThrows(IllegalArgumentException.class,e::validate);}
 @Test void inputPathsRemainLiteralArguments(){LocalClipTools.Edit e=new LocalClipTools.Edit(3);Path p=Paths.get("clip $(touch injected); 'quote'.mp4");List<String> args=LocalClipTools.command(p,Paths.get("result.mp4"),e);assertEquals(p.toAbsolutePath().toString(),args.get(args.indexOf("-i")+1));assertFalse(args.contains("sh"));assertTrue(args.contains("-n"));}
 @Test void clipRangeIsAnInputLimitBeforeSpeedTransform(){LocalClipTools.Edit e=new LocalClipTools.Edit(3);e.start=1;e.speed=2;List<String> args=LocalClipTools.command(Paths.get("in.mp4"),Paths.get("out.mp4"),e);assertTrue(args.indexOf("-t")<args.indexOf("-i"));assertTrue(args.contains("setpts=PTS/2.000000"));assertTrue(args.contains("atempo=2.000000"));}
 @Test void realExportPreservesOriginalAndProducesRequestedDuration(@TempDir Path dir) throws Exception {
  boolean available;try{available=new ProcessBuilder("ffmpeg","-version").redirectOutput(ProcessBuilder.Redirect.to(new java.io.File("/dev/null"))).redirectError(ProcessBuilder.Redirect.to(new java.io.File("/dev/null"))).start().waitFor()==0;}catch(Exception ex){available=false;}Assumptions.assumeTrue(available,"local FFmpeg integration");
  Path input=dir.resolve("clip ; literal.mp4");Process generate=new ProcessBuilder("ffmpeg","-v","error","-f","lavfi","-i","color=c=red:s=64x48:r=30","-t","4","-c:v","libx264","-threads","2",input.toString()).inheritIO().start();assertEquals(0,generate.waitFor());byte[] original=Files.readAllBytes(input);
  LocalClipTools.Edit edit=new LocalClipTools.Edit(3);edit.start=1;edit.speed=2;edit.mute=true;LocalClipTools tool=new LocalClipTools();try{Path result=tool.export(input,edit);assertArrayEquals(original,Files.readAllBytes(input));assertNotEquals(input,result);assertEquals(1,LocalClipTools.duration(result),.12);ClipPreview.Sheet sheet=LocalClipTools.preview(result);assertTrue(sheet.frames>1);assertEquals(2560,sheet.width);}finally{tool.close();}
 }
}
