package dev.theredstonee.trsclient.core.ui.graphics;
import com.google.gson.*;
import java.io.*;
import java.util.*;
/** Reviewed exact IDs. Unknown extension options remain visible in Advanced. */
public final class GraphicsPurpose {
  private static final Map<String,JsonObject> ENTRIES=load();
  private GraphicsPurpose(){}
  private static Map<String,JsonObject> load(){
    try(Reader reader=new InputStreamReader(GraphicsPurpose.class.getResourceAsStream("/assets/trsclient/graphics-catalog.json"),"UTF-8")){
      Map<String,JsonObject> out=new HashMap<String,JsonObject>();
      for(Map.Entry<String,JsonElement> e:new JsonParser().parse(reader).getAsJsonObject().entrySet())out.put(e.getKey(),e.getValue().getAsJsonObject());
      return Collections.unmodifiableMap(out);
    }catch(IOException e){throw new ExceptionInInitializerError(e);}
  }
  public static String name(String id,String fallback){JsonObject row=ENTRIES.get(id);return row!=null&&dev.theredstonee.trsclient.core.i18n.I18n.code().equals("es")?row.get("nameEs").getAsString():fallback;}
  public static int category(String id){JsonObject row=ENTRIES.get(id);return row==null?4:row.get("category").getAsInt();}
  public static String group(String id){JsonObject row=ENTRIES.get(id);return row==null?"diagnostics":row.get("group").getAsString();}
}
