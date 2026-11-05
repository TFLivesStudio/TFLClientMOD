package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.hosting.e4mc.WorldShareService;
import dev.theredstonee.trsclient.core.ui.menus.WorldShareUi;
import dev.theredstonee.trsclient.ornithe.mixin.LanListenerAccess;
import java.net.InetSocketAddress;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;

public final class OrnitheWorldSharing {
    private static final WorldShareService SERVICE=new WorldShareService(FabricLoader.getInstance().getConfigDir().resolve("tflcore/sharing/runtime"));
    private OrnitheWorldSharing() { }
    public static WorldShareService service() { return SERVICE; }
    public static void tick(OrnitheClient c) { var server=c.mc.getServer(); SERVICE.tick(server!=null&&server.isRunning()?server:null); }
    public static int port(OrnitheClient c) {
        var s=c.mc.getServer(); if(s==null || !s.isPublished())return 0;
        var channels=((LanListenerAccess)s.getConnection()).tfl$lanChannels();
        synchronized(channels) {
            for(var future:channels) if(future.channel().isActive() && future.channel().localAddress() instanceof InetSocketAddress a) return a.getPort();
        }
        return 0;
    }
    public static void open(OrnitheClient c,Screen parent) {
        c.mc.openScreen(new CoreScreen(new WorldShareUi(SERVICE,new WorldShareUi.Host() {
            public Object server() { return c.mc.world==null ? null : c.mc.getServer(); }
            public int lanPort() { return port(c); }
            public boolean openLan() {
                var s=c.mc.getServer();
                return s!=null && (s.isPublished() || s.publish(s.getDefaultGameMode(),false)!=null);
            }
            public String playerName() { return c.mc.player==null ? "" : c.mc.player.getName(); }
            public void copy(String address) { Screen.setClipboard(address); }
            public void close() { c.mc.openScreen(parent); }
        }),false));
    }
}
