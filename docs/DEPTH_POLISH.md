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

Initial shared suite: 1,149 tests, zero failures, two skipped. Native 1.8.9 baseline: 219 assertions, rendered model verified by nontransparent framebuffer pixels. Native Fabric 26.3: options/pause/emotes launched; a dedicated real-world probe rendered the slim native skin and captured walk/run/jump/sneak, opacity, attachment recreation, and the same map at top/30°/55°. Further production-JAR and optimizer checks are in progress; final evidence is recorded in `DEPTH_POLISH_STATUS.md`.

On this software-rendered host, the 1.8.9 video probe observed approximately 23 presented fps Balanced, 15.8 Quality, and 15 Performance. Quality requests 24 fps but did not sustain it on this host. One pending decoded frame and one native video texture are retained. These are diagnostic measurements, not physical GPU FPS claims. Initial 26.3 volume frames averaged approximately 101 ms on the worker, which limits update rate under interaction; static views reuse the result.

## Known acceptance work still in progress

Production artifacts with isolated test-probe mods, optimizer scenarios, death/respawn/dimension and resource lifecycle, native motion recordings, complete packaging and publication gates. Do not describe this intermediate record as completion of every mission acceptance criterion.
