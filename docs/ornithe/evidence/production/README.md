# Remapped installable-JAR acceptance

Local `runProdClient -PtestStack=argentum` launched the remapped `.2` JAR with Calamus Gen2,
OSL, Legacy LWJGL3 and Argentum, without development harness classes. This is separate from
Loom's mapped development runtime. Screenshots show the shared title, an integrated world
with native HUD, shared module settings and modern pause navigation.

Linux/Xvfb/llvmpipe, Java 25.0.5, 854×480. Manual input through X11. This is a boot/world/menu
acceptance check, not full per-module production certification or a hardware benchmark.
