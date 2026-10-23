package dev.theredstonee.trsclient.core.hud;

import dev.theredstonee.trsclient.core.config.ModuleConfig;
import dev.theredstonee.trsclient.core.module.HudModule;
import dev.theredstonee.trsclient.core.module.Module;
import dev.theredstonee.trsclient.core.module.ModuleRegistry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Bounded session-local HUD history. A drag or slider gesture is committed once on release. */
public final class HudHistory {
    private static final int LIMIT = 40;
    private final ModuleRegistry registry;
    private final List<Map<String, ModuleConfig>> states = new ArrayList<Map<String, ModuleConfig>>();
    private int cursor;

    public HudHistory(ModuleRegistry registry) { this.registry = registry; clear(); }
    public void clear() { states.clear(); states.add(snapshot()); cursor = 0; }
    public boolean canUndo() { return cursor > 0; }
    public boolean canRedo() { return cursor + 1 < states.size(); }
    public void commit() {
        Map<String, ModuleConfig> next = snapshot();
        if (same(states.get(cursor), next)) return;
        while (states.size() > cursor + 1) states.remove(states.size() - 1);
        states.add(next);
        if (states.size() > LIMIT) states.remove(0);
        cursor = states.size() - 1;
    }
    public void undo() { if (canUndo()) apply(states.get(--cursor)); }
    public void redo() { if (canRedo()) apply(states.get(++cursor)); }
    private Map<String, ModuleConfig> snapshot() {
        Map<String, ModuleConfig> out = new LinkedHashMap<String, ModuleConfig>();
        for (Module module : registry.all()) if (module instanceof HudModule) out.put(module.id(), module.write());
        return out;
    }
    private void apply(Map<String, ModuleConfig> state) {
        for (Module module : registry.all()) {
            ModuleConfig config = state.get(module.id());
            if (module instanceof HudModule && config != null) module.read(config.copy());
        }
    }
    private static boolean same(Map<String, ModuleConfig> left, Map<String, ModuleConfig> right) {
        if (!left.keySet().equals(right.keySet())) return false;
        for (String key : left.keySet()) {
            ModuleConfig a = left.get(key), b = right.get(key);
            if (!Objects.equals(a.enabled, b.enabled) || !Objects.equals(a.anchor, b.anchor)
                || !Objects.equals(a.offsetX, b.offsetX) || !Objects.equals(a.offsetY, b.offsetY)
                || !a.flags.equals(b.flags) || !a.numbers.equals(b.numbers) || !a.colors.equals(b.colors)
                || !a.choices.equals(b.choices) || !a.keys.equals(b.keys) || !a.texts.equals(b.texts)) return false;
        }
        return true;
    }
}
