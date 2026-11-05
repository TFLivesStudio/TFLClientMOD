import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import net.eliotex.zirconium.utils.misc.NativeBackedImage;

/** CPU-only PNG microbenchmark. Does not represent GPU FPS or replace native acceptance. */
class TextureDecodeBenchmark {
    private static volatile int consumed;
    static BufferedImage decode(byte[] png,boolean nativePath)throws Exception{return nativePath?NativeBackedImage.make(new ByteArrayInputStream(png)):ImageIO.read(new ByteArrayInputStream(png));}
    static long measure(byte[] png,boolean fast)throws Exception{long start=System.nanoTime();BufferedImage image=decode(png,fast);consumed^=image.getRGB(image.getWidth()/2,image.getHeight()/2);image.flush();return System.nanoTime()-start;}
    public static void main(String[] args)throws Exception{
        int size=args.length==0?1024:Integer.parseInt(args[0]);ImageIO.setUseCache(false);BufferedImage source=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<size;y++)for(int x=0;x<size;x++)source.setRGB(x,y,0xff000000|((x*8747)^(y*24371))&0xffffff);
        ByteArrayOutputStream encoded=new ByteArrayOutputStream();ImageIO.write(source,"png",encoded);byte[] png=encoded.toByteArray();
        BufferedImage javaImage=decode(png,false),fastImage=decode(png,true);
        if(!Arrays.equals(javaImage.getRGB(0,0,size,size,null,0,size),fastImage.getRGB(0,0,size,size,null,0,size)))throw new AssertionError("PNG decoders differ");
        for(int n=0;n<12;n++){measure(png,false);measure(png,true);}
        long[] javaTimes=new long[32],nativeTimes=new long[32];
        for(int n=0;n<32;n++){if((n&1)==0){javaTimes[n]=measure(png,false);nativeTimes[n]=measure(png,true);}else{nativeTimes[n]=measure(png,true);javaTimes[n]=measure(png,false);}}
        Arrays.sort(javaTimes);Arrays.sort(nativeTimes);
        System.out.printf(Locale.ROOT,"{\"kind\":\"CPU-only procedural RGBA PNG decode\",\"width\":%d,\"iterations\":32,\"identicalRgb\":true,\"imageIoMedianMs\":%.6f,\"spngMedianMs\":%.6f,\"imageIoP95Ms\":%.6f,\"spngP95Ms\":%.6f,\"physicalGpu\":false}%n",size,javaTimes[16]/1e6,nativeTimes[16]/1e6,javaTimes[30]/1e6,nativeTimes[30]/1e6);
    }
}
