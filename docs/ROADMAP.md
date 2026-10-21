# TFL Core — Roadmap

## Product principles
- Minecraft **26.3 first** (Fabric), **1.8.9 second** (compatibility strategy TBD).
- Strictly client-side; no server-side requirements or competitive advantages through prohibited features.
- Fast by default, beautiful when enabled, fully configurable.
- Use specialized optimization mods bundled by **TFL Client** rather than reimplementing their internals.
- Local-first configuration and no telemetry without explicit opt-in.
- Distribute through TFL Client; no launcher authentication credentials are handled by the mod.

## 0.1 Foundation (this branch)
- [x] Fabric Loom scaffold, Java 25 target, client-only entrypoint.
- [ ] CI build confirmed, reproducible Gradle wrapper checked in.
- [ ] In-game smoke test on 26.3.

## 0.2 Settings and module system
- Modular feature registration and dependency/compatibility checks.
- Local, versioned JSON settings, migrations, presets, export/import.
- Central keybind manager, Right Shift to open TFL Core settings.
- Clear failure isolation between optional modules.

## 0.3 Interface
- Custom main menu with static fallback and configurable 3D scenes.
- Blue TFLives identity, multiple user-selectable themes.
- UI scaling, accessibility, reduced motion and low-impact presets.

## 0.4 HUD
- Widget framework, drag/resize editor, profile import/export.
- FPS, frametimes, ping, coordinates, armor, effects, keystrokes, CPS, biome/time.

## 0.5 Gameplay
- PvP HUD and custom crosshair, zoom, waypoints, minimap and technical overlays.
- Never claim server-side information unavailable to the client.
- Feature permissions/compatibility cautions for third-party servers.

## 0.6 Performance and integration
- Presets: FPS / Balanced / Quality / Custom / hardware suggestions.
- Integrate optimization mod settings only through compatible APIs or supported configs.
- Optional Discord Rich Presence; launcher integration via an explicit, safe protocol.
- Configuration sync via a future secure backend with opt-in and offline fallback.

## 1.8.9 port
- Separate adapter/implementation after Fabric 26.3 is stable.
- Evaluate Forge 1.8.9 + compatible OptiFine as a candidate, not a predetermined solution.

## Not yet implemented
Everything after the 0.1 bootstrap. The roadmap is an intention, not a feature list of a released build.
