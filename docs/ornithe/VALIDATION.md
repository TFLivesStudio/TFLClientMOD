# Ornithe Phase 1 — validation (2026-10-24)

Experimental release; Linux x86_64, Temurin 25.0.5, Xvfb, Mesa llvmpipe (LLVM 19.1.7),
software OpenGL. Native game launches, not compilation-only compatibility claims.

## Build and tests

- Independent Ornithe build completed with Gradle 9.7.1 / Loom 1.18.4 / Ploceus 1.18.3.
- Shared core: **1,128 tests; 1,126 passed, 0 failures/errors, 2 skipped**.
- Common production sources also compile with Java 8/Gson 2.2.4; no other game target was built.
- Existing HTTP fixture now explicitly flushes oversized response headers: Java 25 otherwise
  closed its intentionally truncated response before transmitting headers. Production downloader is unchanged.
- Installable archive validation: one `tflcore` client mod, Java 25 classes, Gen2 references,
  GPL/NOTICE/font licenses, no Forge classes, development harness, nested bridge or external mod binaries.

## Actual client matrix

| Stack | In-game checks | Fresh process/preferences | Status |
| --- | --- | --- | --- |
| baseline | 31 passed | 2 passed | passed / passed |
| lwjgl3 | 31 passed | 2 passed | passed / passed |
| argentum | 31 passed | 2 passed | passed / passed |

The tick-driven development harness opens the shared title/settings/editor, verifies return-to-title,
enters a deterministic flat world, toggles all seven modules off/on, captures both CPS hooks,
holds real V/Control input through X11/Robot, checks the native FOV return hook and sprint latch,
verifies disable/release restoration, opens settings in world and checks GUI sprint reset.
A second JVM loads the saved coordinates/crosshair state and crosshair size. JSON reports enumerate
every assertion; screenshots accompany the runtime artifact. The harness is excluded from installable JARs.

## Diagnostic measurements

200 client ticks after warm-up; deterministic flat world, no movement, 854×480, same mod settings.
Measurements are short software-renderer smoke samples, not controlled benchmarks. Heap is a point
sample without forced GC. They establish instrumentation, **not a performance ranking**.

| Stack | Frames | FPS from mean interval | Median ms | P95 ms | Heap MiB |
| --- | ---: | ---: | ---: | ---: | ---: |
| baseline | 257 | 23.50 | 41.11 | 59.43 | 284.2 |
| lwjgl3 | 176 | 16.20 | 56.05 | 97.01 | 224.4 |
| argentum | 283 | 25.55 | 33.88 | 53.68 | 642.3 |

## Compatibility limits

Argentum 1.0.2 initializes its renderer in the real client, builds chunks and enables model instancing;
the seven TFL functions and restart checks pass beside it. Its configuration is not modified.
No proprietary implementation was copied, decompiled or added as a compile dependency.
Legacy LWJGL3 reports its SDL3 backend and LWJGL 3.4.3 runtime.

Known upstream/environment diagnostics: missing legacy Realms/Twitch classes, legacy Log4j console
appender warnings in development, and Java 25 Unsafe deprecation notices from third-party libraries.
These do not prevent the tested sessions. The cloud has no audio device; sound was tested with a null
backend, not speakers. Development remapping can warn about an optional third-party target.

No physical-GPU, Windows, macOS, multiplayer/server-policy, broad modpack, IME or long-session
certification is implied. Vanilla world/server/options screens keep vanilla styling in this phase.
Only seven modules are ported; advanced per-module settings omitted from the PoC UI are not implemented.
The experiment remains a draft PR and a prerelease; Forge is retained.

## Production JAR

The remapped installable JAR was launched separately with Fabric development mode disabled,
Calamus Gen2, OSL 0.22.2 (complete published bundle), Legacy LWJGL3 and Argentum.
Production launch uses Calamus explicitly because Loom’s generic task defaults to Fabric intermediary.
Main menu/Nunito and real Right Shift settings input were visually verified.
Additional production world evidence is recorded alongside the final artifact.

Reproduce with `runProdClient -PtestStack=argentum -PruntimeModsDir=<verified downloads>`.
This uses the release JAR, not development classes. See README.md for the build command.
