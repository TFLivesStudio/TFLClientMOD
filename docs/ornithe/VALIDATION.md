# Ornithe II — validation

Local evidence: Java 25.0.5, Linux x86_64, Xvfb 854×480, Mesa llvmpipe (LLVM 19.1.7).
These are actual Minecraft launches. Software OpenGL measurements do not establish physical-GPU speed or superiority over Forge.

## Client checks

| Stack | Native assertions | Restart assertions | Result |
| --- | ---: | ---: | --- |
| baseline | 212 | 2 | passed / passed |
| lwjgl3 | 214 | 2 | passed / passed |
| argentum | 214 | 2 | passed / passed |

Machine-readable local reports are committed under [evidence/](evidence/). Final Actions runs repeat the suite against the release commit.
The suite tests visible/native outcomes for 21 modules plus consent guards for two; other enabled/disabled checks establish state handling only.
Restart checks verify selected saved preferences, not every setting of every module.

Output assertions include native FOV/sprint/CPS, terrain and unloaded-chunk rejection, negative-coordinate waypoint persistence,
world map textures, native chat components/history/filter, privacy, durability tooltip, particle allocation, Redstone states,
reach/combo, humanoid emote pose restoration, color grading, circuit checker, isolated freecam movement,
six-face panorama PNG/HUD/camera restoration, screenshot watcher and local gallery.
Renderer checks cover active texture unit restoration, one batch per accented Nunito text run, Unicode fallback metrics and texture reuse.

## Build regression

Final build results are recorded in PROJECT_II_PROGRESS.md and the GitHub Actions checks. Common includes the new HUD Arrange/Undo test;
Ornithe has nine target contracts covering capability preservation, explicit consent and byte parity of native Netty compression.
Common production retains Java 8 bytecode/Gson 2.2.4; target Java 25 does not change other platform requirements.

## Diagnostic smoke samples

Mixed test stages and enabled modules, short runs, no controlled baseline without TFL, no forced GC. Values are instrumentation samples only.

| Stack | Frames | FPS from mean | p50 ms | p95 ms | p99 ms | Heap MiB |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| baseline | 397 | 23.21 | 44.07 | 71.88 | 112.45 | 495.0 |
| lwjgl3 | 410 | 23.57 | 43.38 | 67.79 | 106.13 | 506.5 |
| argentum | 436 | 25.45 | 39.39 | 64.89 | 87.29 | 224.3 |

A module-menu diagnostics action exports a bounded frametime ring, p50/p95/p99, heap, owned texture count/estimate, resolution, GL/Java/OS and initialization/world-load durations.
Texture estimates cover TFL-owned dynamic textures, not total GPU VRAM. Export writes asynchronously and excludes server addresses/config content.
Use [WINDOWS_BENCHMARK.md](WINDOWS_BENCHMARK.md) for comparable physical-GPU scenarios with/without TFL and Argentum.

## Remaining acceptance limits

- Windows/macOS and physical GPU stability/performance, borderless multi-monitor behavior and sound on speakers (cloud uses a null audio backend).
- Authenticated multiplayer, encrypted/compressed real connections, multi-address connection racing, reconnect behavior and server policies.
- Actual Nether/End transitions, sustained exploration/teleports, reload stress and large persistent maps.
- Cosmetic tint/animations/cape with an authorized real cape, chat heads, all HUD styles/previews and every exposed setting.
- Default crowded HUD layouts may overlap; explicit Arrange is available and undoable. No universal all-widget fit is claimed.
- No remote services or external recording; shulker/totem/shield mechanics absent from 1.8.9 are not simulated.
- Optional video requires a local MJPEG asset. No included high-resolution animated wallpaper or native player preview is claimed.

Legacy Realms/Twitch absence, Java Unsafe and old logging warnings occur in development; they did not prevent these passing sessions.
Phase I evidence remains in Git history at c637961. Its seven-module claims do not describe this release.
