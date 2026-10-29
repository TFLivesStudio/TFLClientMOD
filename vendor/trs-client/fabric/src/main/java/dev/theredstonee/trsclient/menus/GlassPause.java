package dev.theredstonee.trsclient.menus;

import dev.theredstonee.trsclient.compat.Mc;
import net.minecraft.client.gui.screens.Screen;

/** Keep the native disconnect/options callbacks while sharing the 1.8.9 pause presentation. */
public final class GlassPause {
    private GlassPause() {}
    public static void open(final Screen parent) {
        //? if >=26.3 {
        /*final net.minecraft.client.gui.components.Button options=find(parent,"menu.options",0);
        net.minecraft.client.gui.components.Button exit=find(parent,"menu.returnToMenu",0);
        if(exit==null)exit=find(parent,"menu.disconnect",0);
        if(options==null||exit==null)return;
        final net.minecraft.client.gui.components.Button disconnect=exit;
        final java.util.List<dev.theredstonee.trsclient.core.ui.menu.MenuAction> rows=new java.util.ArrayList<>();
        rows.add(new dev.theredstonee.trsclient.core.ui.menu.MenuAction(dev.theredstonee.trsclient.core.i18n.I18n.tr("pause.resume"),"play",()->Mc.setScreen(null)));
        rows.add(new dev.theredstonee.trsclient.core.ui.menu.MenuAction("TFL Client","settings",()->VanillaMenus.openFromPause(Mc.screen(),"core")));
        rows.add(new dev.theredstonee.trsclient.core.ui.menu.MenuAction(dev.theredstonee.trsclient.core.i18n.I18n.tr("pause.options"),"settings",()->options.onPress(new net.minecraft.client.input.KeyEvent(257,0,0))));
        rows.add(new dev.theredstonee.trsclient.core.ui.menu.MenuAction(dev.theredstonee.trsclient.core.i18n.I18n.tr("pause.captures"),"image",()->VanillaMenus.openFromPause(Mc.screen(),"clips")));
        rows.add(new dev.theredstonee.trsclient.core.ui.menu.MenuAction(dev.theredstonee.trsclient.core.i18n.I18n.tr(Mc.mc().hasSingleplayerServer()?"pause.singleplayerWorldInformation":"pause.serverInformation"),"info",()->VanillaMenus.openFromPause(Mc.screen(),"serverInfo")));
        rows.add(new dev.theredstonee.trsclient.core.ui.menu.MenuAction(dev.theredstonee.trsclient.core.i18n.I18n.tr("pause.diagnostics"),"activity",()->Mc.setScreen(new dev.theredstonee.trsclient.screen.TrsUiScreen("Diagnostics",new dev.theredstonee.trsclient.core.ui.menus.ActionListUi(dev.theredstonee.trsclient.core.i18n.I18n.tr("pause.diagnostics"),"Minecraft 26.3 · TFL Client · "+dev.theredstonee.trsclient.TrsClient.get().modules().registry.all().size()+" shared modules. Open Client settings to configure each module.",()->Mc.setScreen(parent),()->{})))));
        rows.add(new dev.theredstonee.trsclient.core.ui.menu.MenuAction(Mc.mc().hasSingleplayerServer()?dev.theredstonee.trsclient.core.i18n.I18n.tr("pause.saveQuit"):dev.theredstonee.trsclient.core.i18n.I18n.tr("pause.disconnect"),"back",()->disconnect.onPress(new net.minecraft.client.input.KeyEvent(257,0,0))));
        // Preserve version-specific native actions (statistics, advancements, LAN, etc.).
        final java.util.List<net.minecraft.client.gui.components.Button> extras=new java.util.ArrayList<>();
        collect(parent,extras,0);
        final String resume=net.minecraft.network.chat.Component.translatable("menu.returnToGame").getString();
        for(final net.minecraft.client.gui.components.Button button:extras) {
            if(button==options||button==disconnect||button.getMessage().getString().equals(resume))continue;
            rows.add(new dev.theredstonee.trsclient.core.ui.menu.MenuAction(button.getMessage().getString(),"settings",()->button.onPress(new net.minecraft.client.input.KeyEvent(257,0,0))));
        }
        final Screen next=new dev.theredstonee.trsclient.screen.TrsUiScreen(dev.theredstonee.trsclient.core.i18n.I18n.tr("pause.title"),new dev.theredstonee.trsclient.core.ui.menus.ActionListUi(dev.theredstonee.trsclient.core.i18n.I18n.tr("pause.title"),rows,()->Mc.setScreen(null),()->{}).navigationRail()) {
            @Override protected boolean customBackground(){return true;}
        };
        Mc.mc().execute(()->{if(Mc.screen()==parent)Mc.setScreen(next);});
        *///?}
    }
    //? if >=26.3 {
    /*private static void collect(net.minecraft.client.gui.components.events.ContainerEventHandler root,java.util.List<net.minecraft.client.gui.components.Button> out,int depth){
        if(depth>8)return;
        for(Object child:root.children()){
            if(child instanceof net.minecraft.client.gui.components.Button && ((net.minecraft.client.gui.components.Button)child).active)out.add((net.minecraft.client.gui.components.Button)child);
            if(child instanceof net.minecraft.client.gui.components.events.ContainerEventHandler)collect((net.minecraft.client.gui.components.events.ContainerEventHandler)child,out,depth+1);
        }
    }
    private static net.minecraft.client.gui.components.Button find(net.minecraft.client.gui.components.events.ContainerEventHandler root,String key,int depth){
        if(depth>8)return null;
        String label=net.minecraft.network.chat.Component.translatable(key).getString();
        for(Object child:root.children()){
            if(child instanceof net.minecraft.client.gui.components.Button && ((net.minecraft.client.gui.components.Button)child).getMessage().getString().equals(label))return (net.minecraft.client.gui.components.Button)child;
            if(child instanceof net.minecraft.client.gui.components.events.ContainerEventHandler){var result=find((net.minecraft.client.gui.components.events.ContainerEventHandler)child,key,depth+1);if(result!=null)return result;}
        }
        return null;
    }
    *///?}
}
