package dev.theredstonee.trsclient.ornithe;

import static org.junit.jupiter.api.Assertions.*;

import dev.theredstonee.trsclient.core.config.*;
import dev.theredstonee.trsclient.core.module.TrsModules;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OrnitheConsentTest {
  @TempDir Path dir;

  @Test
  void legacyEnabledPreferencesNeverGrantApproval() throws Exception {
    Path path = dir.resolve("client.json");
    Files.writeString(
        path,
        "{\"configVersion\":2,\"modules\":{\"autoGg\":{\"enabled\":true},\"autorefill\":{\"enabled\":true}}}");
    var modules = new TrsModules();
    var consent = new OrnitheConsent();
    modules.registry.addPart(consent);
    var store = new ConfigStore(path);
    assertEquals(ConfigStore.Status.LOADED, store.load(modules.registry));
    assertTrue(modules.autoGg.isEnabled());
    assertTrue(modules.autoRefill.isEnabled());
    assertFalse(consent.approved("autoGg"));
    assertFalse(consent.approved("autorefill"));
    store.save(modules.registry);
    var roundTrip = new OrnitheConsent();
    var reloaded = new TrsModules();
    reloaded.registry.addPart(roundTrip);
    new ConfigStore(path).load(reloaded.registry);
    assertFalse(roundTrip.approved("autoGg"));
    assertTrue(reloaded.autoGg.isEnabled());
  }

  @Test
  void explicitChoicePersistsAndPreservesOtherPlatforms() throws Exception {
    var modules = new TrsModules();
    var consent = new OrnitheConsent();
    modules.registry.addPart(consent);
    var config = new TrsConfig();
    config.platformApprovals.put(
        "future-platform", new LinkedHashMap<>(Map.of("unknown-action", true)));
    modules.registry.apply(config);
    consent.explicitToggle("autoGg", true);
    consent.explicitToggle("notes", true);
    assertTrue(consent.approved("autoGg"));
    assertFalse(consent.approved("notes"));
    var captured = modules.registry.capture();
    assertEquals(
        Boolean.TRUE, captured.platformApprovals.get("future-platform").get("unknown-action"));
    var copy = new OrnitheConsent();
    copy.read(captured);
    assertTrue(copy.approved("autoGg"));
    copy.explicitToggle("autoGg", false);
    assertFalse(copy.approved("autoGg"));
  }

  @Test
  void futureConfigurationRemainsUntouched() throws Exception {
    Path path = dir.resolve("future.json");
    String original =
        "{\"configVersion\":999,\"platformApprovals\":{\"ornithe-1.8.9\":{\"autoGg\":true}}}";
    Files.writeString(path, original);
    var modules = new TrsModules();
    var consent = new OrnitheConsent();
    modules.registry.addPart(consent);
    var store = new ConfigStore(path);
    assertEquals(ConfigStore.Status.READ_ONLY, store.load(modules.registry));
    assertFalse(consent.approved("autoGg"));
    assertThrows(java.io.IOException.class, () -> store.save(modules.registry));
    assertEquals(original, Files.readString(path));
  }
}
