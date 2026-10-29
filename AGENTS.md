# Active targets (user decision, 2026-10-28)

- Minecraft 1.8.9: Ornithe Gen2 + optional Argentum; modern versions: Fabric.
- Forge, NeoForge, other legacy adapters and the old root src tree have been retired. Do not reintroduce them.
- Diorama (MIT) is the map engine; preserve vendor/diorama/LICENSE and pinned UPSTREAM.md.
- Visual quality, animation and full UI identity are the default; performance reductions are explicit options.
- Preserve user data and the shared module/settings stack. Test both current targets before claiming parity.

The following adoption-stage notes are historical where they mention retired targets.

# Coding agent instructions — TRS Client adoption branch

This branch contains a full snapshot of the independent **TRS Client mod** from https://github.com/theredstonee/TRS-Launcher (commit `c52087ddb6b73b9d1287ef6d7f9d31010fcb5702`) under `vendor/trs-client/`.

**Decision:** TFL Core adopts TRS Client MOD wholesale as its functional foundation. Do not adopt the TRS Launcher (Tauri/Rust/Nuxt).

## Read before changes
1. `docs/TRS_ADOPTION.md` (migration gates and licensing).
2. `vendor/trs-client/README.md`.
3. `vendor/TRS-UPSTREAM-NOTICE.txt`.
4. `vendor/trs-client/fabric/versions/26.3/gradle.properties`, `vendor/trs-client/ornithe/gradle.properties`.
5. Original TFL Core 0.3 docs for current config and user-data continuity.

## Priority order
1. **Verify vendored upstream Fabric 26.3 build** and runtime in clean Minecraft; fix only reproducible build problems.
2. Verify Ornithe Gen2 1.8.9 separately. Gradle/Stonecutter must run on JDK 21+ (Java 25 works); compile 1.8.9 bytecode for its required Java version/toolchain.
3. Map every TRS optional online service, TRS Launcher IPC, hardcoded TRS host, updater and sensitive capability; disable by default in future TFL build, preserving audited offline features. Do not silently substitute TFL credentials.
4. Choose one authoritative module/HUD/render stack from TRS; retire the old TFL stack only after working TFL build is demonstrated.
5. Replace **all redstone/retro/pixel UI** with modern Headless-UI-inspired native desktop design, legible typography and accessible interaction. Preserve module features and don't hide defects behind visuals.
6. Add a real high-resolution **black-hole looping video wallpaper** only with rights-cleared assets, asynchronous decoding, static/reduced-motion fallback, and measured performance.
7. Preserve original TFL user settings/waypoints/profiles via a tested migration from TFL schema v2; don't destroy settings.
8. Keep up-to-date COPYING/NOTICE/third-party licenses and upstream attribution; changes under GPL-3.0-only.
9. Build, test, benchmark and **actually launch Minecraft** before RC claims.

## Critical constraints
- Do not merge this draft PR into main while there are competing mod IDs, keybinds, HUD, title replacement, or world lifecycle callbacks.
- Do not call a green build of the OLD root TFL mod evidence that imported TRS works.
- Don't claim the upstream redstone UI already meets the user’s modern GUI requirements: it does not.
- Use bounded commits, no force push or destructive resets; avoid changing unrelated TFL projects.
- Publish no release artifact before audited API/branding/license and client integration.

## Operation Modpack Ready — active TFL distribution

The root build delegates to the adopted implementation; the inactive root `src/` has been retired. Build current TFL with `vendor/trs-client/gradlew -p vendor/trs-client
:common:test :fabric:26.3:build -PtrsVersions=26.3`, then `python3 tools/distribution.py package <jar>`.
The original Java package/resource namespace `trsclient` is deliberate; public loader ID is `tflcore`.
Do not re-enable TRS services or bundle optimization binaries without a separately approved audit.
Keep common compatible with Java 8 and Gson 2.2.4; the active Ornithe adapter itself requires Java 25.
Read docs/RELEASE_READINESS.md and docs/CONTINUATION.md for actual evidence and open gates.

## Shared UI parity (active user requirement)

New menu/HUD/map features must be implemented for both active targets: Minecraft 1.8.9
and Fabric 26.3. Keep presentation, settings IDs/defaults/persistence and behavior in common;
platform-specific rendering/input hooks may differ. Exercise both adapters when a shared
feature changes. The existing 1.8.9 capability audit still applies: do not claim complete
functional equivalence for adapters whose options remain limited. Use `.github/workflows/unified-client.yml`
for shared regressions and record any unverified behavior explicitly. Visible menus use TFL
branding; retain technical loader identities, dependency declarations and legal notices.
