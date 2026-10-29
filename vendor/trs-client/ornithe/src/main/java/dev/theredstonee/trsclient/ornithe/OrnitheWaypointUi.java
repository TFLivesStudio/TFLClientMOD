package dev.theredstonee.trsclient.ornithe;
import dev.theredstonee.trsclient.core.ui.*;
import dev.theredstonee.trsclient.core.ui.menus.*;
import dev.theredstonee.trsclient.core.waypoint.Waypoint;
final class OrnitheWaypointUi extends WindowUi {
 private final OrnitheClient c;private final Waypoint point;private final boolean fresh;private final Runnable close;
 private final TextInput[] fields={new TextInput(32),new TextInput(12),new TextInput(12),new TextInput(12)};private String error="";private int selectedColor;
 OrnitheWaypointUi(OrnitheClient c,Waypoint point,boolean fresh,Runnable close){this.c=c;this.point=point;this.fresh=fresh;this.close=close;selectedColor=point.color;fields[0].setText(point.name);fields[1].setText(""+point.x);fields[2].setText(""+point.y);fields[3].setText(""+point.z);fields[0].setFocused(true);}
 protected String title(){return "Waypoint";}protected void playClick(){c.clickSound();}
 protected void content(Canvas canvas,int x,int y,int w,int h,int mx,int my,float dt){String[] names={"Name","X","Y","Z"};for(int i=0;i<4;i++){final int field=i;int top=y+i*30;canvas.text(names[i],x,top+5,Theme.get().textDim,false);MenuSkin.textField(canvas,x+45,top,w-50,22,fields[i].focused(),false,true);canvas.text(fields[i].text()+(fields[i].focused()?"|":""),x+51,top+6,Theme.get().text,false);hits.add(x+45,top,w-50,22,()->{for(int j=0;j<4;j++)fields[j].setFocused(j==field);});}
  button(canvas,x,y+128,80,22,"Save",true,true,mx,my,()->{try{int wx=Integer.parseInt(fields[1].text()),wy=Integer.parseInt(fields[2].text()),wz=Integer.parseInt(fields[3].text());if(Math.abs((long)wx)>30000000||Math.abs((long)wz)>30000000||wy<0||wy>255)throw new NumberFormatException();point.color=selectedColor;point.name=fields[0].text();point.x=wx;point.y=wy;point.z=wz;if(fresh)c.waypoints.add(c.mapPlatform.worldKey(),point);else c.waypoints.touch();c.mapPlatform.waypointsChanged();requestClose();}catch(NumberFormatException bad){error="Enter coordinates within the world.";}});
  if(!fresh)button(canvas,x+86,y+128,80,22,"Delete",false,true,mx,my,()->{c.waypoints.remove(c.mapPlatform.worldKey(),point);c.mapPlatform.waypointsChanged();requestClose();});
  button(canvas,x+172,y+128,80,22,"Color",false,true,mx,my,()->{int[] colors={0x3b82f6,0xf43f5e,0x22c55e,0xf59e0b,0xa78bfa};int next=0;for(int i=0;i<colors.length;i++)if(selectedColor==colors[i])next=(i+1)%colors.length;selectedColor=colors[next];});SelectionStyle.rounded(canvas,x+258,y+131,14,14,4,selectedColor|0xff000000);canvas.text(error,x,y+158,Theme.get().textDim,false);
 }
 public boolean charTyped(char value){for(TextInput f:fields)if(f.focused())return f.type(value);return false;}
 public boolean keyPressed(int raw,UiKey key,boolean shift){if(key==UiKey.TAB){int next=0;for(int i=0;i<4;i++)if(fields[i].focused())next=(i+(shift?3:1))%4;for(int i=0;i<4;i++)fields[i].setFocused(i==next);return true;}for(TextInput f:fields)if(f.focused()&&f.key(key))return true;return super.keyPressed(raw,key,shift);}
 public boolean wantsTextInput(){return true;}protected void onClosed(){close.run();}
}
