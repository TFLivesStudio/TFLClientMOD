package dev.theredstonee.trsclient.ornithe;

import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

/** Optional boundary: no Lucerna-owned types appear in the base renderer. */
interface ShaderFarTerrainBridge {
    enum Status { LEGACY, READY, SKIP }
    record Batch(int buffer,int count,float x,float y,float z,double distance) {}
    Status status();
    boolean shadowPass();
    boolean terrain(float[] projection,float[] model,float near,float far,int blocks,List<Batch> batches);
    void water(List<Batch> batches);
    void reset();
    default String diagnostic(){return "";}
    static ShaderFarTerrainBridge create() {
        if(FabricLoader.getInstance().isModLoaded("lucerna")) {
            try {
                Class.forName("us.z1x.lucerna.api.farterrain.FarTerrainApi",false,ShaderFarTerrainBridge.class.getClassLoader());
                return new LucernaFarTerrainAdapter();
            } catch(ReflectiveOperationException|LinkageError incompatible) {
                OrnitheClient.LOG.warn("Lucerna Far Terrain API v1 unavailable; shader LOD integration requires Lucerna 0.1.5-tfl.1",incompatible);
                return new ShaderFarTerrainBridge() {
                    public Status status(){
                        try{return (boolean)Class.forName("us.z1x.lucerna.Lucerna").getMethod("isShaderPipelineActive").invoke(null)?Status.SKIP:Status.LEGACY;}
                        catch(ReflectiveOperationException|LinkageError e){return Status.SKIP;}
                    }
                    public boolean shadowPass(){return false;}
                    public boolean terrain(float[] p,float[] m,float n,float f,int b,List<Batch> batches){return false;}
                    public void water(List<Batch> batches){}
                    public void reset(){}
                };
            }
        }
        return null;
    }
}
