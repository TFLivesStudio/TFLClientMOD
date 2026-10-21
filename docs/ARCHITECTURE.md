# Architecture principles

TFL Selection is distributed by TFL Client. **TFL Core** is the client-only mod, not the launcher itself.

## Boundaries
- Launcher: downloads and updates approved modpack dependencies, provides version selection.
- Core: client UI, module orchestration, gameplay overlays, local settings.
- Third-party mods: rendering/optimization and other mature implementations, selected per Minecraft version.
- Backend (future): consensual profile sync; no online requirement for normal gameplay.

## Planned internal packages
- `client.ui`: screens, rendering layers, themes.
- `client.module`: lifecycle, config, capabilities and safe module isolation.
- `client.hud`: widgets and editor.
- `client.gameplay`: overlays and permitted client tools.
- `client.performance`: measuring and opt-in performance profiles.
- `client.integration`: Discord and future TFL Client link.

No performance, UX, security or compatibility claims are considered verified until CI and in-game tests exist.
