# TFL Core 0.1.0-SNAPSHOT — first smoke test

> **Documento histórico.** La guía vigente es [TESTING_0.3.md](TESTING_0.3.md). La 0.3.0-alpha.1 todavía no se ha validado en partida.

**Scope:** Minecraft Java **26.3**, **Fabric**, **Java 25**. This is an initial bootstrap, **not** a finished playable custom client.

## Obtain a verified build
1. Sign in to GitHub with access to the private repository.
2. Open [Actions](https://github.com/TFLivesStudio/TFLClientMOD/actions/workflows/build.yml), choose a **successful** run on `main`.
3. Download the `TFL-Core-Fabric-26.3-...` artifact from the bottom of the run. Unzip it and use the normal distribution JAR (not sources/dev).
4. Never install a JAR from a failed run.

## Install temporarily for testing
1. Prepare a clean **Fabric 26.3** client installation, with Fabric Loader **0.19.5 or newer** and the **Fabric API** for 26.3.
2. Move the TFL Core JAR into that instance's `mods/` folder.
3. Start Minecraft with **Java 25**, confirm TFL Core is listed among loaded mods and open the client main menu.
4. Exit, inspect `logs/latest.log` for `TFL Core 0.1.0-SNAPSHOT initialized` and for any errors.

## What this build is expected to do
- Load as a **client-only Fabric mod**.
- Log initialization.
- Not require any server-side installation, account synchronization or telemetry.

## What this build does NOT do yet
- No customized main menu, 3D backgrounds, Right Shift settings, HUD widgets, PvP features, or performance profiles.
- No automatic modpack installation or optimization mod bundle.
- No Minecraft 1.8.9 support.

## Local builds
Windows: `gradlew.bat clean build`  
Linux/macOS: `./gradlew clean build`

Requires Java 25; Gradle downloads itself via the checked-in Wrapper. The distributable JAR appears in `build/libs/`. Do not install `*-dev.jar` or `*-sources.jar`.

## Bug reporting
Include Minecraft/Fabric/Fabric API versions, Java version, full crash or latest logs (redact tokens, paths, usernames as needed), steps to reproduce, and whether a clean Fabric profile also fails.
