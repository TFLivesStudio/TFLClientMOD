# Coding agent instructions — TRS Client adoption branch

This branch contains a full snapshot of the independent **TRS Client mod** from https://github.com/theredstonee/TRS-Launcher (commit `c52087ddb6b73b9d1287ef6d7f9d31010fcb5702`) under `vendor/trs-client/`.

**Decision:** TFL Core adopts TRS Client MOD wholesale as its functional foundation. Do not adopt the TRS Launcher (Tauri/Rust/Nuxt).

## Read before changes
1. `docs/TRS_ADOPTION.md` (migration gates and licensing).
2. `vendor/trs-client/README.md`.
3. `vendor/TRS-UPSTREAM-NOTICE.txt`.
4. `vendor/trs-client/fabric/versions/26.3/gradle.properties`, `vendor/trs-client/legacy/versions/1.8.9/gradle.properties`.
5. Original TFL Core 0.3 docs for current config and user-data continuity.

## Priority order
1. **Verify vendored upstream Fabric 26.3 build** and runtime in clean Minecraft; fix only reproducible build problems.
2. Verify Forge 1.8.9 separately. Gradle/Stonecutter must run on JDK 21+ (Java 25 works); compile 1.8.9 bytecode for its required Java version/toolchain.
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
