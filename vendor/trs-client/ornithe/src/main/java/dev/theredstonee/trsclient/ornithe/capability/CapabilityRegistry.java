package dev.theredstonee.trsclient.ornithe.capability;

import dev.theredstonee.trsclient.core.module.Module;
import dev.theredstonee.trsclient.core.module.ModuleRegistry;
import dev.theredstonee.trsclient.core.module.Setting;
import java.util.*;

/** Runtime bindings and platform limits. Saved enabled state is never rewritten by availability. */
public final class CapabilityRegistry {
  public enum State {
    AVAILABLE(true),
    LIMITED(true),
    VERSION_UNAVAILABLE(false),
    OPTIONAL_INTEGRATION(false),
    BACKEND_UNAVAILABLE(false),
    CONFLICT(false),
    EXPERIMENTAL(true),
    PENDING(false);
    public final boolean runnable;

    State(boolean runnable) {
      this.runnable = runnable;
    }
  }

  public record Capability(State state, String detail, Set<String> settings, List<String> hooks) {
    public Capability {
      Objects.requireNonNull(state);
      Objects.requireNonNull(detail);
      settings = Set.copyOf(settings);
      hooks = List.copyOf(hooks);
      if (state.runnable && hooks.isEmpty())
        throw new IllegalArgumentException("A runnable capability needs native runtime bindings");
    }
  }

  private final Map<String, Capability> entries = new LinkedHashMap<>();

  public CapabilityRegistry(ModuleRegistry registry) {
    for (Module m : registry.all())
      entries.put(
          m.id(),
          new Capability(
              State.PENDING, "Native Ornithe adapter not connected yet", Set.of(), List.of()));
  }

  public void declare(String id, State state, String detail) {
    put(id, new Capability(state, detail, Set.of(), List.of()));
  }

  public void connect(
      Module module, State state, String detail, Collection<String> settings, String... hooks) {
    if (!state.runnable)
      throw new IllegalArgumentException("Use declare for unavailable capabilities");
    Set<String> known = new HashSet<>();
    for (Setting s : module.settings()) known.add(s.key());
    if (!known.containsAll(settings))
      throw new IllegalArgumentException("Unknown settings for " + module.id());
    put(module.id(), new Capability(state, detail, new HashSet<>(settings), Arrays.asList(hooks)));
  }

  private void put(String id, Capability entry) {
    if (!entries.containsKey(id)) throw new IllegalArgumentException("Unregistered module: " + id);
    entries.put(id, entry);
  }

  public Capability get(String id) {
    Capability c = entries.get(id);
    if (c == null) throw new IllegalArgumentException(id);
    return c;
  }

  public boolean runnable(Module m) {
    return get(m.id()).state().runnable && m.available();
  }

  public boolean active(Module m) {
    return runnable(m) && m.isEnabled();
  }

  public List<Setting> settings(Module m) {
    Set<String> implemented = get(m.id()).settings();
    return m.settings().stream().filter(s -> implemented.contains(s.key())).toList();
  }

  public Map<String, Capability> entries() {
    return Collections.unmodifiableMap(entries);
  }
}
