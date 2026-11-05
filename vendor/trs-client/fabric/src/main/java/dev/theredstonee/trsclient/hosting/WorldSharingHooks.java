package dev.theredstonee.trsclient.hosting;

//? if >=26.3 {
/*import dev.theredstonee.trsclient.compat.Mc;
import dev.theredstonee.trsclient.core.hosting.e4mc.WorldShareService;
import dev.theredstonee.trsclient.core.ui.menus.WorldShareUi;
import dev.theredstonee.trsclient.screen.TrsUiScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.MinecraftServer;
*/
//?}

public final class WorldSharingHooks {
    private WorldSharingHooks() { }
    //? if >=26.3 {
    /*private static final WorldShareService SERVICE=new WorldShareService(FabricLoader.getInstance().getConfigDir().resolve("tflcore/sharing/runtime"));
    public static WorldShareService service() { return SERVICE; }
    public static void tick() { SERVICE.tick(Mc.mc().level==null ? null : Mc.mc().getSingleplayerServer()); }
    public static int port() { var s=Mc.mc().getSingleplayerServer(); return s!=null && s.isPublished() ? s.getPort() : 0; }
    public static void open(Screen parent) {
        Mc.setScreen(new TrsUiScreen("World sharing",new WorldShareUi(SERVICE,new WorldShareUi.Host() {
            public Object server() { return Mc.mc().level==null ? null : Mc.mc().getSingleplayerServer(); }
            public int lanPort() { return port(); }
            public boolean openLan() {
                var s=Mc.mc().getSingleplayerServer();
                return s!=null && (s.isPublished() || s.publishServer(MinecraftServer.MultiplayerScope.LAN,false,net.minecraft.util.HttpUtil.getAvailablePort()));
            }
            public String playerName() { return Mc.mc().player==null ? "" : Mc.mc().player.getGameProfile().name(); }
            public void copy(String address) { Mc.mc().keyboardHandler.setClipboard(address); }
            public void close() { Mc.setScreen(parent); }
        })));
    }
    *///?}
}
