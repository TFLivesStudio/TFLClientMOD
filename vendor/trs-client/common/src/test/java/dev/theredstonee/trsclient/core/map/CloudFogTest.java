package dev.theredstonee.trsclient.core.map;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
class CloudFogTest {
    @Test void cloudsAreDeterministicOpaqueAndShaded(){
        int[] a=CloudFog.pixels(),b=CloudFog.pixels();
        assertArrayEquals(a,b);assertEquals(256*256,a.length);
        for(int color:a)assertEquals(255,color>>>24);
        assertTrue(Arrays.stream(a).distinct().count()>32);
    }
    @Test void fogDoesNotInventOrPersistTerrain(){
        MapRegion r=new MapRegion(0,0);int[] original=r.pixels.clone(),out=new int[MapRegion.AREA];
        CloudFog.pixels();MapCompose.compose(null,r,out);
        assertArrayEquals(original,r.pixels);
        assertTrue(Arrays.stream(out).allMatch(p->p==0));
        assertEquals(0,r.version);
    }
}
