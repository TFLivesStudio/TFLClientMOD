package dev.theredstonee.trsclient.core.ui.graphics;

import java.util.*;

/** Explicit, capability-based presets. Never cycles controls to guess a translated value. */
public final class GraphicsProfiles {
  public enum Profile {
    QUALITY,
    BALANCED,
    PERFORMANCE
  }

  public interface Capability {
    Object read();

    boolean accepts(Object value);

    void write(Object value);

    default String consequence() {
      return "graphics.profile.reload";
    }

    default Object[] choices() {
      return new Object[0];
    }
  }

  public static final class Change {
    public final GraphicsUi.Option option;
    public final Capability binding;
    public final Object before, after;

    Change(GraphicsUi.Option option, Capability binding, Object before, Object after) {
      this.option = option;
      this.binding = binding;
      this.before = before;
      this.after = after;
    }
  }

  public static final class Plan {
    public final Profile profile;
    public final List<Change> changes;
    private final List<Change> guards;
    public final List<String> omitted;

    Plan(Profile profile, List<Change> changes, List<String> omitted, List<Change> guards) {
      this.profile = profile;
      this.guards = Collections.unmodifiableList(guards);
      this.changes = Collections.unmodifiableList(changes);
      this.omitted = Collections.unmodifiableList(omitted);
    }
  }

  private final GraphicsUi.Host host;
  private Plan undo;
  private final Map<GraphicsUi.Option, Capability> bindings =
      new IdentityHashMap<GraphicsUi.Option, Capability>();

  private Capability binding(GraphicsUi.Option option) {
    if (!bindings.containsKey(option)) bindings.put(option, option.capability());
    return bindings.get(option);
  }

  public GraphicsProfiles(GraphicsUi.Host host) {
    this.host = host;
  }

  public Plan prepare(Profile profile) {
    List<Change> changes = new ArrayList<>(), guards = new ArrayList<>();
    List<String> omitted = new ArrayList<>();
    for (GraphicsUi.Option option : host.options()) {
      if (option.changed()) throw new IllegalStateException("graphics.pending");
      String key = key(option.id());
      if (key == null) continue;
      Capability binding = binding(option);
      if (!option.available() || binding == null) {
        omitted.add(option.id());
        continue;
      }
      Object current = binding.read(), target = target(key, profile, current, binding.choices());
      if (target == null || !binding.accepts(target)) {
        omitted.add(option.id());
        continue;
      }
      Change change = new Change(option, binding, current, target);
      guards.add(change);
      if (!Objects.equals(current, target)) changes.add(change);
    }
    return new Plan(profile, changes, omitted, guards);
  }

  /** Validate again before the first mutation; rollback all touched values on any failure. */
  public void apply(Plan plan) {
    String beforeConfirmation = host.applyConfirmation();
    if (beforeConfirmation != null && !beforeConfirmation.isEmpty())
      throw new IllegalStateException(beforeConfirmation);
    for (GraphicsUi.Option option : host.options())
      if (option.changed()) throw new IllegalStateException("graphics.pending");
    for (Change c : plan.guards)
      if (!host.options().contains(c.option)
          || !c.option.available()
          || !Objects.equals(c.before, c.binding.read())
          || !c.binding.accepts(c.after)) throw new IllegalStateException("graphics.profile.stale");
    List<Change> touched = new ArrayList<>();
    try {
      for (Change c : plan.changes) {
        touched.add(c);
        c.binding.write(c.after);
        if (!Objects.equals(c.after, c.binding.read()))
          throw new IllegalStateException(c.option.id());
      }
      // Presets exclude scalers and Colores, but never bypass a provider confirmation.
      String confirmation = host.applyConfirmation();
      if (confirmation != null && !confirmation.isEmpty())
        throw new IllegalStateException(confirmation);
      host.apply();
      undo = plan;
    } catch (RuntimeException failure) {
      restore(touched, failure);
      throw failure;
    }
  }

  private void restore(List<Change> changes, RuntimeException original) {
    for (int i = changes.size() - 1; i >= 0; i--) {
      Change c = changes.get(i);
      try {
        c.binding.write(c.before);
        if (!Objects.equals(c.before, c.binding.read()))
          throw new IllegalStateException(c.option.id());
      } catch (RuntimeException rollback) {
        original.addSuppressed(rollback);
      }
    }
    try {
      String confirmation = host.applyConfirmation();
      if (confirmation != null && !confirmation.isEmpty())
        throw new IllegalStateException(confirmation);
      host.apply();
    } catch (RuntimeException rollback) {
      original.addSuppressed(rollback);
    }
    if (original.getSuppressed().length > 0)
      throw new IllegalStateException("graphics.profile.rollbackFailed", original);
  }

