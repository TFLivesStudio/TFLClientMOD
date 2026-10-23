# TFL Core — Full TRS Client Mod adoption

## Decision and upstream attribution

We will use **the complete TRS Client MOD source tree** as the authoritative functional foundation for TFL Core / TFL Selection, **not** the TRS Launcher application.

Upstream: https://github.com/theredstonee/TRS-Launcher/tree/main/client-mod
Pinned source commit: `c52087ddb6b73b9d1287ef6d7f9d31010fcb5702` (2026-10-22).
Upstream author: © 2026 Ohev Tamerin (Theredstonee).
License: **GPL-3.0-only**. Preserve `vendor/trs-client/LICENSE`, third-party licensing `vendor/trs-client/licenses/` and `vendor/TRS-UPSTREAM-NOTICE.txt` from upstream root.
The TRS Client also credits dzwdz/Chat Heads under MPL-2.0; inspect additional licensing and branding of resources before redistribution.

**This is a GPL-derived project.** When distributing it, provide the corresponding source and preserve author notices/modified-work notices; separately investigate trademark and media rights. Keep the separately developed TFL Client launcher technically distinct. A private dev repository is acceptable for internal iteration, but must not be the only inaccessible source for released derivatives.

## Scope of the complete adoption

The source snapshot goes under `vendor/trs-client/` and retains, as upstream supplies:
- `common/` — core modules, HUD, config, cosmetics, maps and client logic.
- `fabric/` — multi-version Stonecutter Fabric, including Minecraft 26.3.
- `legacy/` — Forge 1.8.9–1.12.2 support.
- `forge/`, `forge-mojmap-legacy/`, `forge-1.13.2/`, `legacy-1.7.10/`, `neoforge/` — other upstream loader adapters.
- `e4mc-bridge/`, Gradle wrappers, assets, licenses, docs and scripts (except generated caches).

**Verification evidence**: [TRS isolated builds — GitHub Actions #2](https://github.com/TFLivesStudio/TFLClientMOD/actions/runs/39906626198). Both Fabric 26.3 and Forge 1.8.9 tasks passed and uploaded artifacts. This establishes compile-only compatibility, not Minecraft runtime behavior, branding completeness or TFL integration.

**Do not** vendor unrelated Tauri/Nuxt/Rust TRS Launcher application, Microsoft login, signing keys, production backend or release infrastructure.

## Critical truth: source adoption is not yet a port

- Upstream UI uses a redstone/pixel aesthetic and does **not** meet TFL's modern Headless-UI-inspired design requirement. Rewrite the appearance and native UI components for TFL.
- Upstream services include optional TRS badges, capes, friends, hosting, social chat, clips and launcher-specific links. These may call TRS endpoints, launch processes, fetch remote resources or depend on TRS Launcher. **Default them off, audit all endpoints/consent, and replace with TFL-specific integrations only under explicitly approved designs.** Preserve applicable offline features.
- Do not run original `tflcore` and upstream `trsclient` together: both own the title screen, Right Shift, HUD, crosshair, FOV, sprint and config. Build one unified mod and install only one JAR.
- Do not blindly mass-rename namespaces/packages: it can break mixins, assets, translations and persisted config. Plan safe mod ID transition.
- Preserve player data: TFL schema v2, HUD widget placements, waypoints, keybindings and profiles; use backups and tested conversion to the adopted format.
- Keep the cinematic looping **black-hole video menu** as a separate original/licensed asset+player with fallback. TRS does not supply it.
- Preserve upstream copyright notices, even when changing the TRS branding to TFL Selection. Add prominent modified-work notices.

## Migration gates

**Gate 0 — import and provenance**
- [x] Import *complete* `client-mod/` from pinned SHA into `vendor/trs-client/` on this branch.
- [x] Verify SHA, file count, GPL notice, MPL Chat Heads license and loader folders.
- [ ] Preserve current TFL code on `main` until a working replacement is certified.

**Gate 1 — independently compile source**
- [x] Build vendored Fabric 26.3 project with JDK 25. **GitHub Actions successful**.
- [x] Build vendored Forge 1.8.9 separately using the legacy build. **GitHub Actions successful** (Stonecutter requires Gradle runtime Java 21+, configured as Java 25; target compilation remains Java 8).
- [ ] Inspect mod metadata and runtime dependencies; don't claim CI of the OLD TFL root validates vendored sources.
- [ ] Run Fabric 26.3 in an isolated instance, record screen/UI/functional tests.

**Gate 2 — full TFL product adoption**
- [ ] Choose TRS modules as the **sole** module/hud/gameplay implementation; remove old code only after migration.
- [ ] Replace UI, copy, branding, assets, typography with modern TFL design; license media correctly.
- [ ] Disable and audit TRS-specific online services and launcher IPC by default; retain offline functionality.
- [ ] Introduce user data conversion, import/export, and safe-mode.
- [ ] Build optional black-hole video playback with static fallback and measured GPU/CPU impact.
- [ ] Rename `trsclient` to `tflcore` only when migrations, assets, mixin config, resource IDs and tests prove it safe.

**Gate 3 — release certification**
- [ ] Verify CI and runtime for Fabric 26.3, then Forge 1.8.9.
- [ ] Validate menus, HUD, overlays, PvP, minimap, waypoints, performance tools, settings and compatibility with curated mods.
- [ ] Profile performance; don't invent FPS gains.
- [ ] Publish source and license compliance package when distributing GPL-derived JARs.
- [ ] Switch TFL Client install manifest only after a working replacement exists.
- [ ] Merge only when replacement supersedes the legacy mod and no critical regressions remain.

## Upstream isolated build commands

Fabric 26.3, from `vendor/trs-client`:

```powershell
.\gradlew.bat :fabric:26.3:build -PtrsVersions=26.3
```

Forge 1.8.9, from `vendor/trs-client\legacy`:

```powershell
.\gradlew.bat build -PtrsVersions=1.8.9
```

Check Gradle task listings where needed; these are intended commands, **not yet verified as passing in this repository**. Upstream uses distinct Stonecutter Gradle projects and toolchains.

## Migration coordination

The importer lives at `.github/workflows/adopt-trs-upstream.yml` and runs on changes to itself in branch `adopt/trs-client-foundation`. It copies a pinned upstream snapshot and attempts to commit it using repository Actions permission. If repository policy prevents Actions writes, use a local, controlled sparse checkout to vendor source and push the branch manually.

Do not merge the migration branch as production merely because the source snapshot exists. **At the time of this update, TFL's stable install path still uses the original `tflcore` mod. Both isolated TRS builds pass CI; a unified TFL JAR and in-game tests remain pending.**
