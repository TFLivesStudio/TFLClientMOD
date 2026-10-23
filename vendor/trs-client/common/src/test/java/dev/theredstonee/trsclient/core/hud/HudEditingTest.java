package dev.theredstonee.trsclient.core.hud;

import dev.theredstonee.trsclient.core.config.ModuleConfig;
import dev.theredstonee.trsclient.core.config.TrsConfig;
import dev.theredstonee.trsclient.core.module.TrsModules;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

class HudEditingTest {
    @Test void presetsPreserveOriginalAndGameplayControls() {
        TrsModules m = new TrsModules();
        m.fps.position().offsetX = .37;
        m.toggleSprint.setEnabled(true);
        m.fullbright.setEnabled(true);
        assertNull(HudPresets.create(m.profiles, m.registry, HudPresets.Preset.MINIMAL));
        assertEquals("Minimal", m.profiles.activeName());
        assertTrue(m.fps.isEnabled());
        assertFalse(m.cps.isEnabled());
        assertFalse(m.minimap.isEnabled());
        assertTrue(m.toggleSprint.isEnabled());
        assertTrue(m.fullbright.isEnabled());
        m.profiles.switchTo(0);
        assertEquals(.37, m.fps.position().offsetX, 1e-9);
        assertTrue(m.cps.isEnabled());
        assertNull(HudPresets.create(m.profiles, m.registry, HudPresets.Preset.MINIMAL));
        assertEquals("Minimal 2", m.profiles.activeName());
    }

    @Test void presetsAreExplicitAndCapacityFailureDoesNotModifyLayout() {
        TrsModules m = new TrsModules();
        assertNull(HudPresets.create(m.profiles, m.registry, HudPresets.Preset.EXPLORATION));
        assertTrue(m.minimap.isEnabled());
        assertTrue(m.coords.isEnabled());
        assertFalse(m.cps.isEnabled());
        while (m.profiles.canCreate()) assertNull(m.profiles.create(m.profiles.suggestName()));
        double before = m.coords.position().offsetX = .41;
        assertNotNull(HudPresets.create(m.profiles, m.registry, HudPresets.Preset.PVP));
        assertEquals(before, m.coords.position().offsetX, 1e-9);
        assertFalse(m.cps.isEnabled());
    }

    @Test void customCopiesAndPresetRoundTrips() {
        TrsModules m = new TrsModules();
        m.fps.scale.set(1.45);
        assertNull(HudPresets.create(m.profiles, m.registry, HudPresets.Preset.CUSTOM));
        assertEquals(1.45, m.fps.scale.get(), 1e-9);
        assertNull(HudPresets.create(m.profiles, m.registry, HudPresets.Preset.BUILDER));
        TrsModules restored = new TrsModules();
        restored.registry.apply(m.registry.capture());
        assertEquals("Builder", restored.profiles.activeName());
        assertTrue(restored.redstoneSignal.isEnabled());
        restored.profiles.switchTo(1);
        assertEquals(1.45, restored.fps.scale.get(), 1e-9);
    }

    @Test void historyCoalescesGesturesAndBranchesAfterUndo() {
        TrsModules m = new TrsModules();
        HudHistory history = new HudHistory(m.registry);
        double initial = m.fps.position().offsetX;
        m.fps.position().offsetX = .2;
        m.fps.position().offsetX = .3;
        m.fps.scale.set(1.5);
        history.commit();
        history.commit();
        history.undo();
        assertEquals(initial, m.fps.position().offsetX, 1e-9);
        assertEquals(1, m.fps.scale.get(), 1e-9);
        assertFalse(history.canUndo());
        history.redo();
        assertEquals(.3, m.fps.position().offsetX, 1e-9);
        history.undo();
        m.fps.position().offsetX = .8;
        history.commit();
        assertFalse(history.canRedo());
        history.clear();
        assertFalse(history.canUndo());
    }

    @Test void historyDoesNotTouchNonHudStateAndIsBounded() {
        TrsModules m = new TrsModules();
        HudHistory history = new HudHistory(m.registry);
        m.fullbright.setEnabled(true);
        for (int i = 1; i <= 100; i++) {
            m.fps.position().offsetX = i / 100.0;
            history.commit();
        }
        int steps = 0;
        while (history.canUndo()) { history.undo(); steps++; }
        assertEquals(39, steps);
        assertTrue(m.fullbright.isEnabled());
    }

    @Test void unavailableModuleSettingsSurviveActiveProfileSaveAndSwitch() {
        TrsModules m = new TrsModules();
        TrsConfig config = m.registry.capture();
        ModuleConfig unknown = new ModuleConfig();
        unknown.texts.put("future", "preserve me");
        config.hudProfiles.profiles.get(0).modules.put("optional-widget", unknown);
        m.registry.apply(config);
        m.profiles.create("Copy");
        m.profiles.switchTo(0);
        TrsConfig saved = m.registry.capture();
        assertEquals("preserve me", saved.hudProfiles.profiles.get(0).modules.get("optional-widget").texts.get("future"));
    }

    @Test void oversizedWidgetsDoNotProduceImpossibleGuideOrNegativeCoordinates() {
        HudSnap.Result result = HudSnap.snap(0, 0, 300, 200, 100, 80, Collections.<int[]>emptyList(), 6);
        assertEquals(0, result.x);
        assertEquals(0, result.y);
        assertFalse(result.hasGuideX());
        assertFalse(result.hasGuideY());
        assertEquals(0, HudLayout.snapX(0, 300, 100));
    }

    @Test void collisionFeedbackAllowsTouchingEdgesAndSnapSupportsAGap() {
        assertFalse(HudLayout.overlaps(new int[]{0, 0, 20, 10}, new int[]{20, 0, 20, 10}));
        assertTrue(HudLayout.overlaps(new int[]{0, 0, 20, 10}, new int[]{19, 0, 20, 10}));
        HudSnap.Result result = HudSnap.snap(83, 60, 20, 10, 320, 240,
                Collections.singletonList(new int[]{50, 60, 30, 10}), 6);
        assertEquals(84, result.x);
    }
}
