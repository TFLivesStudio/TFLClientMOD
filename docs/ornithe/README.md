# Project Ornithe — Phase 1 (Minecraft 1.8.9 only)

Experimental platform for **the existing TFL Client**, not a replacement for the Forge distribution.
Branch `feat/ornithe-1.8.9-poc`; review target `adopt/trs-client-foundation`. No changes to `main`.

## Scope

The same `TrsModules`, `ConfigStore`, settings controls, `TitleUi`, `HudEditor`, `HudLayout`,
`ZoomState`, `ToggleState`, `ClickCounter` and crosshair geometry are compiled from `common`.
The adapter exposes seven implemented modules: FPS, coordinates, CPS, keystrokes, zoom,
toggle sprint and crosshair. Other registry entries remain preserved in configuration but have no
Ornithe hooks and are not presented as working features. No TRS backend, launcher IPC, updater,
account service or optimization module is started.

Right Shift opens settings; V holds zoom; use vanilla Controls to rebind these keys.
The sprint control toggles sprint while the module is enabled; native press/release edges
retain short taps and ignore repeated key-downs. Leaving a world or opening a GUI
clears its latch. Vanilla continues to enforce movement, food and collision conditions.
The HUD editor reuses common positioning/scaling. Native menus handle worlds, servers and options.
Zoom offers factor/smoothing in this phase; wheel zoom, mouse slowdown and cinematic mode are
not ported. Coordinates show XYZ; keystrokes show movement/space (mouse CPS is a separate HUD).
Nunito is generated once as a texture atlas on resource initialization/reload; vanilla font is fallback.
The title uses the existing static licensed wallpaper and shared modern UI.

## Reproducible build

Requires JDK 25, network for Maven/Minecraft downloads, and the checked-in wrapper:

```sh
./vendor/trs-client/ornithe/gradlew -p vendor/trs-client/ornithe build :common:test --max-workers=2
python3 tools/ornithe/verify_jar.py vendor/trs-client/ornithe/build/libs/tflcore-ornithe-1.8.9-0.4.0-ornithe.1.jar
```

No Forge or Fabric 26.3 project is configured by this build. Shared production code is also
compiled with Java 8/Gson 2.2.4 by `:common:compileJava`; existing tests use Gson 2.11.0
because their fixtures call newer JSON mutation APIs. The Ornithe distribution itself requires
Java 25 (class version 69), as requested.

The exact dependency lock and SHA-512 values are in
[`dependencies.json`](../../tools/ornithe/dependencies.json). Runtime external mods are NOT bundled.

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

## Prism Launcher installation

1. Download the [official Ornithe installer](https://ornithemc.net/download) and use its
   **PrismLauncher/MultiMC instance creator** to create a separate Minecraft 1.8.9 instance.
   Import/open that instance in Prism; use your authenticated Minecraft account.
2. Install **Ornithe Gen2**, selecting Fabric Loader 0.19.5 through the Ornithe installer/component.
   The official installer supplies the required launcher component. A generic Fabric or Gen1 profile
   does not supply Calamus Gen2. The official profile also sets `-Dfabric.fixPackageAccess=true`
   and `-Dfabric.gameVersion=1.8.9`; retain those JVM arguments. Do not install Forge into this instance.
3. In instance Settings → Java, select a **64-bit Java 25** executable. Allocate suitable memory
   (for example 2 GiB); the release does not alter launcher settings.
4. Add the experimental `tflcore-ornithe-1.8.9-0.4.0-ornithe.1.jar` and OSL 0.22.2 to Mods.
5. First launch with those two mods. Then add Legacy LWJGL3 1.4.10; finally add Argentum 1.0.2.
   Download original files from their Modrinth pages; the lock above identifies exact releases/hashes.
6. Remove other TFL/TRS client JARs from this instance. Open TFL settings with Right Shift.
   Enable Coordinates/Crosshair as desired; open HUD Editor to position the supported overlays.

Configuration uses `config/tflcore/client.json` and the existing backup/migration logic. Start with
an isolated instance; copying configuration is optional. Unsupported module data is retained.
No Argentum configuration is read or rewritten by TFL.

## Runtime reproduction and evidence

A display/OpenGL context is required. The development harness loads a deterministic flat world,
opens shared screens, exercises module state and native hooks, writes screenshots/JSON, and quits.
Its classes are excluded from distributable and source JARs. It cannot be activated in production.

```sh
python3 tools/ornithe/fetch_dependencies.py /absolute/local/ornithe-mods
./vendor/trs-client/ornithe/gradlew -p vendor/trs-client/ornithe runClient -Psmoke
./vendor/trs-client/ornithe/gradlew -p vendor/trs-client/ornithe runClient -Psmoke -Prestart
./vendor/trs-client/ornithe/gradlew -p vendor/trs-client/ornithe runClient -Psmoke -PtestStack=lwjgl3 -PruntimeModsDir=/absolute/local/ornithe-mods
./vendor/trs-client/ornithe/gradlew -p vendor/trs-client/ornithe runClient -Psmoke -PtestStack=argentum -PruntimeModsDir=/absolute/local/ornithe-mods
```

Reports land in `vendor/trs-client/ornithe/build/smoke/<stack>/`. The baseline must run before its
restart check. External mods enter **only the local runtime classpath**, remapped by Loom;
Argentum has no compile/API dependency. Its proprietary implementation is not copied or redistributed.
Read the accompanying [validation report](VALIDATION.md) for actual outcomes and limits.

## Architecture and next phases

| Concern | Existing Forge 1.8.9 | New Ornithe Gen2 1.8.9 |
| --- | --- | --- |
| Loader/build | Forge/FML, separate legacy build | Fabric Loader, Ploceus/Loom independent build |
| Naming | MCP/Forge APIs | Feather names → Calamus Gen2 at remap |
| Lifecycle/input | FML events and legacy hooks | OSL lifecycle/keybinds plus bounded mixins |
| Shared behavior | `common` | Same `common`, no second registry/config model |
| Graphics | Existing Forge adapter | Native Canvas/texture bridge; LWJGL2 compatibility APIs also supplied by Legacy LWJGL3 |
| Renderer | Existing vanilla/Forge pipeline | External Argentum optional; no chunk renderer implemented by TFL |
| Runtime Java | Legacy-compatible bytecode | Explicit Java 25 experiment |
| Feature claim | Existing release scope | Seven initial modules only |

Next: (1) validate this experiment on Windows/Linux/macOS physical GPUs and real servers;
(2) add per-module capability declarations and native input tests, preserving configuration;
(3) port pure HUD modules (clock/memory/armor/effects/ping), then world data modules with lifecycle tests;
(4) port camera/visual hooks one at a time with Argentum compatibility checks;
(5) audit performance/render modules individually and omit those redundant with Argentum;
(6) keep unavailable social/account/hosting integrations disabled pending separate authorization/audit.
Only consider retiring Forge after feature parity, measured representative performance and stability.

No benchmark here establishes an improvement over Forge or a benefit attributable to Argentum.
Software OpenGL smoke measurements are diagnostic, not a hardware performance comparison.
