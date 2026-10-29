package dev.theredstonee.trsclient.core.ui.graphics;
import dev.theredstonee.trsclient.core.ui.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GraphicsUiTest {
 static final class Model implements GraphicsUi.Option {
  final String id,name;final int category;boolean enabled=true,value,saved;
  Model(String id,String name,int category){this.id=id;this.name=name;this.category=category;}
  public String id(){return id;}public String name(){return name;}public String description(){return "Native option";}public String value(){return value?"On":"Off";}public int category(){return category;}public boolean available(){return enabled;}public boolean changed(){return value!=saved;}public void step(int direction){value=!value;}public void revert(){value=saved;}
 }
 static final class Host implements GraphicsUi.Host {
  final Model display=new Model("minecraft:brightness","Brightness",0),scaler=new Model("amplio:upscaler","Upscaler",3);int applied;
  public List<GraphicsUi.Option> options(){return Arrays.<GraphicsUi.Option>asList(display,scaler);}
  public void apply(){display.saved=display.value;scaler.saved=scaler.value;applied++;}public void close(){}public void shaders(){}public boolean hasShaders(){return false;}public String status(){return "";}
 }
 static final class Draw implements Canvas {
  final List<String> text=new ArrayList<>();
  public void raise(float z){}public void push(){}public void pop(){}public void translate(float x,float y){}public void scale(float f){}
  public void fill(int a,int b,int c,int d,int color){}public void text(String s,int x,int y,int color,boolean shadow){text.add(s);}public int textWidth(String s){return s.length()*6;}public int lineHeight(){return 9;}public String clip(String s,int max){return s.substring(0,Math.min(s.length(),Math.max(0,max/6)));}public void flush(){}public void scissor(int a,int b,int c,int d){}public void noScissor(){}
 }
 @Test void keyboardEditsAndAppliesTheProvidersOwnModel(){
  Host host=new Host();GraphicsUi ui=new GraphicsUi(host);ui.render(new Draw(),480,270,-1,-1);
  ui.keyPressed(0,UiKey.ENTER,false);ui.keyPressed(0,UiKey.RIGHT,false);assertTrue(host.display.changed());
  ui.keyPressed(0,UiKey.ESCAPE,false);assertTrue(host.display.changed(),"pending change is retained until apply or undo");
  ui.keyPressed(0,UiKey.TAB,false);ui.keyPressed(0,UiKey.ENTER,false);assertEquals(1,host.applied);assertFalse(host.display.changed());assertTrue(host.display.value);
 }
 @Test void unavailableProviderOptionCannotBeChangedByKeyboard(){
  Host host=new Host();host.display.enabled=false;GraphicsUi ui=new GraphicsUi(host);ui.render(new Draw(),480,270,-1,-1);ui.keyPressed(0,UiKey.ENTER,false);ui.keyPressed(0,UiKey.RIGHT,false);assertFalse(host.display.value);
 }
 @Test void purposeSearchFindsAnOptionOutsideTheSelectedCategory(){
  Host host=new Host();GraphicsUi ui=new GraphicsUi(host);Draw draw=new Draw();ui.render(draw,480,270,-1,-1);ui.keyPressed(0,UiKey.TAB,false);for(char c:"fsr1".toCharArray())assertTrue(ui.charTyped(c));draw.text.clear();ui.render(draw,480,270,-1,-1);assertTrue(draw.text.contains("Upscaler"));assertFalse(draw.text.contains("Brightness"));
 }
}
