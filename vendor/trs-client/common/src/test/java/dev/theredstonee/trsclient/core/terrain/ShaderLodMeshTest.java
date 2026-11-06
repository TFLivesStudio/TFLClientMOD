package dev.theredstonee.trsclient.core.terrain;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ShaderLodMeshTest {
    private TerrainColumns chunk(int x,int state,int color) {
        short[] h=new short[256];int[] states=new int[256],rgb=new int[256],tints=new int[256];byte[] light=new byte[256];
        Arrays.fill(h,(short)64);Arrays.fill(states,state);Arrays.fill(rgb,color);Arrays.fill(tints,-1);Arrays.fill(light,(byte)0xa3);
        return new TerrainColumns(x,0,h,rgb,light,states,tints);
    }
    @Test void shaderMeshSeparatesWaterAndKeepsRawColorNormalsCenteredLightAndMiniIds() throws Exception {
        Map<Long,TerrainColumns> chunks=new HashMap<>();chunks.put(TerrainColumns.key(0,0),chunk(0,1<<4,0x80c050));chunks.put(TerrainColumns.key(1,0),chunk(1,9<<4,0x204080));
        LodMesh mesh=LodMesh.build(0,0,1,7,chunks,true);
        assertTrue(mesh.shaderLayout);assertTrue(mesh.vertices.length>0&&mesh.waterVertices.length>0);
        for(float[] vertices:new float[][]{mesh.vertices,mesh.waterVertices}){
            assertEquals(0,vertices.length%39);
            for(int i=0;i<vertices.length;i+=13){
                boolean water=vertices==mesh.waterVertices;
                assertEquals(water?12:2,vertices[i+12]);assertEquals(water?.65f:1f,vertices[i+6]);
                assertEquals((3+.5f)/16,vertices[i+10],1e-6);assertEquals((10+.5f)/16,vertices[i+11],1e-6);
                assertEquals(1,vertices[i+7]*vertices[i+7]+vertices[i+8]*vertices[i+8]+vertices[i+9]*vertices[i+9],1e-6);
                assertEquals((water?0x20:0x80)/255f,vertices[i+3],1e-6);
            }
        }
        assertEquals(0,TerrainMaterial.classify(-1));assertEquals(6,TerrainMaterial.classify(11<<4));assertEquals(11,TerrainMaterial.classify(87<<4));
    }
    @Test void waterOrderingPreservesWholeTriangleAttributesAndUsesBackToFrontDistance() throws Exception {
        float[] input=new float[78];
        for(int i=0;i<6;i++){input[i*13]=i<3?1:9;input[i*13+6]=.65f;input[i*13+12]=12;}
        float[] sorted=WaterTriangleOrder.sort(input,0,0,0);
        assertArrayEquals(Arrays.copyOfRange(input,39,78),Arrays.copyOfRange(sorted,0,39));
        assertArrayEquals(Arrays.copyOfRange(input,0,39),Arrays.copyOfRange(sorted,39,78));
    }
}
