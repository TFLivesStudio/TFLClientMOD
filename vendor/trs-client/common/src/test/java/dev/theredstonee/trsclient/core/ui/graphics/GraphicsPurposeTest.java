package dev.theredstonee.trsclient.core.ui.graphics;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GraphicsPurposeTest {
  @Test
  void actualNativeIdsGroupByEffect() {
    assertEquals(0, GraphicsPurpose.category("minecraft:max_frame_rate"));
    assertEquals(0, GraphicsPurpose.category("minecraft:brightness"));
    assertEquals(1, GraphicsPurpose.category("minecraft:entity_shadows"));
    assertEquals(2, GraphicsPurpose.category("celeritas:defer_chunk_updates"));
    assertEquals(2, GraphicsPurpose.category("argentum:entity_instancing"));
    assertEquals(3, GraphicsPurpose.category("amplio:sharpness"));
    assertEquals(3, GraphicsPurpose.category("lucerna:shadow_distance"));
    assertEquals(4, GraphicsPurpose.category("argentum:check_gl_errors"));
  }

  @Test
  void providerNamespacesDoNotBecomeNavigationCategories() {
    assertNotEquals(
        GraphicsPurpose.category("argentum:particle_culling"),
        GraphicsPurpose.category("argentum:check_gl_errors"));
    assertEquals(
        GraphicsPurpose.category("argentum:particle_culling"),
        GraphicsPurpose.category("sodium:particle_culling"));
  }

  @Test
  void unknownExtensionsStayVisibleInAdvancedWithoutGuessingFromTheirName() {
    assertEquals(4, GraphicsPurpose.category("extension:particle_entity_brightness_fps"));
    assertEquals("diagnostics", GraphicsPurpose.group("extension:particle_entity_brightness_fps"));
  }

  @Test
  void spanishLabelsAndAuthoredHelpFollowLanguageChanges() {
    dev.theredstonee.trsclient.core.i18n.I18n.use("es");
    try {
      assertNotEquals(
          "Entity instancing",
          GraphicsPurpose.name("argentum:entity_instancing", "Entity instancing"));
      assertNotEquals(
          "native help", GraphicsPurpose.description("argentum:entity_instancing", "native help"));
      assertFalse(
          GraphicsPurpose.description("argentum:entity_instancing", "native help").isEmpty());
      dev.theredstonee.trsclient.core.i18n.I18n.use("en");
      assertEquals(
          "Entity instancing",
          GraphicsPurpose.name("argentum:entity_instancing", "Entity instancing"));
      assertEquals(
          "native help", GraphicsPurpose.description("argentum:entity_instancing", "native help"));
    } finally {
      dev.theredstonee.trsclient.core.i18n.I18n.use("en");
    }
  }

  @Test
  void reviewedCatalogueHasLocalizedNamesAndExistingSubgroupLabels() throws Exception {
    try (java.io.Reader reader =
        new java.io.InputStreamReader(
            GraphicsPurpose.class.getResourceAsStream("/assets/trsclient/graphics-catalog.json"),
            "UTF-8")) {
      com.google.gson.JsonObject entries =
          new com.google.gson.JsonParser().parse(reader).getAsJsonObject();
      assertTrue(entries.entrySet().size() >= 200);
      for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : entries.entrySet()) {
        com.google.gson.JsonObject row = entry.getValue().getAsJsonObject();
        assertFalse(row.get("nameEs").getAsString().trim().isEmpty(), entry.getKey());
        assertTrue(
            row.get("category").getAsInt() >= 0 && row.get("category").getAsInt() <= 4,
            entry.getKey());
        String key = "graphics.group." + row.get("group").getAsString();
        assertTrue(dev.theredstonee.trsclient.core.i18n.I18n.raw("en").containsKey(key), key);
        assertTrue(dev.theredstonee.trsclient.core.i18n.I18n.raw("es").containsKey(key), key);
      }
    }
  }
}
