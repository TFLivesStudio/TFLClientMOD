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
            }
        });
        if (!result.success) throw new IllegalStateException("Cannot register managed DH runtime policy: " + result.message);
    }
}
*///?}
