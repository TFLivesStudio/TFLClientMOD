# TFL Client 1.8.9 — Ornithe II experimental prerelease

**v0.4.0-ornithe.2** expands the seven-module proof of concept into the shared module/settings/HUD
architecture, with native adapters for maps, PvP/camera, Redstone, chat/privacy, local emotes,
captures, notes, panoramas and selected performance controls. Forge remains available separately.

**79 IDs audited; 73 adapters with limitations; 1 absent mechanic; 3 unavailable backends;
2 optional external integrations. Zero modules are certified complete across every setting/platform.
21 modules have explicit native output assertions, plus two consent-guard checks.**
No complete feature parity, hardware FPS superiority or production stability is claimed.

Required: **Minecraft 1.8.9, Ornithe Gen2 / Fabric Loader 0.19.5, Java 25, OSL 0.22.2**.
Optional: **Legacy LWJGL3 1.4.10 + Argentum 1.0.2**, obtained separately from their original authors.
A generic Fabric/Gen1 or Forge instance cannot load this JAR.

Local native checks passed baseline **212**, LWJGL3 **214**, Argentum **214** assertions, plus
**2 restart assertions per stack**. Checks include real map textures, chat component retention,
Redstone state translation, emote pose/camera restoration, isolated freecam movement, a six-face
panorama PNG and the local screenshot gallery. Enabled-state tests alone are not functional certification.
Final CI repeats the build, common/target tests and all three native stacks before publication.

Artifacts: installable JAR, sources JAR, complete corresponding repository ZIP, dependency lock,
79-row JSON/Markdown audit, installation/compatibility/validation/benchmark docs, runtime evidence
and SHA-256 checksums. No external mod binaries or proprietary Argentum implementation are bundled.
Code remains GPL-3.0-only with existing font/icon/MPL notices preserved.

Install the JAR + OSL in a separate Prism Ornithe Gen2 1.8.9 instance using Java 25; launch once,
then optionally add LWJGL3 and Argentum. Open Right Shift for modules and HUD Editor. M opens the
world map; R opens local emotes. Automation requires an explicit off/on toggle before use.
Retain the official profile's `-Dfabric.fixPackageAccess=true -Dfabric.gameVersion=1.8.9` arguments.

Known limits: advanced settings are filtered/preserved rather than falsely exposed; crowded HUD
layouts may need Arrange/manual placement. Real multiplayer, all dimensions, authorized cape visuals,
Windows/macOS and physical GPUs need further acceptance. Animated wallpaper requires an optional
local MJPEG asset. Remote services, recording and multiplayer emote synchronization are unavailable.

[Prism installation](https://github.com/TFLivesStudio/TFLClientMOD/blob/feat/ornithe-1.8.9-poc/docs/ornithe/README.md)
· [79-module audit](https://github.com/TFLivesStudio/TFLClientMOD/blob/feat/ornithe-1.8.9-poc/docs/ornithe/FEATURE_MATRIX.md)
· [Validation](https://github.com/TFLivesStudio/TFLClientMOD/blob/feat/ornithe-1.8.9-poc/docs/ornithe/VALIDATION.md)
· [Compatibility](https://github.com/TFLivesStudio/TFLClientMOD/blob/feat/ornithe-1.8.9-poc/docs/ornithe/COMPATIBILITY.md)
· [Windows measurement protocol](https://github.com/TFLivesStudio/TFLClientMOD/blob/feat/ornithe-1.8.9-poc/docs/ornithe/WINDOWS_BENCHMARK.md)
· [Draft PR #6](https://github.com/TFLivesStudio/TFLClientMOD/pull/6)

Keep Forge until broader functional, server and physical-GPU testing establishes migration readiness.
This prerelease remains experimental, is not marked latest/stable, and does not merge PR #6.
