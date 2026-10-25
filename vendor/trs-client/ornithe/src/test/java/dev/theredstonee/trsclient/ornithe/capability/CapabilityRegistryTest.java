package dev.theredstonee.trsclient.ornithe.capability;

import static org.junit.jupiter.api.Assertions.*;

import dev.theredstonee.trsclient.core.config.ConfigStore;
import dev.theredstonee.trsclient.core.module.TrsModules;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CapabilityRegistryTest {
  @TempDir Path temporary;

  @Test
  void inventoryCoverageAndNoInventedRuntimeAvailability() throws Exception {
    TrsModules modules = new TrsModules();
    var registry = new CapabilityRegistry(modules.registry);
    var tree =
        new com.google.gson.JsonParser()
            .parse(
                Files.readString(
                    Paths.get(System.getProperty("ornithe.repoRoot"), "distribution/modules.json")))
            .getAsJsonObject();
    Set<String> ids = new HashSet<>();
    for (var entry : tree.getAsJsonArray("modules"))
      ids.add(entry.getAsJsonObject().get("id").getAsString());
    assertEquals(80, ids.size());
    assertEquals(ids, registry.entries().keySet());
    for (var module : modules.registry.all()) assertFalse(registry.active(module), module.id());
    assertThrows(
        IllegalArgumentException.class,
        () -> registry.declare("imaginary", CapabilityRegistry.State.PENDING, "no"));
  }

  @Test
  void runnableBindingsMustIdentifyHooksAndKnownSettings() {
    var modules = new TrsModules();
    var registry = new CapabilityRegistry(modules.registry);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            registry.connect(
                modules.zoom, CapabilityRegistry.State.AVAILABLE, "no hook", List.of("factor")));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            registry.connect(
                modules.zoom,
                CapabilityRegistry.State.LIMITED,
                "bad setting",
                List.of("imaginary"),
                "native hook"));
    registry.connect(
        modules.zoom,
        CapabilityRegistry.State.LIMITED,
        "Factor only",
        List.of("factor"),
        "GameRendererMixin.getFov");
    assertEquals(List.of(modules.zoomFactor), registry.settings(modules.zoom));
  }

  @Test
  void unavailableAndConflictPoliciesPreserveSavedStateAndUnknownData() throws Exception {
    var modules = new TrsModules();
    var registry = new CapabilityRegistry(modules.registry);
    modules.zoom.setEnabled(true);
    Path file = temporary.resolve("client.json");
    Files.writeString(
        file,
        "{\"configVersion\":2,\"futureField\":{\"preserved\":true},\"modules\":{\"zoom\":{\"enabled\":true}}}");
    var store = new ConfigStore(file);
    store.load(modules.registry);
    registry.declare("zoom", CapabilityRegistry.State.CONFLICT, "External native conflict");
    assertTrue(modules.zoom.isEnabled());
    assertFalse(registry.active(modules.zoom));
    store.save(modules.registry);
    var tree = new com.google.gson.JsonParser().parse(Files.readString(file)).getAsJsonObject();
    assertTrue(tree.getAsJsonObject("futureField").get("preserved").getAsBoolean());
    var restored = new TrsModules();
    new ConfigStore(file).load(restored.registry);
    assertTrue(restored.zoom.isEnabled());
  }

  @Test
  void futureConfigurationRemainsReadOnly() throws Exception {
    Path file = temporary.resolve("client.json");
    String source = "{\"configVersion\":99,\"modules\":{}}";
    Files.writeString(file, source);
    var modules = new TrsModules();
    var store = new ConfigStore(file);
    assertEquals(ConfigStore.Status.READ_ONLY, store.load(modules.registry));
    assertThrows(java.io.IOException.class, () -> store.save(modules.registry));
    assertEquals(source, Files.readString(file));
  }
}
