# TFL Core 0.2.0-SNAPSHOT — First interactive alpha

## Environment
- Minecraft Java **26.3**; Java **25**.
- Fabric Loader **0.19.5+** and Fabric API matching 26.3.
- Use a separate clean profile without other UI/render mods for the first pass.
- Install exactly one TFL Core JAR (never the sources or dev JAR).
- Back up the `config/tflcore.json` file before comparing profiles.

## Test order and expected behavior

| ID | Test | Expected behavior |
|---|---|---|
| A01 | Start Fabric in a clean profile | Loads without startup crash; TFL Core logged as 0.2.0-SNAPSHOT |
| A02 | Return to main menu | Original Minecraft title screen replaced by TFLClient |
| A03 | Click Un jugador | Vanilla singleplayer world list opens |
| A04 | Click Multijugador | Vanilla multiplayer server list opens |
| A05 | Click Opciones de Minecraft | Standard Minecraft settings open |
| A06 | Click TFL Core / Configuracion | TFL Core settings appear with General, HUD, Perfiles tabs |
| A07 | Switch BLUE → MIDNIGHT → FROST | Backdrop/theme colors change and persist after restart |
| A08 | Disable animated background | Moving light bars stop |
| A09 | Enable reduced motion | Decorative animations stop even if background option enabled |
| A10 | Enter a world and press Right Shift | Settings screen opens (rebind via Minecraft Controls if needed) |
| A11 | Toggle FPS / coordinates / clock independently | Enabled items appear in TFL HUD; disabling removes each item |
| A12 | Move HUD with profile controls | HUD shifts horizontally/vertically, values saved |
| A13 | Select FPS preset | TFL decorative animation disabled; external performance mods not changed |
| A14 | Select QUALITY preset | TFL menu effects enabled without changing external mods |
| A15 | Disable custom main menu | Minecraft title screen remains vanilla after next opening |
| A16 | Restart game | All chosen settings persist in `config/tflcore.json` |
| A17 | Disconnect from a multiplayer server | No server-side mod or plugin required |
| A18 | Press F1, resize window, change GUI scale | Observe HUD/UI clipping, input and positioning |
| A19 | Use with selected optimization mods | Verify no rendering or keybinding conflicts |

## Known limitations
- The animated menu is a **lightweight procedural 2D transition**, NOT yet a live 3D Minecraft scene. True 3D requires a separate renderer with performance budgeting and fallbacks.
- HUD currently supports FPS, coordinates, and optional device clock; advanced draggable editor and other promised HUD modules are **not implemented** yet.
- FPS/QUALITY presets currently affect **TFL Core visuals** only. They do not change Sodium, graphics drivers or Minecraft options.
- Right Shift is registered in the vanilla keybindings. Some GUI screens may consume input; test the key in-world first.
- The 1.8.9 client is planned separately.
- Synchronization, Discord, telemetry collection, servers, accounts and network traffic are **not implemented**.
- Compile success is not a replacement for in-game UI testing.

## Report
Send the build link, MC/Loader/Fabric API/Java versions, relevant `latest.log` lines, exact test ID, a screenshot if UI differs from expectations, and whether the issue reproduces on a clean profile. Please redact tokens and personally identifying file paths.
