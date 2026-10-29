//? if >=26.3 {
/*package dev.theredstonee.trsclient.screen;
import dev.theredstonee.trsclient.core.ui.graphics.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;
// Uses the actual OptionInstance, ValueSet, formatter, validation and update listener.
public final class VanillaGraphics implements GraphicsUi.Host {
 private final VideoSettingsScreen source;private final List<GraphicsUi.Option> options=new ArrayList<>();
 private final Set<OptionInstance<?>> seen=Collections.newSetFromMap(new IdentityHashMap<>());
 private final Map<OptionInstance<?>,String> ids=new IdentityHashMap<>();private long tickBucket;
 public VanillaGraphics(VideoSettingsScreen source){this.source=source;var game=Minecraft.getInstance().options;
  for(Method method:Options.class.getDeclaredMethods())if(method.getParameterCount()==0&&method.getReturnType()==OptionInstance.class)try{method.setAccessible(true);ids.put((OptionInstance<?>)method.invoke(game),method.getName());}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
  for(String methodName:new String[]{"displayOptions","qualityOptions","preferenceOptions"})try{Method method=VideoSettingsScreen.class.getDeclaredMethod(methodName,Options.class);method.setAccessible(true);for(OptionInstance<?> option:(OptionInstance<?>[])method.invoke(null,game))add(option);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
  collect(source,Collections.newSetFromMap(new IdentityHashMap<>()));
  var c=dev.theredstonee.trsclient.TrsClient.get();TflGraphicsOptions.add(options,c.modules(),c::saveConfig,dev.theredstonee.trsclient.render.ColorPass.supported());
 }
 private void collect(Object node,Set<Object> visited){if(!visited.add(node))return;for(Class<?> t=node.getClass();t!=null;t=t.getSuperclass())for(Field f:t.getDeclaredFields())if(f.getType()==OptionInstance.class&&!Modifier.isStatic(f.getModifiers()))try{f.setAccessible(true);Object value=f.get(node);if(value!=null)add((OptionInstance<?>)value);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}if(node instanceof ContainerEventHandler container)for(Object child:container.children())collect(child,visited);}
 private void add(OptionInstance<?> option){if(option!=null&&seen.add(option))options.add(new Bound(option));}
 public List<GraphicsUi.Option> options(){long bucket=System.nanoTime()/50_000_000L;if(bucket!=tickBucket){tickBucket=bucket;source.tick();}return options;}
 public void apply(){Minecraft.getInstance().options.save();}
 public void close(){for(GraphicsUi.Option option:options)option.endSlide();source.removed();source.onClose();}
 public boolean hasShaders(){return false;}public void shaders(){}public String status(){return "";}
 private final class Bound implements GraphicsUi.Option {
  private double pendingFraction=Double.NaN;
  private final OptionInstance option;private final String id,name;private final Function<Object,Component> formatter;
  Bound(OptionInstance<?> option){this.option=option;name=((Component)field(option,"caption")).getString();id="minecraft:"+ids.getOrDefault(option,"display_control_"+options.size());formatter=(Function<Object,Component>)field(option,"toString");}
  public String id(){return id;}public String name(){return name;}public int category(){return GraphicsPurpose.category(id);}
  public String description(){Tooltip tooltip=((OptionInstance.TooltipSupplier)field(option,"tooltip")).apply(option.get());return tooltip==null?dev.theredstonee.trsclient.core.i18n.I18n.tr("graphics.immediate"):((Component)field(tooltip,"message")).getString();}
  public String value(){String message=formatter.apply(Double.isNaN(pendingFraction)?option.get():((OptionInstance.SliderableValueSet)option.values()).fromSliderValue(pendingFraction)).getString();int colon=message.indexOf(':');return colon<0?message:message.substring(colon+1).trim();}
  public boolean available(){OptionsList list=(OptionsList)field(source,"list");AbstractWidget widget=list==null?null:list.findOption(option);return widget==null||widget.active;}
  public boolean changed(){return false;}public void revert(){}public String backend(){return "Minecraft OptionInstance: "+id;}
  public boolean slider(){return option.values() instanceof OptionInstance.SliderableValueSet;}
  public double fraction(){return Double.isNaN(pendingFraction)?((OptionInstance.SliderableValueSet)option.values()).toSliderValue(option.get()):pendingFraction;}
  public void setFraction(double f){OptionInstance.SliderableValueSet values=(OptionInstance.SliderableValueSet)option.values();double clamped=Math.max(0,Math.min(1,f));if(values.applyValueImmediately())option.set(values.fromSliderValue(clamped));else pendingFraction=clamped;}
  public void endSlide(){if(!Double.isNaN(pendingFraction)){option.set(((OptionInstance.SliderableValueSet)option.values()).fromSliderValue(pendingFraction));pendingFraction=Double.NaN;}Minecraft.getInstance().options.save();}
  public void step(int direction){if(slider()){OptionInstance.SliderableValueSet values=(OptionInstance.SliderableValueSet)option.values();Optional<?> next=direction>0?values.next(option.get()):values.previous(option.get());if(next.isPresent())option.set(next.get());else{setFraction(fraction()+direction*.05);endSlide();}}else if(option.values() instanceof OptionInstance.CycleableValueSet values){List<?> list=values.valueListSupplier().getSelectedList();if(!list.isEmpty())values.valueSetter().set(option,list.get(Math.floorMod(list.indexOf(option.get())+direction,list.size())));}Minecraft.getInstance().options.save();}
 }
 private static Object field(Object o,String name){for(Class<?> t=o.getClass();t!=null;t=t.getSuperclass())try{Field f=t.getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(NoSuchFieldException ignored){}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}throw new IllegalStateException("Missing native field: "+name);}
}
*///?}
