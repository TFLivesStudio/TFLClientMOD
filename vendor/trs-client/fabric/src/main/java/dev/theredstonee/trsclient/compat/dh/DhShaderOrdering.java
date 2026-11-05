//? if >=26.3 {
/*package dev.theredstonee.trsclient.compat.dh;

import com.seibel.distanthorizons.api.DhApi;
import java.lang.reflect.*;
import java.lang.ref.WeakReference;
import java.util.concurrent.ArrayBlockingQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;

// DH 3.3.4 plans its pass before Iris' BeforeRender callback updates the deferred flag.
// Reconcile the same public Iris compatibility state before planning, including frame one.
public final class DhShaderOrdering {
    private static final ArrayBlockingQueue<Message> CHAT=new ArrayBlockingQueue<>(64);
    private record Message(WeakReference<ClientLevel> level,String text){}
    private static Method manager,pipeline,compat,instance,active;
    private static Field override;
    private static boolean available,checked,warned;
    private static int delivered;
    public static void beforePass(){
        if(!checked){checked=true;try{
            Class<?> iris=Class.forName("net.irisshaders.iris.Iris");manager=iris.getMethod("getPipelineManager");active=iris.getMethod("isPackInUseQuick");
            pipeline=Class.forName("net.irisshaders.iris.pipeline.PipelineManager").getMethod("getPipelineNullable");
            compat=Class.forName("net.irisshaders.iris.pipeline.WorldRenderingPipeline").getMethod("getDHCompat");
            instance=Class.forName("net.irisshaders.iris.compat.dh.DHCompat").getMethod("getInstance");
            override=Class.forName("net.irisshaders.iris.compat.dh.DHCompatInternal").getField("shouldOverride");available=true;
        }catch(ClassNotFoundException absent){return;}catch(ReflectiveOperationException error){warn();return;}}
        if(!available||DhApi.Delayed.renderProxy==null)return;
        try{Object current=pipeline.invoke(manager.invoke(null));Object state=current==null?null:instance.invoke(compat.invoke(current));
            DhApi.Delayed.renderProxy.setDeferTransparentRendering((Boolean)active.invoke(null)&&state!=null&&override.getBoolean(state));
        }catch(ReflectiveOperationException error){available=false;warn();}
    }
    private static void warn(){if(!warned){warned=true;dev.theredstonee.trsclient.TrsClient.LOGGER.warn("Iris/DH ordering adapter unavailable for this runtime; native compatibility handlers remain active");}}
    // Font glyph upload must run before a RenderPearl render pass is opened, never from DH's
    // render-task queue inside Sodium's terrain pass. Preserve the native translatable component.
    public static void chat(String text){var level=Minecraft.getInstance().level;if(level!=null)CHAT.offer(new Message(new WeakReference<>(level),text));}
    public static void tick(Minecraft mc){for(int i=0;i<4;i++){Message message=CHAT.poll();if(message==null)return;if(mc.player!=null&&mc.level==message.level.get()){mc.player.sendSystemMessage(Component.translatable(message.text));delivered++;}}}
}

*///?}
