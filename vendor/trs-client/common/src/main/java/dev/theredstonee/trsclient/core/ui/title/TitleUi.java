package dev.theredstonee.trsclient.core.ui.title;

import dev.theredstonee.trsclient.core.i18n.I18n;
import dev.theredstonee.trsclient.core.intro.IntroGate;
import dev.theredstonee.trsclient.core.module.TrsModules;
import dev.theredstonee.trsclient.core.ui.*;
import dev.theredstonee.trsclient.core.ui.background.MenuBackground;
import java.util.ArrayList;
import java.util.List;

/** TFL native home. Only working local destinations are part of the focus order. */
public final class TitleUi extends UiScreen {
    private static final class Entry {
        final String id, label; final Runnable action;
        int x,y,w,h;
        Entry(String id,Runnable action) { this.id=id;this.label=I18n.tr("title."+id);this.action=action; }
    }
    private final TitleHost host;
    private final List<Entry> entries=new ArrayList<Entry>();
    private int focus=-1,pending=-1,frames;
    private float delay,frameMs,uiMicros;
    private boolean sceneEnabled=true;
    public TitleUi(final TitleHost host) {
        this.host=host; I18n.refresh();
        entries.add(new Entry("singleplayer",()->host.singleplayer()));
        entries.add(new Entry("multiplayer",()->host.multiplayer()));
        entries.add(new Entry("options",()->host.options()));
        entries.add(new Entry("trsMenu",()->host.trsMenu()));
        if(host.hasMods())entries.add(new Entry("mods",()->host.mods()));
        entries.add(new Entry("quit",()->host.quit()));
    }
    @Override protected void draw(Canvas raw,int width,int height,int mx,int my,float dt) {
        long start=System.nanoTime(); Theme t=Theme.get(); Canvas c=FadeCanvas.of(raw,alpha());
        TrsModules m=IntroGate.modules();
        if(m!=null)MenuBackground.shared().setMode(host.animated()?m.menuBackground.get():MenuBackground.Mode.STATIC);
        MenuBackground.shared().draw(FadeCanvas.unwrap(raw),width,height,sceneEnabled);
        int left=Math.max(12,Math.min(48,width/14));
        int rail=Math.min(180,Math.max(80,width/2-left));
        // The transparent rail leaves the event horizon visible and keeps navigation quiet.
        for(int x=0;x<width/2;x+=4) {
            int a=Math.max(0,185-x*370/Math.max(1,width));
            c.fill(x,0,Math.min(x+4,width/2),height,(a<<24)|0x060B14);
        }
        boolean compact=height<190;
        int row=Math.min(28,Math.max(11,(height-(compact?34:100))/entries.size()));
        int top=compact?25:Math.max(70,(height-row*entries.size())/2);
        int headingY=compact?7:Math.max(18,top-46);
        c.push();c.translate(left,headingY);c.scale(compact?1.15f:1.9f);
        c.strongText("TFL Client",0,0,t.text);c.pop();
        if(!compact)c.text("0.4.0-dev.3",left,headingY+26,t.textDim,false);
        for(int i=0;i<entries.size();i++) {
            Entry e=entries.get(i);e.x=left;e.y=top+i*row;e.w=rail;e.h=Math.max(10,row-3);
            boolean selected=focus==i||inside(mx,my,e);
            // One play action; remaining destinations are text navigation, never a stack of large cards.
            if(i==0) SelectionStyle.button(c,e.x,e.y,e.w,e.h,true,selected);
            else if(selected) {
                SelectionStyle.rounded(c,e.x,e.y,e.w,e.h,4,0x64344B69);
                c.fill(e.x,e.y+3,e.x+2,e.y+e.h-3,t.accent);
            }
            String label="trsMenu".equals(e.id)?I18n.trOr("title.clientSettings","Client settings"):e.label;
            c.text(c.clip(label,e.w-18),e.x+9,e.y+(e.h-c.lineHeight())/2,selected||i==0?t.text:t.textDim,false);
            final int index=i;hits.add(e.x,e.y,e.w,e.h,()->activate(index));
        }
        if(!compact)c.text(c.clip(host.versionLine(),Math.max(0,width-24)),12,height-13,t.textDim,false);
        if(pending>=0) {delay-=dt;if(delay<=0){int index=pending;pending=-1;entries.get(index).action.run();}}
        frames++;frameMs=dt*1000;float us=(System.nanoTime()-start)/1000f;uiMicros=frames==1?us:uiMicros+(us-uiMicros)*0.05f;
    }
    private static boolean inside(double x,double y,Entry e){return x>=e.x&&x<e.x+e.w&&y>=e.y&&y<e.y+e.h;}
    private void activate(int index){if(index<0||index>=entries.size()||pending>=0)return;host.playClick();pending=index;delay=.07f;}
    public void focus(int index){focus=index>=0&&index<entries.size()?index:-1;}
    public int[] buttonRect(String id){if(frames==0)return null;for(Entry e:entries)if(e.id.equals(id)||e.label.equals(id))return new int[]{e.x,e.y,e.w,e.h};return null;}
    @Override public boolean keyPressed(int raw,UiKey key,boolean shift){
        int step;
        switch(key){case TAB:step=shift?-1:1;break;case DOWN:case RIGHT:step=1;break;case UP:case LEFT:step=-1;break;
            case ENTER:if(focus<0)return false;activate(focus);return true;default:return false;}
        focus=focus<0?(step>0?0:entries.size()-1):(focus+step+entries.size())%entries.size();
        host.narrate(I18n.tr("title.narrateButton",entries.get(focus).label));return true;
    }
    @Override public boolean mouseClicked(double x,double y,int button){boolean hit=super.mouseClicked(x,y,button);if(!hit)focus=-1;return hit;}
    @Override public boolean wantsTextInput(){return false;}
    @Override public void requestClose(){}
    @Override protected void onClosed(){}
    public void setSceneEnabled(boolean enabled){sceneEnabled=enabled;}
    public float frameMicros(){return uiMicros;}
    public float frameMillis(){return frameMs;}
    // Compatibility for the upstream diagnostic harness; the home has no player model or social rail.
    public int sideMode(){return 0;}
    public void setFigureAngle(float degrees){}
    public boolean figureShown(){return false;}
    public float modelMicros(){return 0;}
    public int modelFaces(){return 0;}
    public String toast(){return null;}
    public float sceneMicros(){return 0;}
    public boolean sparseScene(){return host.simpleAnimation();}
}
