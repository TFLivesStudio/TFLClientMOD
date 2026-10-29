# Diorama adoption

Source: https://github.com/SunkenInTime/diorama
Pinned commit: `4ce572ea5ceac7974fe2bb1b344c52ff2e55acd4`
License: MIT, Copyright Dara Adedeji. Original notices retained.

TFL modifications: integrated into the existing client entrypoint/build; source-level 26.3 adapter; visual identity and settings bridge. Upstream test sources remain reference only. No separate third-party binary download is required.

The `ornithe/` subtree ports the original projection, voxel reader, ray tracer, mipmap and fog algorithms to 1.8.9. Legacy native bounds are copied on the client thread; connected shapes (fences, walls, inner/outer stairs) are approximated. TFL supplies per-world snapshot storage and GUI/HUD adapters. Modern optional integrations remain upstream code; they are not certified by TFL's smoke scenario.
