package dev.theredstonee.trsclient.core.ui.clips;
import dev.theredstonee.trsclient.core.ui.*;
import dev.theredstonee.trsclient.core.ui.menus.WindowUi;
import dev.theredstonee.trsclient.core.clips.LocalClipTools;
import java.nio.file.Path;
/** Shared, reversible local clip editing. The export works without a launcher service. */
public final class ClipEditorUi extends WindowUi {
 private final Path input;private final Runnable close;private final LocalClipTools tools=new LocalClipTools();
 private final TextInput start=new TextInput(20),end=new TextInput(20);
 private final LocalClipTools.Edit edit;
 private volatile boolean exporting;private volatile String status="FFmpeg must be installed locally. Your original is preserved.";
 public ClipEditorUi(Path input,long durationMs,Runnable close){this.input=input;this.close=close;edit=new LocalClipTools.Edit(durationMs>0?durationMs/1000.0:30);start.setText("0");end.setText(String.valueOf(edit.end));}
 protected String title(){return "Clip editor";}protected void playClick(){}
 protected void content(Canvas c,int x,int y,int w,int h,int mx,int my,float dt){
  Theme t=Theme.get();c.text(c.clip(input.getFileName().toString(),w),x,y,t.text,false);
  c.text("Start (seconds)",x,y+24,t.textDim,false);c.text("End (seconds)",x+w/2,y+24,t.textDim,false);
  field(c,start,x,y+38,w/2-8);field(c,end,x+w/2,y+38,w/2-8);
  hits.add(x,y+38,w/2-8,20,()->{start.setFocused(true);end.setFocused(false);});hits.add(x+w/2,y+38,w/2-8,20,()->{start.setFocused(false);end.setFocused(true);});
  button(c,x,y+72,w/2-5,22,edit.mute?"Audio: muted":"Audio: original",false,!exporting,mx,my,()->edit.mute=!edit.mute);
  button(c,x+w/2,y+72,w/2-5,22,"Speed: "+edit.speed+"×",false,!exporting,mx,my,()->edit.speed=edit.speed==1?2:edit.speed==2?.5:1);
  button(c,x,y+101,w/2-5,22,edit.portrait?"Crop: portrait 9:16":"Crop: original",false,!exporting,mx,my,()->edit.portrait=!edit.portrait);
  button(c,x+w/2,y+101,w/2-5,22,edit.rotate?"Rotate: 90°":"Rotate: original",false,!exporting,mx,my,()->edit.rotate=!edit.rotate);
  button(c,x,y+134,Math.min(150,w),24,exporting?"Exporting…":"Export new MP4",true,!exporting,mx,my,this::export);
  int top=y+168;for(String line:Paint.wrap(c,status,w)){if(top>y+h-10)break;c.text(line,x,top,t.textDim,false);top+=12;}
 }
 private void field(Canvas c,TextInput input,int x,int y,int w){dev.theredstonee.trsclient.core.ui.menus.MenuSkin.textField(c,x,y,w,20,input.focused(),false,true);c.text(c.clip(input.text()+(input.focused()?"|":""),w-10),x+5,y+6,Theme.get().text,false);}
 private void export(){try{edit.start=Double.parseDouble(start.text());edit.end=Double.parseDouble(end.text());edit.validate();}catch(IllegalArgumentException e){status="Enter a valid start and end in seconds.";return;}
  exporting=true;status="Exporting a new file…";Thread worker=new Thread(()->{try{status="Saved: "+tools.export(input,edit).getFileName();}catch(Exception e){status="Export failed: "+e.getMessage();}finally{exporting=false;}},"TFL-clip-export");worker.setDaemon(true);worker.start();
 }
 public boolean charTyped(char ch){return start.focused()?start.type(ch):end.focused()&&end.type(ch);}
 public boolean keyPressed(int raw,UiKey key,boolean shift){if(key==UiKey.TAB){boolean first=start.focused();start.setFocused(!first);end.setFocused(first);return true;}if((start.focused()&&start.key(key))||(end.focused()&&end.key(key)))return true;return super.keyPressed(raw,key,shift);}
 public boolean wantsTextInput(){return start.focused()||end.focused();}
 public void removed(){tools.close();}
 protected void onClosed(){tools.close();close.run();}
}
