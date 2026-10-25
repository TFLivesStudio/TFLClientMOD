# Depth & Polish candidate

Development base: PR #7, `5808ac9`. Branch: `feat/depth-polish`. The original Forge target remains present; this candidate targets Ornithe Gen2 1.8.9 and Fabric 26.3. Version `0.4.0-depth.1`.

## Implemented

- Real, precomputed periodic 64³ density and directional-light volume. A bounded CPU ray marcher integrates extinction through a vertical layer, adds soft shadows, and composites through each loader's native texture backend. LOW/BALANCED/HIGH limit cloud sample resolution, ray steps and update rate; OFF releases the map renderer. This is a software volume backend, not a GPU cloud shader.
- Independent terrain composition resolution preserves known map detail. Only received/persisted client map data enters immutable worker snapshots. Unknown pixels use a uniform base, never hidden terrain. Recently received data clears the cloud mask over 900 ms. The minimap explicitly uses a simplified, cached top integral of the same 3D field.
- Smooth world-map tilt (0–55°), yaw, inverse-ground picking, drag and cursor-anchored zoom. The `3D` button cycles top/30°/55°; `R` rotates. Settings persist in World Map. Markers and picking follow the actually displayed worker frame.
- Official `miniPlayer` HUD module (registry now 80 modules): native local-player model, equipment, skin layers, legitimate cape and animation hooks. Shared settings: visibility, camera angle, manual rotation, opacity, background, smoothing, armor/held items/cape, hide with F1/menus, per-widget FPS limit, position and scale. HUD editor/profile/config infrastructure includes it. No second entity or simulation. Modern uses an independent native picture-in-picture renderer; 1.8.9 uses an owned depth framebuffer and restores entity orientation in `finally`.
- Video diagnostics distinguish decoded/uploaded/submitted frames, loop/restart, errors, static preference, disabled/reduced motion and missing decoder. Three bundled encodes of the same blue supplied clip avoid full-HD decoding in lower-quality presets. Static and Off remain explicit preferences. Draw-submission counters are supplemented with native screenshot comparisons; they alone do not prove visible motion.
- Neutral Singleplayer button with hover and keyboard focus retained. Fresh 1.8.9 networking defaults disable experimental fastConnect/netOptimize and retain existing explicit settings. Contextual singleplayer/multiplayer pause labels. Only the basic inventory tutorial prompt is suppressed; other tutorial progress is preserved.

## Validation record

See [the detailed execution record](DEPTH_POLISH_STATUS.md) and `docs/evidence/depth-polish` for native captures, background ROI comparisons, test totals, optimizer pins and CPU measurements. Installed production JARs are exercised with a separate test-probe mod; that probe is excluded from the downloadable mod.

1.8.9 + Argentum passed 248 assertions and a separate process restart. 26.3 + Sodium passed 24 assertions covering native equipment, movement, emote, resource reload, F1, death/respawn and dimension return. Shared tests total 1,150 (zero failures, two skipped); target and packaging tests also pass. CI repeats each required baseline and optimizer stack before publication.

## Installation and known limits

Use separate Minecraft instances with Java 25. For 1.8.9, install Ornithe Gen2 with Fabric Loader 0.19.5 and OSL 0.22.2; this candidate is not a Forge JAR. For 26.3, install Fabric Loader 0.19.5 and Fabric API 0.161.0+26.3. Optimizers remain separate optional downloads with exact hashes in the included dependency manifests. Remove older TFL/TRS JARs from the test instance before installing this candidate.

The volume renderer uses CPU work on a background thread. HIGH is expensive on this host; LOW/BALANCED and OFF allow explicit tradeoffs. Static views reuse the completed image. The native software renderer does not establish physical-GPU FPS. Video rates depend on available CPU; Quality did not sustain its requested 24 fps here. Existing Static/Off or reduced-motion settings remain respected; the title badge explains the active mode.

Iris could not initialize OpenGL on this host; shader packs and physical GPU compatibility remain unverified. Account capes and multiplayer paths require their own environments. Existing limitations of other legacy adapters remain documented; this preview does not certify every setting of all 80 modules.
