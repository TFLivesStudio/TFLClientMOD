package dev.theredstonee.trsclient.core.ui.graphics;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GraphicsPurposeTest {
 @Test void actualNativeIdsGroupByEffect(){
  assertEquals(0,GraphicsPurpose.category("minecraft:max_frame_rate"));
  assertEquals(0,GraphicsPurpose.category("minecraft:brightness"));
  assertEquals(1,GraphicsPurpose.category("minecraft:entity_shadows"));
  assertEquals(2,GraphicsPurpose.category("celeritas:defer_chunk_updates"));
  assertEquals(2,GraphicsPurpose.category("argentum:entity_instancing"));
  assertEquals(3,GraphicsPurpose.category("amplio:sharpness"));
  assertEquals(3,GraphicsPurpose.category("lucerna:shadow_distance"));
  assertEquals(4,GraphicsPurpose.category("argentum:check_gl_errors"));
 }
 @Test void providerNamespacesDoNotBecomeNavigationCategories(){
  assertNotEquals(GraphicsPurpose.category("argentum:particle_culling"),GraphicsPurpose.category("argentum:check_gl_errors"));
  assertEquals(GraphicsPurpose.category("argentum:particle_culling"),GraphicsPurpose.category("sodium:particle_culling"));
 }
}
