package dev.theredstonee.trsclient.ornithe;
import dev.diorama.Projection;
import dev.theredstonee.trsclient.core.ui.*;
import dev.theredstonee.trsclient.core.waypoint.Waypoint;
/** Native TFL shell for the ported Diorama renderer. Coordinates use its oblique projection. */
public final class OrnitheDioramaUi extends UiScreen {
 private final OrnitheClient c;private final Runnable close;
 private double x,y,z,zoom=8,lastX,lastY;private int width,height;private boolean follow=true;private int layer=dev.diorama.bake.TileKey.SURFACE;
 public OrnitheDioramaUi(OrnitheClient c,Runnable close){this.c=c;this.close=close;x=c.mapPlatform.x();y=c.mapPlatform.y();z=c.mapPlatform.z();}
 protected void draw(Canvas canvas,int w,int h,int mx,int my,float dt){width=w;height=h;
  if(follow){x=c.mapPlatform.x();y=c.mapPlatform.y();z=c.mapPlatform.z();}
  double scale=c.window().getScale();double p=zoom/scale;
  c.diorama.draw(canvas,0,0,w,h,x,y,z,p,layer);
  Theme t=Theme.get();SelectionStyle.panel(canvas,8,8,Math.min(w-16,310),28,0xC0101828,t.border);
  canvas.strongText("TFL Maps",17,17,t.text);
  control(canvas,100,13,46,"Center",()->{follow=true;});control(canvas,151,13,24,"−",()->zoom=Math.max(.125,zoom/1.4));control(canvas,180,13,24,"+",()->zoom=Math.min(16,zoom*1.4));control(canvas,209,13,55,"Settings",()->c.mc.openScreen(new CoreScreen(new dev.theredstonee.trsclient.core.ui.menu.ModMenu(new OrnitheMenuHost(c,c.mc.screen)).select(c.modules.worldMap),false)));control(canvas,w-62,13,50,"Close",()->requestClose());
  control(canvas,12,44,58,layer==dev.diorama.bake.TileKey.SURFACE?"Surface":"Caves",()->toggleLayer());
  if(layer!=dev.diorama.bake.TileKey.SURFACE){control(canvas,75,44,24,"−",()->layer=Math.max(8,layer-8));control(canvas,104,44,24,"+",()->layer=Math.min(255,layer+8));}
  if(c.modules.worldMapWaypoints.get())for(Waypoint point:c.waypoints.visible(c.mapPlatform.worldKey(),c.mapPlatform.dimension())){
   int px=(int)Math.round(w/2+(point.x-x)*p),py=(int)Math.round(h/2+(Projection.v(point.y,point.z)-Projection.v(y,z))*p);
   if(px<0||py<0||px>=w||py>=h)continue;
   SelectionStyle.rounded(canvas,px-3,py-3,7,7,3,point.color|0xFF000000);canvas.text(point.name,px+6,py-4,t.text,true);hits.add(px-5,py-5,Math.max(10,canvas.textWidth(point.name)+14),12,()->edit(point,false));
  }
  int playerX=(int)Math.round(w/2+(c.mapPlatform.x()-x)*p),playerY=(int)Math.round(h/2+(Projection.v(c.mapPlatform.y(),c.mapPlatform.z())-Projection.v(y,z))*p);
  SelectionStyle.rounded(canvas,playerX-3,playerY-3,7,7,3,t.accent);
  double worldX=x+(mx-w/2)/p,worldZ=Projection.zAt(Projection.v(y,z)+(my-h/2)/p,y);
  String status=String.format(java.util.Locale.ROOT,"X %.0f  Z %.0f   ·   %.2f×   ·   %d chunks   ·   Right click: waypoint",worldX,worldZ,zoom,c.diorama.snapshotCount());
  SelectionStyle.panel(canvas,8,h-24,Math.min(w-16,canvas.textWidth(status)+16),18,0xC0101828,t.border);canvas.text(canvas.clip(status,w-32),16,h-19,t.text,false);
  if(!c.diorama.failure.isEmpty())canvas.text(canvas.clip(c.diorama.failure,w-20),10,h-38,0xFFFFA0A0,true);
 }
 private void control(Canvas canvas,int x,int y,int w,String label,Runnable action){SelectionStyle.button(canvas,x,y,w,18,false,false);canvas.text(label,x+5,y+5,Theme.get().text,false);hits.add(x,y,w,18,action);}
 public boolean mouseClicked(double mx,double my,int button){if(super.mouseClicked(mx,my,button))return true;lastX=mx;lastY=my;
  if(button==1){double p=zoom/c.window().getScale();int wx=(int)Math.floor(x+(mx-width/2)/p),wz=(int)Math.floor(Projection.zAt(Projection.v(y,z)+(my-height/2)/p,y));var picked=c.diorama.pick(x+(mx-width/2)/p,Projection.v(y,z)+(my-height/2)/p,layer);
   if(picked!=null){Waypoint point=new Waypoint("Waypoint",picked.getX(),picked.getY(),picked.getZ(),c.mapPlatform.dimension(),0x3B82F6);edit(point,true);}return true;}return button==0;
 }
 public boolean mouseDragged(double mx,double my,int button){if(button!=0)return false;double p=zoom/c.window().getScale();x-=(mx-lastX)/p;z-=(my-lastY)/p/Projection.sin();lastX=mx;lastY=my;follow=false;return true;}
 public boolean mouseScrolled(double mx,double my,double delta){double old=zoom/c.window().getScale(),next=Math.max(.125,Math.min(16,zoom*Math.pow(1.2,delta)))/c.window().getScale();double anchorX=x+(mx-width/2)/old,anchorV=Projection.v(y,z)+(my-height/2)/old;zoom=next*c.window().getScale();x=anchorX-(mx-width/2)/next;z=Projection.zAt(anchorV-(my-height/2)/next,y);follow=false;return true;}
 public boolean keyPressed(int raw,UiKey key,boolean shift){if(key==UiKey.ESCAPE||raw==c.mapKey.getKeyCode()){requestClose();return true;}if(raw==org.lwjgl.input.Keyboard.KEY_C){toggleLayer();return true;}if(raw==org.lwjgl.input.Keyboard.KEY_PRIOR&&layer!=dev.diorama.bake.TileKey.SURFACE){layer=Math.min(255,layer+8);return true;}if(raw==org.lwjgl.input.Keyboard.KEY_NEXT&&layer!=dev.diorama.bake.TileKey.SURFACE){layer=Math.max(8,layer-8);return true;}if(raw==org.lwjgl.input.Keyboard.KEY_SPACE){follow=true;return true;}return false;}
 private void toggleLayer(){if(!c.map.fairPlay().caveAllowed(c.mapPlatform.dimension().equals("dim-1")))return;layer=layer==dev.diorama.bake.TileKey.SURFACE?Math.min(255,(int)y+16):dev.diorama.bake.TileKey.SURFACE;}
 private void edit(Waypoint point,boolean fresh){var parent=c.mc.screen;c.mc.openScreen(new CoreScreen(new OrnitheWaypointUi(c,point,fresh,()->c.mc.openScreen(parent)),false));}
 protected void onClosed(){close.run();}
 public boolean pausesGame(){return false;}
}
