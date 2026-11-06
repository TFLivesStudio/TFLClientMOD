package dev.theredstonee.trsclient.ornithe;

import java.util.List;
import us.z1x.lucerna.Lucerna;
import us.z1x.lucerna.api.farterrain.*;
import us.z1x.lucerna.shadows.ShadowRenderingState;

/** Compiled against Lucerna's real, compile-only API. Never bundled or shaded. */
final class LucernaFarTerrainAdapter implements ShaderFarTerrainBridge {
    private Object pipeline,failedPipeline;
    private FarTerrainFrame frame;
    private String lastDiagnostic="";
    LucernaFarTerrainAdapter() throws ReflectiveOperationException {
        if(FarTerrainApi.class.getField("VERSION").getInt(null)!=1)
            throw new LinkageError("Unsupported Lucerna Far Terrain API");
    }
    public Status status() {
        Object current=Lucerna.getPipelineManager().getPipelineNullable();
        if(current!=pipeline){pipeline=current;failedPipeline=null;frame=null;lastDiagnostic="";}
        if(!FarTerrainApi.isPipelineActive())return Status.LEGACY;
        if(!FarTerrainApi.enabled()) {
            diagnostic("Lucerna Far Terrain bridge is disabled; enable -Dlucerna.experimentalFarTerrain=true to use shader-pack DH programs");
            return Status.LEGACY;
        }
        if(current==failedPipeline)return Status.SKIP;
        if(!FarTerrainApi.hasProgram(FarTerrainApi.Phase.TERRAIN)||!FarTerrainApi.hasProgram(FarTerrainApi.Phase.WATER)){
            diagnostic("LOD omitted: the active shader-pack dimension has no complete dh_terrain/dh_water program pairs");
            return Status.SKIP;
        }
        return Status.READY;
    }
    public boolean shadowPass(){return ShadowRenderingState.areShadowsCurrentlyBeingRendered();}
    public boolean terrain(float[] p,float[] m,float near,float far,int blocks,List<Batch> batches) {
        try {
            frame=new FarTerrainFrame(p,m,near,far,blocks);
            var result=FarTerrainApi.render(FarTerrainApi.Phase.TERRAIN,frame,meshes(batches));
            if(result!=FarTerrainApi.Result.RENDERED){handle(result);frame=null;return false;}
            return true;
        } catch(RuntimeException|LinkageError error){failedPipeline=pipeline;frame=null;diagnostic("Lucerna Far Terrain bridge failed: "+error+"; reload the shader pack to retry");return false;}
    }
    public void water(List<Batch> batches) {
        if(status()!=Status.READY || frame==null || shadowPass())return;
        var sameFrame=frame;frame=null;
        try {
            var result=FarTerrainApi.render(FarTerrainApi.Phase.WATER,sameFrame,meshes(batches));
            if(result!=FarTerrainApi.Result.RENDERED)handle(result);
        } catch(RuntimeException|LinkageError error){failedPipeline=pipeline;diagnostic("Lucerna Far Terrain water failed: "+error+"; reload the shader pack to retry");}
    }
    private static List<FarTerrainMesh> meshes(List<Batch> input){return input.stream().map(b->new FarTerrainMesh(b.buffer(),b.count(),b.x(),b.y(),b.z())).toList();}
    private void handle(FarTerrainApi.Result result){
        if(result==FarTerrainApi.Result.FAILED)failedPipeline=pipeline;
        diagnostic("Lucerna Far Terrain result "+result+"; no internal-shader retry in this frame"+(result==FarTerrainApi.Result.FAILED?"; reload the shader pack to retry":""));
    }
    private void diagnostic(String message){if(!message.equals(lastDiagnostic)){lastDiagnostic=message;OrnitheClient.LOG.warn(message);}}
    public void reset(){frame=null;}
    public String diagnostic(){return lastDiagnostic;}
}
