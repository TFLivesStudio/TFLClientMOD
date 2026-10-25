# Windows / physical GPU benchmark protocol

The cloud results use Mesa llvmpipe. They establish functional integration and expose regressions;
they do not establish GPU FPS or input-latency superiority over Forge.

Use separate Prism instances and copied worlds; never point two instances at the same config/world.
Record Windows build, CPU, GPU/driver, RAM, display refresh, resolution, GUI scale, Java vendor/version,
heap allocation, render distance, resource packs, VSync/frame limit, and all optimization settings.
Keep those values and the world seed/save/camera route identical across comparisons.

Compare these profiles:

1. Ornithe Gen2 + OSL, without TFL.
2. The same profile with TFL experimental .2.
3. Ornithe + OSL + Legacy LWJGL3 + Argentum, without TFL.
4. The same optimized profile with TFL .2.
5. Forge 1.8.9 + its current TFL release, only with equivalent graphics/world settings.

For each profile, warm the world for two minutes, then collect three 60-second samples per scenario:
all optional modules off; selected HUD widgets; minimap while following a fixed exploration route;
local emote in third person; module menu/editor/world map; shader color grading separately.
Do not mix panorama capture, chunk generation or shader compilation into steady-state samples.
Record startup and world-load timings separately, with both cold and warm caches identified.

TFL's ESC → Capabilities & Diagnostics → Export measured frame diagnostics writes a local JSON file
under `tflcore-diagnostics/`. It records the most recent up-to-8192 frame intervals, p50/p95/p99,
FPS calculated from mean frame time, heap usage, TFL-owned dynamic texture count/estimated byte size,
map sampled columns, module states and rendering environment. Export immediately after each sample;
label the file with the scenario. Texture bytes exclude Vanilla/Argentum-owned textures and driver
allocation overhead. Initialization timing starts at mod initialization, not at process launch.

For profiles without TFL, use the same external frame-time capture tool/settings for every profile,
including those with TFL. Compare external frame timings with external timings, never with a different
measurement point. A frame-time recorder measures frame delivery, not end-to-end mouse-to-photon
latency. Measure the latter with suitable physical equipment if making latency claims.

Report individual samples and medians. Include p50/p95/p99 rather than a peak FPS screenshot.
A slower p95/p99, increasing heap/texture count across repeated world/menu transitions, input leakage,
wrong FOV restoration, or visual corruption is a regression to investigate before adoption.
Keep GPU queue limiting off in the primary comparison. Test it separately on baseline/LWJGL3;
TFL deliberately bypasses its GPU fences when Argentum is installed.

Acceptance outside the cloud: monitor switching and SDL borderless on Windows, native multiplayer
compression/encryption and multi-address connection failures, Nether/End transitions, high-DPI scaling,
authenticated Vanilla cape rendering, and the user's actual modpack. Attach client logs and exported
JSON, excluding account tokens and private server/config files.
