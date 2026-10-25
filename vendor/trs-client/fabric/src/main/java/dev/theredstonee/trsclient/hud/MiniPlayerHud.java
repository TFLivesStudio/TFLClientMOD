//? if >=26.3 {
/*package dev.theredstonee.trsclient.hud;
import dev.theredstonee.trsclient.core.hud.miniplayer.*;
import dev.theredstonee.trsclient.ui.Gfx;
import dev.theredstonee.trsclient.mixin.MiniPlayerGraphicsAccessor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Quaternionf;
// Extracts the real local player's native render state without changing any entity or camera field.
public final class MiniPlayerHud extends HudElement {
 public final MiniPlayerState state=new MiniPlayerState();
 private final MiniPlayerModule settings;
 private EntityRenderState cached;
 private Object world,player;private int resourceGeneration=-1;
 public long composites;
 public MiniPlayerHud(MiniPlayerModule m){super(m);settings=m;}
 public int width(Font f,boolean p){return 44;}public int height(Font f,boolean p){return 74;}
 public void invalidate(){cached=null;world=player=null;state.invalidate();MiniPlayerRenderer.releaseCached();}
 public void draw(Gfx g,Font font,boolean preview){
  var p=mc.player;
  if(p==null){invalidate();return;}
  int generation=dev.theredstonee.trsclient.core.ui.Textures.store().resourceGeneration();
  if(world!=mc.level||player!=p||resourceGeneration!=generation){invalidate();world=mc.level;player=p;resourceGeneration=generation;}
  var graphics=(GuiGraphicsExtractor)g.raw();
  Vector2f a=graphics.pose().transformPosition(0,0,new Vector2f()),b=graphics.pose().transformPosition(44,74,new Vector2f());
  boolean onScreen=b.x>0&&b.y>0&&a.x<g.width()&&a.y<g.height();
  boolean moving=p.getDeltaMovement().lengthSqr()>.00001||p.isSwinging()||!p.onGround();
  // Keep the native state fresh for breathing, cape and emote animation as well as locomotion.
  if(!settings.shouldDraw(preview,true,moving,dev.theredstonee.trsclient.compat.Mc.hudHidden(),dev.theredstonee.trsclient.compat.Mc.screen()!=null,onScreen))return;
  long key=MiniPlayerState.optionsKey(settings);key=key*31+p.getSkin().hashCode();
  boolean update=state.update(mc.level,p,key,true,System.nanoTime(),settings);
  if(cached==null||update){
   long t=System.nanoTime();
   EntityRenderer renderer=mc.getEntityRenderDispatcher().getRenderer(p);
   cached=renderer.createRenderState(p,settings.smoothing.get()?mc.getDeltaTracker().getGameTimeDeltaPartialTick(false):1f);
   cached.shadowPieces.clear();cached.outlineColor=0;
   if(cached instanceof LivingEntityRenderState living){living.bodyRot=180+state.yaw();}
   if(cached instanceof HumanoidRenderState humanoid&&!settings.armor.get())humanoid.headEquipment=humanoid.chestEquipment=humanoid.legsEquipment=humanoid.feetEquipment=ItemStack.EMPTY;
   if(cached instanceof ArmedEntityRenderState armed&&!settings.heldItems.get()){armed.leftHandItemState.clear();armed.rightHandItemState.clear();armed.leftHandItemStack=armed.rightHandItemStack=ItemStack.EMPTY;}
   if(cached instanceof AvatarRenderState avatar&&!settings.cape.get())avatar.showCape=false;
   state.nanos+=System.nanoTime()-t;
  }
  if(settings.background.get())g.fill(0,0,44,74,settings.backgroundArgb());
  float h=Math.max(.5f,p.getBbHeight()),scale=Math.min((b.y-a.y)*.8f/h,(b.x-a.x)*.7f);
  var entity=new GuiEntityRenderState(cached,new Vector3f(0,h/2,0),new Quaternionf().rotateZ((float)Math.PI),null,
    Math.round(a.x),Math.round(a.y),Math.round(b.x),Math.round(b.y),scale,null);
  ((MiniPlayerGraphicsAccessor)graphics).tfl$miniGuiState().addPicturesInPictureState(new MiniPlayerFrame(entity,state.serial(),settings.opacity.getFloat()/100));composites++;
 }
}
*///?}
