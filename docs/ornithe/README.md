# TFL Client — Project Ornithe II (1.8.9)

Experimental `v0.4.0-ornithe.2`, branch `feat/ornithe-1.8.9-poc`, [draft PR #6](https://github.com/TFLivesStudio/TFLClientMOD/pull/6).
Continues `.1` at `c637961`; does not retire Forge or merge into main.

## Features and honest scope

The existing shared registry, ConfigStore, ModMenu, HUD editor, profiles, presets, crosshair editor,
map engine, world map, waypoint store, local captures/editor, emote wheel, notes and circuit library
now have native Ornithe adapters. The separate seven-module PoC menu is removed.
All 79 IDs have runtime capabilities; only connected settings are exposed, and unknown/unavailable
preferences remain preserved. See [the complete matrix](FEATURE_MATRIX.md) and
[structured audit](../../distribution/ornithe-module-audit.json).

There are **73 limited/experimental native adapters**, one absent 1.8.9 mechanic, three unavailable
backends and two external integrations. **Zero modules are certified as complete across every
setting/lifecycle/platform; 21 have explicit native output assertions.** A toggle test alone does not
verify gameplay. Advanced settings omitted by an adapter are listed individually in the audit.

Native HUD includes armor icons, effects, ping, speed, scoreboard, boss bar and titles. The shared
editor provides drag/scale, guides, undo/redo and explicit **Arrange** with collision handling.
Very crowded all-enabled layouts can still need manual adjustments; existing user layouts are not
silently rearranged. Camera/PvP includes full zoom plumbing, sprint/sneak, reach/combo, local freelook,
freecam/keyframes and cosmetic hooks. Maps only read received chunks and server-sent entities.
Local Redstone, privacy/chat, screenshots, emotes and six-face panorama capture are connected.

Right Shift: modules; V: zoom; M: world map; B/N: add/list waypoints; R: emote wheel.
Rebind through Vanilla Controls or the Key Search action. Each module exposes its native limitations.
AutoGG/text hotkeys/refill/reconnect require an explicit off/on toggle in Ornithe before automation:
old saved enabled flags do not constitute consent. Turn the module off to revoke approval.

The normal title/pause/menu use TFL identity and Nunito. Native options retain Vanilla functionality
with shared button/surface styling; this is not a complete rewrite of every Vanilla screen.
A static licensed black-hole background is included. An optional local `config/tflcore/menu.mjpeg`
uses the shared asynchronous player with static fallback; no unlicensed video is downloaded.
TRS account/social/clip services, remote emote sync, hosting and cosmetic unlocks are unavailable.
Local captures are independent of the launcher clip module.

## Build and exact dependencies

```sh
./vendor/trs-client/ornithe/gradlew -p vendor/trs-client/ornithe build :common:test --no-daemon --max-workers=2
python3 tools/ornithe/verify_jar.py vendor/trs-client/ornithe/build/libs/tflcore-ornithe-1.8.9-0.4.0-ornithe.2.jar
python3 tools/ornithe/audit_capabilities.py
```

The Ornithe JAR requires Java 25. Shared production code remains Java 8/Gson 2.2.4 compatible;
modern Gson is used for existing test fixtures only. External optimization JARs are never bundled.
Exact file URLs, versions and SHA-512 values: [dependencies.json](../../tools/ornithe/dependencies.json).

| Component | Exact version |
| --- | --- |
| Minecraft | 1.8.9 |
| Java | 25 |
| Fabric Loader through Ornithe Gen2 | 0.19.5 |
| Calamus Gen2 | 1.8.9 |
| Feather Gen2 | 1.8.9+build.2 |
| Ploceus / Loom / Gradle | 1.18.3 / 1.18.4 / 9.7.1 |
| Ornithe Standard Libraries | 0.22.2 |
| Legacy LWJGL3 (optional) | 1.4.10 |
| Argentum (optional) | 1.0.2 |

OSL provides the `init` entrypoint, client READY/TICK_END/STOP events and keybind registration.
Minecraft/Feather provide the native screen, texture, FOV and HUD hooks. Ploceus explicitly selects
intermediary generation 2. Feather build **2**, not the similarly named Gen1 build 31, is used.
Raven/Sparrow/Nests metadata has no 1.8.9 entries and those optional patches are not guessed.

References inspected: [official developer page](https://ornithemc.net/develop/),
[official template](https://github.com/OrnitheMC/ornithe-mod-template/tree/gen2/fabric)
at `60988525b70dcabdc72abbfbae0864697b2cc165`, Ornithe Meta v3 Gen2 endpoints, actual
remapped OSL source APIs, and Modrinth metadata for [OSL](https://modrinth.com/mod/osl),
[Legacy LWJGL3](https://modrinth.com/mod/moehreag-legacy-lwjgl3),
[Argentum](https://modrinth.com/mod/argentum). Dependency versions were verified on 2026-10-24.

## Install in Prism Launcher

1. Create a **separate Minecraft 1.8.9 Ornithe Gen2 instance** with the
   [official installer](https://ornithemc.net/download), using its PrismLauncher/MultiMC creator.
   Choose Fabric Loader **0.19.5 through Ornithe**, and your authenticated Minecraft account.
   Generic Fabric/Gen1 profiles lack Calamus Gen2. Keep the official profile arguments
   `-Dfabric.fixPackageAccess=true` and `-Dfabric.gameVersion=1.8.9`.
2. Select a 64-bit **Java 25** executable in the instance settings (for example allocate 2 GiB).
3. Add `tflcore-ornithe-1.8.9-0.4.0-ornithe.2.jar` and **OSL 0.22.2** to Mods.
   Remove older TFL/TRS JARs from this instance; do not add Forge.
4. Launch once. Optionally add **Legacy LWJGL3 1.4.10**, then **Argentum 1.0.2**, downloaded from
   their original Modrinth pages above. TFL does not supply or modify Argentum.
5. Open Right Shift → modules/settings; choose the desired HUDs and use HUD Editor → Arrange or
   position them manually. Enable automation only if wanted. Existing configurations can be copied
   after backup; `config/tflcore/client.json` uses the shared migrations/backups/profiles.

## Reproduce the native checks

```sh
python3 tools/ornithe/fetch_dependencies.py /absolute/local/ornithe-mods
# Requires a display/OpenGL context. Run each stack and its restart sequentially.
python3 tools/ornithe/run_smoke.py --stack baseline --mods-dir /absolute/local/ornithe-mods
python3 tools/ornithe/run_smoke.py --stack baseline --mods-dir /absolute/local/ornithe-mods --restart
# Repeat both commands with --stack lwjgl3, then --stack argentum.
# Installable remapped JAR, separate game directory, no development harness:
./vendor/trs-client/ornithe/gradlew -p vendor/trs-client/ornithe runProdClient -PtestStack=argentum -PruntimeModsDir=/absolute/local/ornithe-mods
```

The harness is excluded from installable/source JARs; corresponding repository ZIP includes its source.
Reports/screenshots land under `vendor/trs-client/ornithe/build/smoke/<stack>`.
[Validation](VALIDATION.md), [Argentum policies](COMPATIBILITY.md),
[Windows benchmark protocol](WINDOWS_BENCHMARK.md) and [release notes](RELEASE.md) document the limits.
Physical GPU/server acceptance and broad modpack testing remain required before retiring Forge.
