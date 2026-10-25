# TFL Client — shared glass UI preview

Development candidate 0.4.0-ui.1; no stable/RC claim. Builds remain platform-specific.

- Pause navigation keeps the world visible. Common native menu backgrounds no longer paint a full-screen in-world color. Dialog windows retain a readability scrim.
- Title Screen settings now expose glass opacity (35–100%), radius (0–16) and accent. The shared panel/button primitives consume them on both targets.
- Keystrokes exposes pressed background/text colors, alpha and chroma; connected in modern Fabric, legacy Forge and the active 1.8.9 adapter. Existing unpressed/background/text controls remain available.
- Minimap's cloud option controls a shared static, softly lit fog underlay on both maps. Only transparent/unmapped areas expose the underlay; it never samples or saves extra terrain. Cached 256² texture, regenerated on resource reload, no volumetric shader or per-frame noise generation.
- Curves use physical-pixel coverage up to 4× GUI scaling; text uses 8× font sampling. This does not enable world MSAA or replace pixel-art item textures.
- Bundled blue 1920×1080, 24 fps black-hole Motion JPEG. Clean installs default to Quality. Existing background-mode preferences are preserved: choose Quality/Balanced in Title Screen settings if an old configuration selected Static. Local `config/tflcore/menu.mjpeg` remains an override. Reduced motion/static/off remain available. Decoder runs off-thread, queues only the latest frame and releases video when entering gameplay.
- Loader implementation branding removed from visible title/status/consent text and current version/metadata. Technical dependency identifiers and license notices remain intact.

The blue clip is a hue-converted version of the user-provided video, with no requested on-screen credit. Source media is not downloaded at runtime.

This is a shared presentation/settings pass, not certification that every existing platform adapter implements every module option. The historical capability audit still applies. Inventory/item textures, third-party mod screens, all resource packs, physical GPUs and multiplayer need separate visual acceptance; do not describe them as fully redesigned or certified by these checks.

## Validation

Both targets compile. Shared suite: 1,138 detected, 1,136 passed, two opt-in skips,
zero failures/errors; target-specific suite: nine passed. Six distribution-tool tests pass.
The real 1.8.9 client completed 214 checks at 1920×1080 / GUI scale 4, including
bundled video presentation, received-world maps and transparent shared pause navigation.
26.3 completed 13 native checks for video presentation, option destinations and persisted
keybinding visibility. The real-world menu run also passed the shared pause and local emote lifecycle
assertions on Vulkan/llvmpipe; screenshots and a summary are included in the evidence folder.

Cloud CI exposed a nested Xvfb launcher and a legacy display-mode discovery failure.
The smoke runner now explicitly owns its display. The X11 shared libraries were already
installed; CI now also installs x11-xserver-utils (the xrandr utility), matching the
existing legacy runtime workflow. Screenshot-watcher checks wait for the actual
asynchronous result with a ten-second deadline rather than assuming 20 ticks equal one
second on a busy software renderer. Neither fix changes gameplay or disables assertions.
