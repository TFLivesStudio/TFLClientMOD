package dev.theredstonee.trsclient.core.emote;
import dev.theredstonee.trsclient.core.module.TrsModules;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LocalEmoteTest {
    @Test void localWheelAndAnimationNeedNeitherOnlineObjectNorAccount() {
        TrsModules m=new TrsModules();EmoteController c=new EmoteController(m,null,true);
        assertFalse(m.trsOnline.isEnabled());assertEquals(EmoteController.State.READY,c.state());
        assertEquals(6,c.all().size());assertFalse(c.unlocked("party"));assertTrue(c.unlocked("winken"));
        c.wheelOpened(1000);UUID id=UUID.randomUUID();
        c.tick(1000,Collections.singletonList(new EmotePlayback.Mover(id,0,0,false,true)),null);
        assertEquals(EmoteController.Result.PLAYED,c.play("winken",1000));
        assertTrue(c.selfPlaying(1100));float[] pose=new float[EmoteRig.PARTS*EmoteRig.STRIDE];
        assertTrue(c.apply(id,1500,pose));
        assertEquals(EmoteController.Result.COOLDOWN,c.play("klatschen",1200));
        c.onAttack(1600);assertFalse(c.selfPlaying(1700));
    }
    @Test void everyLocalEmoteCanBeConfirmedFromTheWheelWithoutAService() {
        TrsModules modules = new TrsModules();
        List<EmoteDef> definitions = new EmoteController(modules, null, true).all();
        assertEquals(6, definitions.size());
        for (EmoteDef definition : definitions) {
            EmoteController controller = new EmoteController(modules, null, true);
            long now = System.currentTimeMillis();
            UUID self = UUID.randomUUID();
            controller.tick(now, Collections.singletonList(new EmotePlayback.Mover(self, 0, 0, false, true)), null);
            final int[] clicks = {0};
            dev.theredstonee.trsclient.core.ui.wheel.EmoteWheel wheel = new dev.theredstonee.trsclient.core.ui.wheel.EmoteWheel(controller,
                new dev.theredstonee.trsclient.core.ui.wheel.EmoteWheel.Host() {
                    public Boolean keyHeld() { return null; }
                    public String keyName() { return "V"; }
                    public void close() {}
                    public void actionBar(String text) { fail(text); }
                    public void click() { clicks[0]++; }
                });
            wheel.hover(definition.id());
            assertTrue(wheel.hovered() >= 0, definition.id());
            wheel.confirmNow();
            assertEquals(1, clicks[0], definition.id());
            assertTrue(controller.selfPlaying(System.currentTimeMillis()), definition.id());
        }
    }
    @Test void disabledModuleAndMissingPlayerCannotPlayAndNeverUseNetwork() {
        TrsModules m=new TrsModules();EmoteController c=new EmoteController(m,null,true);
        assertEquals(EmoteController.Result.UNAVAILABLE,c.play("winken",1000));
        m.emotes.setEnabled(false);assertEquals(EmoteController.State.OFF_MODULE,c.state());
        assertDoesNotThrow(()->c.tick(1000,Collections.emptyList(),null));
    }
    @Test void cameraRestoresWhenLeavingWorld() {
        TrsModules m=new TrsModules();EmoteController c=new EmoteController(m,null,true);
        final int[] mode={0};EmoteController.Camera camera=new EmoteController.Camera(){public int get(){return mode[0];}public void set(int n){mode[0]=n;}};
        UUID id=UUID.randomUUID();List<EmotePlayback.Mover> movers=Collections.singletonList(new EmotePlayback.Mover(id,0,0,false,true));
        c.tick(1000,movers,camera);c.play("winken",1000);c.tick(1100,movers,camera);assertNotEquals(0,mode[0]);
        c.tick(1200,Collections.emptyList(),camera);assertEquals(0,mode[0]);
    }
}
