# Depth & Polish — validation record

Base: PR #7, commit `5808ac9984b14461476b2b4be42e367dca468654`. Branch `feat/depth-polish`. No merge to main; Forge sources preserved.

Implemented and exercised:

- Shared 64³ density/light field, bounded ray marching, top/30°/55° views, smooth yaw, inverse picking, projected heading and clipped grid. Camera/grid and unknown-mask regression tests pass. Native captures of the same map at all three angles are in `evidence/depth-polish/{189,263}`.
- Only received client map snapshots enter the volume worker. Unknown colors stay uniform; received data clears the mask over 900 ms. The minimap is explicitly labeled a simplified top view.
- Mini Player uses native player models, actual skins (wide Steve in 1.8.9, slim/Alex in 26.3), equipment and local animation hooks. Native tests cover equipment visibility, movement, jump/sneak, attack/emote, F1, resource reload, death/respawn and dimension changes. Shared tests cover settings, profile/config persistence, visibility and update policy. The normal HUD editor consumes the same module; this is not exhaustive manual certification of every editor gesture.
- Installed 1.8.9 JAR + OSL/LWJGL3/Argentum passed 248 assertions, including framebuffer pixels and GL/world-camera restoration. A separate process passed three restart assertions, including Mini Player preferences. The production probe is a separate test mod and is excluded from the distributed JAR.
- Installed 26.3 JAR + Sodium 0.9.2 passed 24 native assertions, including world camera, equipment, emote, resource reload, F1, death, respawn and Nether return. Video/option navigation passed 13 assertions. Further clean-baseline/restart/large-title CI runs repeat these scenarios.
- Shared suite: 1,150 tests, zero failures/errors, two skipped. Nine 1.8.9 target tests and six packaging Python tests passed.
- Native background-only image comparisons show changed pixels in both clients, away from controls/status text. The six-mode playback probes cover loop, Quality/Balanced/Performance, Static, Off and restart. The decoder retains one pending frame and reuses destination/pixel buffers (typically one image and two pixel buffers).
- Singleplayer has no permanent selected tint. Pause labels and the local information header distinguish local worlds; only the basic inventory tutorial is skipped. Fresh 1.8.9 networking defaults are off; existing explicit choices persist.

Performance and limits:

- This host uses software rendering (Mesa/llvmpipe), not a physical GPU. The deterministic CPU volume fixture measured approximately LOW 13 ms, BALANCED 55 ms and HIGH 264 ms per image. HIGH is expensive; static views reuse completed images. These measurements are not game-FPS or GPU claims.
- Native video presentation varies with host load: Balanced approximately 16–24 fps, Quality approximately 12–16 fps, Performance approximately 15 fps. Preset rates are targets, not guaranteed throughput. Static/Off preferences remain unchanged by the player.
- Iris 1.11.7 with the pinned Sodium dependency could not initialize: OpenGL context creation failed, then Iris called an unavailable OpenGL function. Shader-pack/physical-GPU compatibility remains unverified; no Iris success claim is made.
- Authorized-account capes, multiplayer networking and every third-party combination are not certified by local offline probes. Other historical 1.8.9 adapter limitations remain; adding Mini Player does not certify all 80 modules.

CI and publication:

The `Depth and Polish` workflow builds and runs installed JARs in baseline/LWJGL3/Argentum and baseline/Sodium matrices, checks restart and video reports, and only publishes prereleases after all jobs pass. All five native matrix scenarios and common tests passed in [run 40238168496](https://github.com/TFLivesStudio/TFLClientMOD/actions/runs/40238168496). Both publication jobs succeeded. Published source commit: `540cc243b29746bcd1ebf0cf9e212eb044b82a0a`; [PR #8](https://github.com/TFLivesStudio/TFLClientMOD/pull/8) remains a draft, preserving PR #7 and main.

Published downloads:

- [1.8.9 prerelease](https://github.com/TFLivesStudio/TFLClientMOD/releases/tag/v0.4.0-depth.1-ornithe189): installed JAR, complete source ZIP, exact dependencies, native evidence, instructions and SHA-256.
- [26.3 prerelease](https://github.com/TFLivesStudio/TFLClientMOD/releases/tag/v0.4.0-depth.1-fabric263): the corresponding Fabric deliverables.

The downloaded published JARs, evidence ZIPs and source ZIP were checked against the release SHA-256 manifests. The archived CI reports contain 248 + 3 checks for 1.8.9/Argentum, and 27 + 1 + 13 for 26.3/Sodium (some camera-ready checks repeat while the worker completes).

Additional native screen recordings are [1.8.9 baseline](evidence/depth-polish/native-189-baseline.mp4) and [26.3 title playback](evidence/depth-polish/native-263-title.mp4). They are real 8 fps X11 captures on a software renderer, not GPU/game FPS benchmarks. The 1.8.9 recording includes automated world/camera/effect transitions; there is no generated UI or interpolated mockup. Movie documentation was archived after the binary publication; the production code remains the published commit above.
