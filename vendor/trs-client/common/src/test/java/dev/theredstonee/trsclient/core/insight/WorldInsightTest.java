package dev.theredstonee.trsclient.core.insight;
import org.junit.jupiter.api.Test;
import java.util.*;
import dev.theredstonee.trsclient.core.hud.HudAnchor;
import static org.junit.jupiter.api.Assertions.*;
class WorldInsightTest {
    @Test void visibilityKeepsPauseClearAndF1IsExplicitOptIn() {
        WorldInsightModule m = new WorldInsightModule();
        assertFalse(m.shouldDraw(true,false,false,false));
        assertFalse(m.shouldDraw(false,false,true,false));
        m.hideF1.set(false);m.hideMenus.set(false);
        assertTrue(m.shouldDraw(true,false,false,false));
        assertTrue(m.shouldDraw(false,false,true,false));
        assertFalse(m.shouldDraw(false,true,true,false));
        m.setEnabled(false);assertFalse(m.shouldDraw(false,false,false,false));
        assertTrue(m.shouldDraw(false,false,true,true));
    }

    @Test void unknownOriginAndHealthAreNeverInvented() {
        WorldInsightModule m = new WorldInsightModule();
        TargetInfo info = new TargetInfo("NPC", "", null, Collections.emptyList(), null, Float.NaN, Float.POSITIVE_INFINITY);
        assertEquals(Collections.singletonList("NPC"), info.lines(m));
    }
    @Test void targetDoesNotRetainMutablePropertiesAndFiltersAreEffective() {
        List<String> values = new ArrayList<>(Arrays.asList("powered: true"));
        TargetInfo info = new TargetInfo("Lever", "minecraft:lever", "Minecraft", values, null, null, null);
        values.clear();
        WorldInsightModule m = new WorldInsightModule();
        assertTrue(info.lines(m).contains("powered: true"));
        assertFalse(info.lines(m).contains("minecraft:lever"));
        m.identifiers.set(true);m.properties.set(false);m.origin.set(false);
        assertEquals(Arrays.asList("Lever", "minecraft:lever"), info.lines(m));
    }
    @Test void settingsAndHudPlacementRoundTripWithoutLosingPreferences() {
        WorldInsightModule m = new WorldInsightModule();m.identifiers.set(true);m.health.set(false);m.scale.set(1.5);
        m.position().anchor = HudAnchor.BOTTOM_RIGHT;
        WorldInsightModule restored = new WorldInsightModule();restored.read(m.write());
        assertTrue(restored.identifiers.get());assertFalse(restored.health.get());assertEquals(1.5, restored.scale.get());
        assertEquals(HudAnchor.BOTTOM_RIGHT, restored.position().anchor);
    }
}
