package dev.theredstonee.trsclient.core.keys;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class KeyVisibilityTest {
    private static KeySearch.Entry entry(String name, String key) {
        return new KeySearch.Entry(name, name, "TFL Client", "tflcore", key, key, new Object());
    }

    @Test void unavailableRowsStayOutOfEmptyAndExplicitSearchWithoutChangingRegisteredAssignments() {
        List<KeySearch.Entry> registered = new ArrayList<KeySearch.Entry>();
        for (String name : Arrays.asList("wardrobe", "social", "quickReply", "saveClip", "toggleRecording")) {
            registered.add(entry("key.trsclient." + name, "key.keyboard.y"));
        }
        KeySearch.Entry local = entry("key.trsclient.emoteWheel", "key.keyboard.r");
        registered.add(local);
        List<KeySearch.Entry> visible = KeyVisibility.visibleEntries(registered);
        assertEquals(6, registered.size(), "UI filtering must preserve the source mappings");
        assertEquals("key.keyboard.y", registered.get(0).keyCode);
        assertEquals(1, KeySearch.filter(visible, "").size());
        assertSame(local, visible.get(0), "Keep the existing binding handle for editing");
        assertTrue(KeySearch.filter(visible, "social").isEmpty());
        assertTrue(KeySearch.filter(visible, "key:y").isEmpty());
        assertSame(local, KeySearch.filter(visible, "key:r").get(0));
    }

    @Test void hiddenBackendAssignmentCannotProduceAFalseConflictForALocalAction() {
        KeySearch.Entry hidden = entry("key.trsclient.quickReply", "key.keyboard.y");
        KeySearch.Entry local = entry("key.othermod.menu", "key.keyboard.y");
        List<KeySearch.Entry> shown = KeyVisibility.visibleEntries(Arrays.asList(hidden, local));
        assertTrue(KeySearch.filter(shown, "conflict").isEmpty());
        assertSame(local, shown.get(0));
        assertFalse(local.conflict());
    }

    @Test void localAndThirdPartyActionsRemainAvailableRegardlessOfSimilarNames() {
        for (String name : Arrays.asList("key.trsclient.menu", "key.trsclient.emoteWheel", "key.trsclient.worldMap", "key.trsclient.notes", "key.screenshot", "key.othermod.social", "key.othermod.saveClip")) {
            assertTrue(KeyVisibility.visible(name), name);
        }
    }
}
