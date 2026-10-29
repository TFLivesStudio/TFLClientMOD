package dev.theredstonee.trsclient.core.branding;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BrandingTest {
 @Test void publicNameContainsOnlyProductAndMinecraftVersion(){
  assertEquals("TFL Client 1.8.9",dev.theredstonee.trsclient.core.product.Product.displayName("1.8.9"));
  assertEquals("TFL Client 26.3",dev.theredstonee.trsclient.core.product.Product.displayName("26.3"));
 }
 @Test void allIconSizesKeepBlackCornersAndWhiteLogo(){
  for(int size:BrandIcons.SIZES){int[] p=BrandIcons.pixels(size);assertEquals(size*size,p.length);assertEquals(0xff000000,p[0]);assertEquals(0xff000000,p[p.length-1]);int white=0;for(int pixel:p)if((pixel&255)>150)white++;assertTrue(white>size*size/8,"logo must remain legible at "+size);assertSame(p,BrandIcons.pixels(size),"reuse prepared icon pixels");}
 }
}
