package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.config.*;
import java.util.*;

/** Reuses ConfigStore; a legacy enabled flag alone never authorizes newly connected automation. */
public final class OrnitheConsent implements ConfigPart {
  public static final Set<String> ACTIONS =
      Set.of("autoGg", "textHotkeys", "autorefill", "autoReconnect");
  private static final String PLATFORM = "ornithe-1.8.9";
  private Map<String, Map<String, Boolean>> values = new LinkedHashMap<>();

  public void read(TrsConfig config) {
    values = new LinkedHashMap<>();
    if (config.platformApprovals != null)
      for (var entry : config.platformApprovals.entrySet())
        if (entry.getValue() != null)
          values.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
  }

  public void write(TrsConfig config) {
    config.platformApprovals = new LinkedHashMap<>();
    for (var entry : values.entrySet())
      config.platformApprovals.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
  }

  public boolean approved(String id) {
    var platform = values.get(PLATFORM);
    return platform != null && Boolean.TRUE.equals(platform.get(id));
  }

  public void explicitToggle(String id, boolean enabled) {
    if (ACTIONS.contains(id))
      values.computeIfAbsent(PLATFORM, k -> new LinkedHashMap<>()).put(id, enabled);
  }
}
