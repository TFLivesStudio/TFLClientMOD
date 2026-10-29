package dev.theredstonee.trsclient.core.ui.graphics;

import java.util.Locale;

/** Stable provider IDs, rather than translated labels or mod names, determine navigation. */
public final class GraphicsPurpose {
  private GraphicsPurpose() {}

  public static int category(String id) {
    String key = id.toLowerCase(Locale.ROOT);
    if (key.contains("amplio:") || key.contains("iris:") || key.contains("lucerna:")) return 3;
    if (key.matches(
        ".*(vsync|fullscreen|resolution|framerate|frame_rate|fps_limit|gui_scale|guiscale|brightness|gamma|anaglyph).*"))
      return 0;
    if (key.matches(
        ".*(debug|error|context|unsafe|advanced|safe_chunk_edges|compact_vertex_format|greedy_render_thread).*"))
      return 4;
    if (key.matches(
        ".*(cull|cache|thread|instanc|batch|occlusion|bake|atlas|chunk_builder|defer_chunk_updates|async_graph_search|animate_only_visible|face_sorting|faster_|render_pass_optimization|cpu_render_ahead|inactivityfps|prioritizechunk).*"))
      return 2;
    return 1;
  }
}
