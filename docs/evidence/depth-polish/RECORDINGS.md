# Native recordings

- `native-189-baseline.mp4`: 105 seconds of the actual 1.8.9 baseline client running the production-JAR probe, recorded at 8 fps. Includes title playback and automated world/model/map/camera/effect tests, with loading and intentional panorama/effect transitions.
- `native-263-title.mp4`: 32 seconds of the actual 26.3 production client title test, captured after moving its native window to the origin of the X display. Includes moving blue footage and explicit preset changes.

These are recordings, not animations synthesized from screenshots. Mesa/llvmpipe software rendering was used. The separate background ROI audit verifies visible changes without using menu hover or FPS/status text as evidence. Model/map screenshots and CI reports complement the recordings.

Production source commit: `540cc243b29746bcd1ebf0cf9e212eb044b82a0a`. The evidence-only archive commit does not change the production JARs.
