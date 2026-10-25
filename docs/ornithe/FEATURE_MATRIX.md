# Ornithe II — 79-module portability audit

Generated with `python3 tools/ornithe/audit_capabilities.py` from the authoritative inventory and committed native evidence.

**79 audited; 73 native adapters with limitations; 1 unavailable mechanic; 3 unavailable backends; 2 external integrations.**
**0 completely certified; 21 modules have explicit native output assertions; 2 additional modules have consent-guard assertions.**
No runtime PENDING entry remains. This does not establish full feature/setting parity. The other adapters still need functional acceptance beyond toggle/screen survival.

Detailed hooks, settings, activation/deactivation, persistence, restoration, dependencies, per-stack checks and limitations are in [the JSON audit](../../distribution/ornithe-module-audit.json).

| ID | Category | Runtime state (Argentum) | Native evidence | Limitations / preserved hidden settings |
| --- | --- | --- | --- | --- |
| fps | HUD | LIMITED | state/screen survival only; functional acceptance pending | Ornithe HUD adapter; advanced options are listed only when connected |
| cps | PVP | LIMITED | output assertions | Ornithe HUD adapter; advanced options are listed only when connected |
| keystrokes | PVP | LIMITED | state/screen survival only; functional acceptance pending | Native coordinates/direction/biome and actual bindings with mouse/CPS/space options |
| ping | HUD | LIMITED | state/screen survival only; functional acceptance pending | Shared tab latency, keepalive jitter, history, spike warning and TPS estimate. 1.8.9 has no active ping-request packet; interval is preserved but not exposed. Hidden: interval. |
| armor | PVP | LIMITED | state/screen survival only; functional acceptance pending | Native item icons with shared ArmorLayout and durability formatting |
| effects | HUD | LIMITED | state/screen survival only; functional acceptance pending | Native tick-cached HUD data |
| coords | WORLD | LIMITED | state/screen survival only; functional acceptance pending | Native tick-cached HUD data |
| clock | HUD | LIMITED | state/screen survival only; functional acceptance pending | Native tick-cached HUD data |
| memory | HUD | LIMITED | state/screen survival only; functional acceptance pending | Native tick-cached HUD data |
| server | WORLD | LIMITED | state/screen survival only; functional acceptance pending | Native tick-cached HUD data |
| packs | HUD | LIMITED | state/screen survival only; functional acceptance pending | Native tick-cached HUD data |
| toggleSprint | PVP | LIMITED | output assertions | Ornithe HUD adapter; advanced options are listed only when connected Hidden: flyBoost, flyBoostFactor, onlyForward, remember. |
| toggleSneak | PVP | LIMITED | state/screen survival only; functional acceptance pending | Native edge-triggered sneak; resets on GUI/world change Hidden: remember. |
| reach | PVP | LIMITED | output assertions | Observed eye-to-hit distance only |
| combo | PVP | LIMITED | output assertions | Confirmed native hurt events after a local attack |
| speed | PVP | LIMITED | state/screen survival only; functional acceptance pending | Native tick-cached HUD data |
| minimap | WORLD | LIMITED | output assertions | Received chunks only; native entity taxonomy and texture-color sampling are limited |
| worldMap | WORLD | LIMITED | output assertions | Received chunks only; native entity taxonomy and texture-color sampling are limited |
| crosshair | PVP | LIMITED | output assertions | Basic geometry connected; 1.8.9 has no attack cooldown indicator Hidden: attack. |
| hitColor | PVP | EXPERIMENTAL | state/screen survival only; functional acceptance pending | Native cosmetic transform/tint hooks only; does not change attacks, cooldowns, damage or packets |
| freelook | WORLD | EXPERIMENTAL | state/screen survival only; functional acceptance pending | Independent native camera angles, shared hold/toggle and server exclusion list. Restores perspective on GUI, death and world changes. |
| zoom | WORLD | LIMITED | output assertions | Native FOV, scroll, mouse slowdown, cinematic smoothing and configurable binding |
| fullbright | WORLD | LIMITED | state/screen survival only; functional acceptance pending | Native lightmap gamma read; original option is never overwritten |
| autorefill | WORLD | EXPERIMENTAL | guard only | Replenishes an emptied selected slot using a normal inventory swap. Survival/adventure only, no GUI, no Fair Play. Old enabled flags require an explicit off/on toggle here. |
| titleScreen | MISC | LIMITED | state/screen survival only; functional acceptance pending | Shared TFL title and asynchronous optional local video at config/tflcore/menu.mjpeg; static black-hole fallback when unavailable |
| menuStyle | MISC | LIMITED | state/screen survival only; functional acceptance pending | Shared pause navigation and modern native option/button surfaces; vanilla behavior retained |
| oldAnimations | PVP | EXPERIMENTAL | state/screen survival only; functional acceptance pending | Native cosmetic transform/tint hooks only; does not change attacks, cooldowns, damage or packets |
| lowFire | PVP | EXPERIMENTAL | state/screen survival only; functional acceptance pending | Native cosmetic transform/tint hooks only; does not change attacks, cooldowns, damage or packets |
| shieldPosition | PVP | VERSION_UNAVAILABLE | state/screen survival only; functional acceptance pending | Combat shields were introduced in Minecraft 1.9 Hidden: blockRotX, blockRotY, blockRotZ, blockScale, blockX, blockY, blockZ, normalRotX, normalRotY, normalRotZ, normalScale, normalX, normalY, normalZ, opacity, preset, transparent. |
| blockOutline | WORLD | EXPERIMENTAL | state/screen survival only; functional acceptance pending | Native cosmetic transform/tint hooks only; does not change attacks, cooldowns, damage or packets |
| hitboxes | PVP | EXPERIMENTAL | state/screen survival only; functional acceptance pending | Native cosmetic transform/tint hooks only; does not change attacks, cooldowns, damage or packets |
| noHurtCam | PVP | LIMITED | state/screen survival only; functional acceptance pending | Cancels native hurt-camera transform; restores automatically when disabled |
| chat | CHAT | LIMITED | output assertions | Timestamps, stacking and bounded history preserve native text/click/hover components. Copy and coordinate links are not connected yet. Hidden: coordLinks, copy, copyMode. |
| autoGg | CHAT | LIMITED | guard only | Sends chat using shared sanitization/rate limits. Old enabled preferences do not authorize sends: switch this module off/on here to approve Ornithe. Disable at any time. |
| textHotkeys | CHAT | LIMITED | state/screen survival only; functional acceptance pending | Sends chat using shared sanitization/rate limits. Old enabled preferences do not authorize sends: switch this module off/on here to approve Ornithe. Disable at any time. |
| waypoints | WORLD | LIMITED | output assertions | Shared persistent points, death markers, add/list bindings and projected names/distances/beams in first person. Third-person world projection is intentionally omitted. |
| trsOnline | MISC | BACKEND_UNAVAILABLE | state/screen survival only; functional acceptance pending | Requires an authorized TFL backend/launcher integration; saved preferences retained Hidden: badgeNametag, badgeTab, capes, cosmetics, fanSymbolNametag, fanSymbolTab, sync. |
| capePhysics | MISC | EXPERIMENTAL | state/screen survival only; functional acceptance pending | Shared bounded cloth/LOD and native cape layer. Uses existing Vanilla cape ownership/resources only, no remote cosmetics. Menu player preview is not connected. |
| colors | WORLD | EXPERIMENTAL | output assertions | Shared color matrix/vibrance with Vanilla post-processing. Skips existing Vanilla shaders; no second world renderer. GPU hardware performance remains unmeasured. |
| emotes | MISC | EXPERIMENTAL | output assertions | Local radial wheel/model animation only; no remote synchronization Hidden: others. |
| redstoneSignal | REDSTONE | LIMITED | output assertions | Native 1.8.9 received block states and shared renderers. Container contents remain unknown; client packet timing is not server TPS. |
| redstoneOverlay | REDSTONE | LIMITED | state/screen survival only; functional acceptance pending | Native 1.8.9 received block states and shared renderers. Container contents remain unknown; client packet timing is not server TPS. |
| redstoneClock | REDSTONE | LIMITED | state/screen survival only; functional acceptance pending | Native 1.8.9 received block states and shared renderers. Container contents remain unknown; client packet timing is not server TPS. |
| clips | MISC | BACKEND_UNAVAILABLE | state/screen survival only; functional acceptance pending | Requires an authorized TFL backend/launcher integration; saved preferences retained Hidden: background, backgroundOpacity, bufferIcon, scale, shadow, textColor. |
| fpsBoost | PERFORMANCE | LIMITED | state/screen survival only; functional acceptance pending | Explicit Vanilla-only detail presets with persistent shared UndoLog. No automatic preset, no culler or Argentum config changes; no measured FPS gain is claimed. |
| dynamicFps | PERFORMANCE | EXPERIMENTAL | state/screen survival only; functional acceptance pending | Shared focus/minimized/AFK limits through the vanilla frame limiter. Sound attenuation is transient; options are preserved. |
| borderlessFullscreen | PERFORMANCE | EXPERIMENTAL | state/screen survival only; functional acceptance pending | SDL borderless on the primary desktop, with window geometry restoration and no GL context recreation. Multi-monitor/Windows acceptance remains pending. |
| entityCulling | PERFORMANCE | OPTIONAL_INTEGRATION | state/screen survival only; functional acceptance pending | No second entity culler is installed. External renderer/culling policy stays under modpack control; saved preferences are retained. Hidden: blockEntities, entities, frames, items, nameTags, occlusion, players. |
| particles | PERFORMANCE | EXPERIMENTAL | output assertions | Shared particle gate before native particle allocation, bounded live counts; no replacement renderer |
| worldDetails | PERFORMANCE | EXPERIMENTAL | state/screen survival only; functional acceptance pending | Native sky, star, weather and atlas animation gates. Fog remains Vanilla; no chunk renderer changes. Hidden: fog. |
| builtinOptimizations | PERFORMANCE | OPTIONAL_INTEGRATION | state/screen survival only; functional acceptance pending | External renderer/optimizers remain separate; no optimization binary is bundled |
| netOptimize | PERFORMANCE | EXPERIMENTAL | state/screen survival only; functional acceptance pending | Shared compression/decryption adapters replace exact Vanilla handlers only. Timing/keepalive packets are observed, never delayed or changed; multiplayer native acceptance remains pending. |
| lowLatency | PERFORMANCE | EXPERIMENTAL | state/screen survival only; functional acceptance pending | Late SDL/mouse polling only with Argentum; TFL GPU fences are bypassed until combined frame-pacing benchmarks exist. Mode preference preserved. Hidden: mode. |
| fastConnect | PERFORMANCE | EXPERIMENTAL | state/screen survival only; functional acceptance pending | Shared DNS cache/prefetch and connection address racing with Vanilla handshake identity. Launcher hints and resource-pack preloads are disabled; native multi-address acceptance remains pending. Hidden: fastSwitch, prePacks. |
| social | CHAT | BACKEND_UNAVAILABLE | state/screen survival only; functional acceptance pending | Requires an authorized TFL backend/launcher integration; saved preferences retained Hidden: corner, dnd, dndFullscreen, duration, hostingDirect, sound, toastInvites, toastMessages, toastOnline, toastRequests, toasts. |
| chatMentions | CHAT | LIMITED | state/screen survival only; functional acceptance pending | Shared mention detection and throttled native sound; whole-line bold highlight. Exact RGB word highlighting is not connected. Hidden: color. |
| chatHeads | CHAT | EXPERIMENTAL | state/screen survival only; functional acceptance pending | Shared sender detection, bounded line cache and native authorized skin/hat textures. Only the first wrapped line gets a head; existing lines update on chat reset. |
| chatFilter | CHAT | LIMITED | output assertions | Shared bounded literal word matching before chat insertion; no outgoing interception |
| autoReconnect | MISC | EXPERIMENTAL | state/screen survival only; functional acceptance pending | Native disconnect countdown and cancel control, shared kick/ban guards and bounded attempts. Old enabled flags require an explicit off/on toggle here. |
| queueAlerts | MISC | LIMITED | state/screen survival only; functional acceptance pending | Shared queue/mention/death notices and native sounds from received chat/action bar. OS taskbar flash is not connected. Hidden: flash. |
| scoreboard | HUD | LIMITED | state/screen survival only; functional acceptance pending | Native 1.8.9 scoreboard/boss/title data, shared HUD placement and editor. Vanilla presentation resumes when disabled. |
| tabPing | HUD | EXPERIMENTAL | state/screen survival only; functional acceptance pending | Native tab-list numeric latency and optional threshold colors; no active ping packet in 1.8.9 |
| bossBar | HUD | LIMITED | state/screen survival only; functional acceptance pending | Native 1.8.9 scoreboard/boss/title data, shared HUD placement and editor. Vanilla presentation resumes when disabled. |
| titles | HUD | LIMITED | state/screen survival only; functional acceptance pending | Native 1.8.9 scoreboard/boss/title data, shared HUD placement and editor. Vanilla presentation resumes when disabled. |
| warnings | HUD | LIMITED | state/screen survival only; functional acceptance pending | Shared threshold/cooldown warnings with real armor/food/health/inventory data and death compass |
| itemCounter | PVP | LIMITED | state/screen survival only; functional acceptance pending | Native inventory icons; 1.8.9 has no totems Hidden: totemPops, totems. |
| hitFeedback | PVP | LIMITED | state/screen survival only; functional acceptance pending | Shared marker/sound on confirmed hits; extra particle emitters not connected Hidden: particles. |
| streamerMode | MISC | LIMITED | output assertions | Masks rendered names and address tokens in common/native text and clipboard. Original server/config/export data remains intact. |
| tooltips | MISC | LIMITED | output assertions | Shared durability, food and filled-map tooltip cards. Shulker boxes do not exist in 1.8.9; compact enchantments are not connected yet. Hidden: enchants, enchantsShift, shulker. |
| serverProfiles | MISC | LIMITED | state/screen survival only; functional acceptance pending | Existing local server-profile matching and shared profile UI |
| panorama | WORLD | EXPERIMENTAL | output assertions | Shared offline cube/equirectangular capture using six native world frames and asynchronous encoding. Temporarily detaches camera/hides HUD, restoring both. No launcher sharing. |
| screenshots | MISC | LIMITED | output assertions | Local gallery/editor and screenshot toast; online sharing, chat actions and third-party preference changes are disabled Hidden: chatActions, replaceEssential. |
| keySearch | MISC | LIMITED | state/screen survival only; functional acceptance pending | Shared search language, conflicts/unbound filters and native keyboard/mouse rebinding. Available through the module action; Vanilla Controls remains available. |
| hungerOverlay | HUD | LIMITED | state/screen survival only; functional acceptance pending | Shared saturation, held-food and legacy health prediction over Vanilla HUD. Exhaustion is not sent by the server and is left unknown. Hidden: exhaustion. |
| circuitLibrary | REDSTONE | EXPERIMENTAL | output assertions | Shared offline library/ghost templates, layers, rotation and block-state checks, with a small bundled GPL recipe set and local cache. Remote submissions and downloads are disabled. |
| notes | WORLD | LIMITED | state/screen survival only; functional acceptance pending | Shared offline notebooks and map coordinate actions; remote sync disabled Hidden: sync. |
| pinnedNote | HUD | LIMITED | state/screen survival only; functional acceptance pending | Shared pinned notebook, checklist and wrapping; offline only |
| cinematic | MISC | EXPERIMENTAL | output assertions | Shared offline keyframes/playback and collision-limited local freecam. Received chunks only. External recording unavailable; multiplayer gameplay acceptance remains pending. Hidden: record. |
| halloween | MISC | LIMITED | state/screen survival only; functional acceptance pending | Local procedural bat hit effects and self ghost trail. Remote event rewards, other-client effects, jumpscares and remote sounds are unavailable. Hidden: jumpscares, others, spookySounds. |
