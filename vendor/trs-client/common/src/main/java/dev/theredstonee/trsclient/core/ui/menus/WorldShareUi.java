package dev.theredstonee.trsclient.core.ui.menus;

import dev.theredstonee.trsclient.core.hosting.e4mc.WorldShareService;
import dev.theredstonee.trsclient.core.i18n.I18n;
import dev.theredstonee.trsclient.core.ui.*;

/** Presentation only; the share session outlives this screen, never the integrated server. */
public final class WorldShareUi extends WindowUi {
    public interface Host {
        Object server();
        int lanPort();
        boolean openLan();
        String playerName();
        void copy(String address);
        void close();
    }
    private final Host host;
    private final WorldShareService service;
    private WorldShareService.Consent consent=new WorldShareService.Consent();
    private final TextInput broker=new TextInput(512);
    private boolean checked;
    private String problem="";
    private int scroll,maxScroll;
    public WorldShareUi(WorldShareService service,Host host) {
        this.service=service; this.host=host; broker.setText(WorldShareService.COMMUNITY_BROKER);
    }
    protected String title() { return I18n.tr("share.title"); }
    protected void playClick() { }
    protected void onClosed() { host.close(); }
    public boolean pausesGame() { return false; }
    protected void content(Canvas c,int x,int y,int w,int h,int mx,int my,float dt) {
        c.scissor(x,y,x+w,y+h); hits.clip(x,y,w,h);
        int top=y-scroll, p=top;
        WorldShareService.Snapshot state=service.snapshot();
        int port=host.lanPort();
        p=paragraph(c,I18n.tr("share.lanNotice"),x,p,w,Theme.get().textDim)+7;
        button(c,x,p,w,23,I18n.tr(port>0 ? "share.lanOpen" : "share.openLan"),false,host.server()!=null && port<=0,mx,my,()->{
            if(!host.openLan()) problem="lan";
        }); p+=30;
        c.text(I18n.tr("share.state."+state.state.name().toLowerCase(java.util.Locale.ROOT))
            +(state.state==WorldShareService.State.PREPARING ? " "+state.progress+"%" : ""),x,p,Theme.get().text,false); p+=18;
        if(!state.address.isEmpty()) { c.text(c.clip(state.address,w),x,p,Theme.get().accent,false); p+=18; }
        String error=!problem.isEmpty()?problem:state.error;
        if(!error.isEmpty()) p=paragraph(c,I18n.tr("share.error."+error),x,p,w,Theme.get().textDim)+6;
        boolean active=state.state==WorldShareService.State.ONLINE || state.state==WorldShareService.State.CONNECTING || state.state==WorldShareService.State.PREPARING;
        if(active) {
            button(c,x,p,w,23,I18n.tr("share.copy"),true,!state.address.isEmpty(),mx,my,()->host.copy(service.snapshot().address)); p+=29;
            button(c,x,p,w,23,I18n.tr("share.stop"),false,true,mx,my,()->{service.stop(); checked=false; consent=new WorldShareService.Consent();}); p+=29;
        } else {
            p=paragraph(c,I18n.tr("share.relayNotice"),x,p,w,Theme.get().textDim)+6;
            c.text(I18n.tr("share.broker"),x,p,Theme.get().text,false); p+=15;
            new dev.theredstonee.trsclient.core.ui.social.Kit(hits,this::playClick).input(c,broker,x,p,w,23,"https://",null); p+=30;
            final int checkY=p;
            MenuSkin.button(c,x,p,w,25,inside(mx,my,x,p,w,25),true);
            c.text((checked?"[x] ":"[ ] ")+c.clip(I18n.tr("share.consent"),w-35),x+7,p+8,Theme.get().text,false);
            hits.add(x,checkY,w,25,()->{checked=!checked;consent.understood(checked);broker.setFocused(false);}); p+=32;
            button(c,x,p,w,23,I18n.tr("share.start"),true,consent.ready() && port>0,mx,my,()->{
                broker.setFocused(false);
                if(service.start(host.server(),host.lanPort(),host.playerName(),broker.text().trim(),consent)) problem="";
                else {problem="start";checked=false;consent=new WorldShareService.Consent();}
            }); p+=29;
        }
        maxScroll=Math.max(0,p-top-h); scroll=Math.min(scroll,maxScroll);
        scrollbar(c,x+w-2,y,h,scroll,maxScroll,p-top);
        hits.noClip(); c.noScissor();
    }
    private int paragraph(Canvas c,String text,int x,int y,int width,int color) {
        return Paint.paragraph(c,text,x,y,width,c.lineHeight()+3,color);
    }
    public boolean mouseScrolled(double x,double y,double amount) { scroll=Math.max(0,Math.min(maxScroll,scroll-(int)(amount*24))); return true; }
    public boolean keyPressed(int raw,UiKey key,boolean shift) {
        if(key==UiKey.ESCAPE)return super.keyPressed(raw,key,shift);
        return broker.focused() && broker.key(key);
    }
    public boolean charTyped(char c) { return broker.focused() && broker.type(c); }
    public boolean wantsTextInput() { return broker.focused(); }
}
