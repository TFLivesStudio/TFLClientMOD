//? if >=26.3 {
/*package dev.theredstonee.trsclient.compat.dh;

import com.seibel.distanthorizons.api.methods.events.DhApiEventRegister;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiAfterDhInitEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import com.seibel.distanthorizons.core.config.Config;

// DH's public config API has no updater switch. This adapter is pinned to 3.3.4;
// it applies in-memory API overrides after config load, preserving the saved values.
public final class DhRuntimePolicy {
    private DhRuntimePolicy() {}
    public static void register() {
        var result = DhApiEventRegister.on(DhApiAfterDhInitEvent.class, new DhApiAfterDhInitEvent() {
            @Override public void afterDistantHorizonsInit(DhApiEventParam<Void> event) {
                Config.Client.Advanced.AutoUpdater.enableAutoUpdater.setApiValue(false, "TFL Core runtime management");
                Config.Client.Advanced.AutoUpdater.enableSilentUpdates.setApiValue(false, "TFL Core runtime management");
                DhWorldSessions.install();
            }
        });
        if (!result.success) throw new IllegalStateException("Cannot register managed DH runtime policy: " + result.message);
    }
    private static Boolean rendering;
    private static int distance=-1;
    public static void tick(net.minecraft.client.Minecraft mc){
        if(com.seibel.distanthorizons.api.DhApi.Delayed.configs==null || dev.theredstonee.trsclient.TrsClient.get()==null)return;
        DhWorldSessions.tick(mc);
        var module=dev.theredstonee.trsclient.TrsClient.get().modules().farTerrain;
        var graphics=com.seibel.distanthorizons.api.DhApi.Delayed.configs.graphics();
        if(rendering==null||rendering!=module.isEnabled()||graphics.renderingEnabled().getValue()!=module.isEnabled()){
            var result=graphics.renderingEnabled().setValue(module.isEnabled(),"TFL Core Far Terrain");
            if(!result)throw new IllegalStateException("Cannot apply TFL terrain renderer state");
            rendering=module.isEnabled();
            if(!rendering&&com.seibel.distanthorizons.api.DhApi.Delayed.renderProxy!=null)com.seibel.distanthorizons.api.DhApi.Delayed.renderProxy.clearRenderDataCache();
        }
        if(distance!=module.distance.getInt()||graphics.chunkRenderDistance().getValue()!=module.distance.getInt()){
            var result=graphics.chunkRenderDistance().setValue(module.distance.getInt(),"TFL Core Far Terrain");
            if(!result)throw new IllegalStateException("Cannot apply TFL terrain distance");
            distance=module.distance.getInt();
        }
    }
}
*///?}