  public boolean canUndo() {
    if (undo == null || undo.changes.isEmpty()) return false;
    for (GraphicsUi.Option option : host.options()) if (option.changed()) return false;
    for (Change c : undo.changes)
      if (!c.option.available()
          || !Objects.equals(c.after, c.binding.read())
          || !c.binding.accepts(c.before)) return false;
    return true;
  }

  public void undo() {
    if (!canUndo()) throw new IllegalStateException("graphics.profile.stale");
    List<Change> reverse = new ArrayList<>();
    for (Change c : undo.changes) reverse.add(new Change(c.option, c.binding, c.after, c.before));
    apply(new Plan(undo.profile, reverse, Collections.emptyList(), reverse));
    undo = null;
  }

  /**
   * Derived from actual provider values, including after reopening or editing in another screen.
   */
  public Profile effective() {
    for (Profile profile : Profile.values()) {
      int count = 0;
      boolean match = true;
      for (GraphicsUi.Option option : host.options()) {
        String key = key(option.id());
        if (key == null) continue;
        Capability binding = binding(option);
        if (binding == null || !option.available()) continue;
        Object current = binding.read(), target = target(key, profile, current, binding.choices());
        if (target == null || !binding.accepts(target)) continue;
        count++;
        if (!Objects.equals(current, target)) {
          match = false;
          break;
        }
      }
      if (match && count > 0) return profile;
    }
    return null;
  }

  private static final Map<String, String> KEYS = new HashMap<String, String>();

  static {
    keys(
        "distance",
        "minecraft:render_distance",
        "minecraft:renderDistance",
        "sodium:general.render_distance");
    keys("graphics", "minecraft:graphics", "minecraft:graphics_mode");
    keys(
        "clouds",
        "minecraft:render_clouds",
        "minecraft:clouds",
        "minecraft:cloudStatus",
        "sodium:quality.clouds");
    keys("particles", "minecraft:particles", "sodium:quality.particles");
    keys(
        "lighting",
        "minecraft:smooth_lighting",
        "minecraft:ambient_occlusion",
        "minecraft:ao",
        "minecraft:ambientOcclusion",
        "sodium:quality.smooth_lighting");
    keys(
        "blend",
        "minecraft:biome_blend",
        "minecraft:biomeBlendRadius",
        "sodium:quality.biome_blend");
    keys(
        "mipmap",
        "minecraft:mipmap_levels",
        "minecraft:mipmapLevels",
        "sodium:quality.mipmap_levels");
  }

  private static void keys(String key, String... ids) {
    for (String id : ids) KEYS.put(id, key);
  }

  private static String key(String id) {
    return KEYS.get(id);
  }

  private static Object target(String key, Profile p, Object current, Object[] choices) {
    int rank = p.ordinal();
    if (current instanceof Boolean)
      return (key.equals("graphics") || key.equals("lighting")) ? rank < 2 : null;
    if (current instanceof Number) {
      int n;
      switch (key) {
        case "distance":
          n = new int[] {16, 10, 6}[rank];
          break;
        case "graphics":
          n = rank < 2 ? 1 : 0;
          break;
        case "clouds":
        case "lighting":
          n = new int[] {2, 1, 0}[rank];
          break;
        case "particles":
          n = rank;
          break;
        case "blend":
          n = new int[] {5, 2, 0}[rank];
          break;
        case "mipmap":
          n = new int[] {4, 2, 0}[rank];
          break;
        default:
          return null;
      }
      if (current instanceof Double) return (double) n;
      if (current instanceof Float) return (float) n;
      return n;
    }
    String[] names;
    switch (key) {
      case "graphics":
        names = new String[] {"FANCY", "FANCY", "FAST"};
        break;
      case "clouds":
        names = new String[] {"FANCY", "FAST", "OFF"};
        break;
      case "particles":
        names = new String[] {"ALL", "DECREASED", "MINIMAL"};
        break;
      case "lighting":
        names = new String[] {"HIGH", "LOW", "OFF"};
        break;
      default:
        return null;
    }
    for (Object choice : choices)
      if (choice instanceof Enum<?> && ((Enum<?>) choice).name().equals(names[rank])) return choice;
    return null;
  }
}
