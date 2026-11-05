package dev.theredstonee.trsclient.core.terrain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import static org.junit.jupiter.api.Assertions.*;

class FarTerrainTest {
    @TempDir Path root;
    private final List<FarTerrainEngine> opened=new ArrayList<>();
    private FarTerrainEngine engine(Path path,String id,long budget){FarTerrainEngine e=new FarTerrainEngine(path,id,budget);opened.add(e);return e;}
    @org.junit.jupiter.api.AfterEach void workersFinishBeforeFixtureCleanup() throws Exception {for(FarTerrainEngine e:opened){e.close();until(e::stopped,()->{});}}
    static TerrainColumns chunk(int x,int z,int height) {
        short[] h=new short[256];int[] rgb=new int[256];byte[] light=new byte[256];Arrays.fill(h,(short)height);Arrays.fill(rgb,0x80c050);Arrays.fill(light,(byte)15);
        return new TerrainColumns(x,z,h,rgb,light);
    }
    static void until(BooleanSupplier ready,Runnable tick) throws Exception {
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(!ready.getAsBoolean() && System.nanoTime()<end){tick.run();java.util.concurrent.locks.LockSupport.parkNanos(1000000);}
        assertTrue(ready.getAsBoolean(),"Timed out waiting for bounded terrain work");
    }
    @Test void derivedCacheSurvivesRestartAndSeparatesSameDimensionSubworlds() throws Exception {
        TerrainStore a=new TerrainStore(root,"server/world/a/overworld",1048576),b=new TerrainStore(root,"server/world/b/overworld",1048576),nether=new TerrainStore(root,"server/world/a/nether",1048576);
        TerrainColumns columns=chunk(-5,7,73);a.write(columns);
        assertTrue(columns.same(new TerrainStore(root,"server/world/a/overworld",1048576).read(-5,7)));
        assertNull(b.read(-5,7));assertNull(nether.read(-5,7));assertEquals(Collections.singleton(columns.key()),a.index());
        assertNotEquals(a.folder(),b.folder());assertNotEquals(a.folder(),nether.folder());
        Files.write(a.folder().resolve("-5_7.lod"),new byte[]{1,2,3});assertThrows(java.io.IOException.class,()->a.read(-5,7));
    }
    @Test void recreatedLocalSaveAndResourcePaletteCannotReuseWrongWorldOrColors() throws Exception {
        Path save=root.resolve("same-name");String first=TerrainStore.localSaveIdentity(save);
        assertEquals(first,TerrainStore.localSaveIdentity(save));Files.delete(save.resolve("tfl-far-terrain-id.txt"));
        assertNotEquals(first,TerrainStore.localSaveIdentity(save));
        short[] height=new short[256];int[] rgb=new int[256],states=new int[256],tints=new int[256];byte[] light=new byte[256];
        Arrays.fill(height,(short)80);Arrays.fill(states,7);Arrays.fill(tints,0x80ff40);Arrays.fill(light,(byte)0xf3);
        TerrainColumns before=new TerrainColumns(0,0,height,rgb,light,states,tints);TerrainStore store=new TerrainStore(root,"palette",1048576);store.write(before);
        int[] palette=new int[8];palette[7]=0xc080ff;TerrainColumns recolored=store.read(0,0).recolored(palette);
        assertEquals(TerrainColumns.multiply(0xc080ff,0x80ff40),recolored.color(0));assertEquals(15,recolored.skyLight(0));assertEquals(3,recolored.blockLight(0));assertEquals(80,recolored.height(0));
    }
    @Test void allResolutionsContainActualHeightsAndCliffWallsWithoutUnknownTerrain() throws Exception {
        Map<Long,TerrainColumns> data=new HashMap<>();data.put(TerrainColumns.key(0,0),chunk(0,0,70));data.put(TerrainColumns.key(1,0),chunk(1,0,30));
        int before=Integer.MAX_VALUE;
        for(int step:new int[]{1,2,4,8}){
            LodMesh mesh=LodMesh.build(0,0,step,1,data);assertTrue(mesh.vertices.length>0);assertTrue(mesh.vertices.length<before);before=mesh.vertices.length;
            assertEquals(0,mesh.vertices.length%32);boolean high=false,low=false,wall=false;
            for(int i=0;i<mesh.vertices.length;i+=8){assertTrue(mesh.vertices[i]>=0 && mesh.vertices[i]<=32);assertTrue(mesh.vertices[i+2]>=0 && mesh.vertices[i+2]<=16);high|=mesh.vertices[i+1]==70;low|=mesh.vertices[i+1]==30;}
            for(int i=0;i<mesh.vertices.length;i+=32)wall|=mesh.vertices[i+1]!=mesh.vertices[i+9] || mesh.vertices[i+9]!=mesh.vertices[i+17];
            assertTrue(high&&low&&wall,"Both elevations and vertical geometry must remain real at step "+step);
        }
        assertEquals(0,LodMesh.build(5,5,8,1,data).vertices.length);
    }
    @Test void lightingAndChangedTerrainInvalidateOnlyDependentRegions() throws Exception {
        try(FarTerrainEngine engine=engine(root,"world/a/0",1048576)){
            engine.capture(0,0,()->chunk(0,0,70));engine.capture(20,0,()->chunk(20,0,70));
            until(()->engine.columnCount()==2,()->engine.tick(0,0,96));
            long distant=engine.regions().get(TerrainColumns.key(5,0)),near=engine.regions().get(TerrainColumns.key(0,0));
            TerrainColumns base=chunk(0,0,70);byte[] light=new byte[256];short[] heights=new short[256];int[] colors=new int[256];
            Arrays.fill(heights,(short)70);Arrays.fill(colors,0x80c050);Arrays.fill(light,(byte)3);
            TerrainColumns dim=new TerrainColumns(0,0,heights,colors,light);assertFalse(base.same(dim));
            engine.capture(0,0,()->dim);until(()->engine.column(0,0).light(0)==3,()->engine.tick(0,0,96));
            assertEquals(distant,engine.regions().get(TerrainColumns.key(5,0)));assertTrue(engine.regions().get(TerrainColumns.key(0,0))>near);
            long changed=engine.regions().get(TerrainColumns.key(0,0));engine.capture(0,0,()->dim);
            until(()->engine.column(0,0)==dim,()->engine.tick(0,0,96));assertEquals(changed,engine.regions().get(TerrainColumns.key(0,0)));
        }
    }
    @Test void closeDrainsAcceptedCacheWritesAndRestartRestoresWithoutWorldAccess() throws Exception {
        FarTerrainEngine engine=engine(root,"world/restart/0",1048576);
        assertTrue(engine.capture(4,-3,()->chunk(4,-3,90)));
        until(()->engine.columnCount()==1,()->engine.tick(4,-3,96));engine.close();
        until(engine::stopped,()->{});
        try(FarTerrainEngine reopened=engine(root,"world/restart/0",1048576)){
            until(()->reopened.columnCount()==1,()->reopened.tick(4,-3,96));assertEquals(90,reopened.column(4,-3).height(0));assertTrue(reopened.diskHits>0);
        }
        try(FarTerrainEngine other=engine(root,"world/other/0",1048576)){
            other.tick(4,-3,96);assertEquals(0,other.columnCount());assertNull(other.pollMesh());
        }
    }
    @Test void cancelledCaptureCannotResurrectDisposedWorldOrItsGeometry() throws Exception {
        FarTerrainEngine engine=engine(root,"world/stale/0",1048576);CountDownLatch entered=new CountDownLatch(1),finish=new CountDownLatch(1);
        engine.capture(0,0,()->{entered.countDown();finish.await();return chunk(0,0,100);});assertTrue(entered.await(3,TimeUnit.SECONDS));engine.close();finish.countDown();
        until(engine::stopped,()->{});engine.tick(0,0,96);assertEquals(0,engine.columnCount());assertNull(engine.pollMesh());assertFalse(engine.capture(0,0,()->chunk(0,0,100)));
    }
}
