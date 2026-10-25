# Project Ornithe II — implementation and release record

Branch `feat/ornithe-1.8.9-poc`; draft PR #6; source baseline `.1` at `c637961`.
Target prerelease `.2`. Main/adoption branches are not merged or modified.

## Implemented

- Removed seven-ID SUPPORTED/PocMenu/PocHost; shared ModMenu/MenuHost with native actions,
  filtered settings and eight-state CapabilityRegistry covering all 79 authoritative IDs.
- Shared HUD/editor/profiles/presets plus real native armor, effects, ping, scoreboards/boss/titles,
  notes and other HUD data. Explicit collision-aware Arrange action and undo test; localized label.
- Received-chunk-only maps, world map and persistent waypoints. Native ClientChunkCache.hasChunk
  always returns true in 1.8.9; adapters explicitly reject EmptyChunk instead of trusting it.
- Zoom/mouse/scroll/cinematic plumbing, sprint/sneak input edges and reset handling; reach/combo,
  local freelook/freecam/keyframes, native cosmetic hooks and local emote/model restoration.
- Native chat components/history/stack/filter/mentions, privacy, tooltips, notes, screenshot watcher,
  gallery/editor, key search and server profiles. Explicit platform consent guards automation.
- Native Redstone states/shared overlays and clock; offline circuit templates/checker with no
  remote service clients. Capes use Vanilla-authorized resources only; seasonal effects local only.
- Panorama six-face native capture/asynchronous encoding/restoration; pending-request cancellation
  prevents disabled-module requests becoming stuck. Native color grading before HUD.
- Native Vanilla detail presets/undo, focus/AFK limiter, particle gate, world detail hooks, SDL
  borderless bridge, conservative latency policy, exact native codec translation and DNS bridge.
  No second chunk renderer/culler, no Argentum config changes, no proprietary binaries bundled.
- Batched Nunito accents, native Unicode fallback, reusable textures and GL state restoration.
  LWJGL2 query buffers require capacity 16; fixed rather than relying on LWJGL3's looser behavior.
- Local diagnostic export and physical-GPU Windows benchmark protocol.

## Evidence and limits

Local native runs: baseline 212 assertions; LWJGL3 214; Argentum 214. Each process restart passes
2 preference/menu assertions. Reports are committed under evidence/. Twenty-one modules have explicit
native output assertions and two additional modules have consent-guard assertions. Other state-toggle
checks are not complete functional acceptance. All 79 are audited in FEATURE_MATRIX.md and
`distribution/ornithe-module-audit.json`: 73 limited/experimental adapters, 1 absent mechanic,
3 unavailable backends, 2 optional external integrations, 0 completely certified modules.

Production remapped JAR (no harness) launched with Argentum and entered an integrated world;
shared title/settings/pause and native HUD screenshots are in evidence/production/.
Hardware is Linux/Xvfb/llvmpipe; no physical GPU or real authenticated multiplayer certification.
See VALIDATION.md / COMPATIBILITY.md for exact remaining per-feature acceptance.

Forge 1.8.9 regression build passed. Fabric 26.3 compilation passed; common regression detected the
missing new editor.arrange translation, now added to all eight supported language tables. Final
sequential common/Ornithe and multiversion build checks are being rerun. An earlier parallel run
also invalidated common class output during tests; those errors are excluded as invalid evidence.

Release is gated on final build/common/native CI. No stable/latest or merge authorization exists.
