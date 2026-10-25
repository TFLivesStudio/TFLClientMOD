# Ornithe II — optimization and compatibility policy

Ornithe + OSL is the minimum profile. Legacy LWJGL3 and Argentum are optional.
No Argentum implementation is inspected/copied/redistributed or used as a compile dependency.
TFL installs no chunk renderer and does not rewrite Argentum settings.

| Module | Actual modification | Argentum / LWJGL policy and fallback | Evidence / unverified benefit |
| --- | --- | --- | --- |
| fpsBoost | Explicit Vanilla graphics/view/particle/FPS presets, shared persistent undo log | No automatic changes, mipmap overrides, second culler or Argentum settings. User can undo. | Build/runtime survival; native preset/undo acceptance and FPS gain unmeasured. |
| dynamicFps | Native frame cap for focus/minimized/AFK and transient sound attenuation | Existing limiter; game options unchanged; disable restores normal cap/volume. | Three-stack survival; minimized/audio device acceptance pending. |
| entityCulling | No TFL culler | External integration only; do not install a second implementation beside renderer/modpack culling. Saved preference retained. | Intentionally unavailable here; no benefit claimed. |
| builtinOptimizations | No bundled optimizer | Modpack owns external binaries; preference retained. | No implementation/performance claim. |
| lowLatency | Late input poll; optional bounded GPU fences without Argentum | Cached method handles bridge LWJGL2/3. With Argentum, TFL fences bypassed and only latePoll exposed. Default off. | Native survival; hardware latency/frame-pacing benefit unmeasured. |
| netOptimize | Exact Vanilla compression/decryption handler replacements, read-only timing observation | Clientbound only; no keepalive delays/protocol changes. No overlap with chunk renderer. | Native Netty byte-parity unit tests; real encrypted multiplayer acceptance pending. |
| fastConnect | Shared DNS/prefetch and address racing | Vanilla handshake identity retained. No launcher hint/preload integration. | Native mixin/class survival; real multiple-address server behavior pending. |
| particles | Shared allocation gate, particle kind/amount/live bound | Before native particle allocation; Vanilla renderer retained; disabling returns normal allocation. | Native suppress/disable restoration assertions pass all stacks. FPS benefit unmeasured. |
| worldDetails | Native sky/star/weather/atlas gates | Does not replace chunk pipeline. Fog option hidden. | Three-stack survival; each visual gate and physical GPU benefit still need acceptance. |
| borderlessFullscreen | SDL primary-desktop window geometry/decorations | Requires Legacy LWJGL3 bridge, otherwise optional/unavailable. No GL context recreation. | Loaded on LWJGL3/Argentum; Windows/multi-monitor/fullscreen transition acceptance pending. |
| colors | Vanilla post chain, shared color matrix | Skips another active Vanilla post shader; releases chain on disable/reload. No chunk replacement. | Actual grayscale pass on all three stacks; hardware cost unmeasured. |
| minimap/worldMap | Tick-budgeted received-chunk sampling, async region cache, reused textures | Rejects native EmptyChunk, never generates chunks; entities limited to server-sent data. | Native sampled terrain/unknown-chunk rejection/map textures; dimension transitions/large-world stress pending. |
| Nunito/canvas | Reused atlas, batched runs, bounded texture ownership | Native fallback outside Latin atlas. GL state captured/restored around common rendering. | Accents/batch count, fallback metrics, allocation reuse and active-texture restoration asserted. No complete before/after draw-call benchmark. |

## Other integrations

- `trsOnline`, `social`, launcher `clips`: backend unavailable; preserve preferences and block runtime.
- `shieldPosition`: Minecraft 1.8.9 has no combat shields; no simulated replacement.
- Capes: only authorized Vanilla cape resources; local cloth simulation, no unlock/sync service.
- Emotes, screenshots, notes, circuit library, panorama: offline adapters; no TRS network clients.
- Circuit library ships three small offline example templates plus existing local cache support;
  it is not the complete remote catalogue and performs no automatic building.
- Seasonal effects are procedural/local only; no account event state/rewards are granted.
- Streamer mode masks rendered/clipboard data; original logs, config, server identities and exports
  remain intact and should not be treated as sanitized artifacts.
- Automatic chat/refill/reconnect requires explicit platform consent, independent of old preferences.
- Fair Play/server exclusions remain in the shared rules. Multiplayer behavior still needs server acceptance.

See FEATURE_MATRIX.md for every module's exposed/hidden settings and evidence. Software renderer
survival is an integration check, not proof that all settings work or improve hardware performance.
