# TFL Selection · TFL Core

[![Fabric 26.3 Build](https://github.com/TFLivesStudio/TFLClientMOD/actions/workflows/build.yml/badge.svg)](https://github.com/TFLivesStudio/TFLClientMOD/actions/workflows/build.yml)

Cliente oficial de Minecraft de **TFLives**, distribuido mediante **TFL Client**.

## Estado
**0.2.0-SNAPSHOT**, alpha experimental. Objetivo inicial: **Minecraft Java 26.3 / Fabric / Java 25**. Se abordará **Minecraft 1.8.9** después de validar la versión moderna.

### Incluido en 0.2
- Menú principal TFL Selection con enlaces a modos de juego y configuración.
- Personalización con BLUE, MIDNIGHT, FROST; efecto de fondo de bajo coste y reducción de movimiento.
- Configuración local persistente en `config/tflcore.json`, sin cuentas ni telemetría.
- Acceso a configuración con Right Shift y botones dentro del menú.
- HUD inicial de FPS, coordenadas y reloj opcional.
- Perfiles de efectos **internos del Core** (BALANCED/FPS/QUALITY); no se promete modificar ajustes gráficos de otros mods.

### Pendiente
- Escenas 3D reales, HUD arrastrable, widgets avanzados, integraciones con optimizadores y TFL Client, port 1.8.9.

## Compilación
Windows: `gradlew.bat clean build`  
Linux/macOS: `./gradlew clean build`

Requiere **Java 25**. GitHub Actions publica el `.jar` en cada compilación satisfactoria. Para uso dentro de Minecraft instala la compilación normal, no `-sources.jar` ni `-dev.jar`.

[Pruebas de la versión 0.2](docs/TESTING_0.2.md) · [Arquitectura](docs/ARCHITECTURE.md) · [Roadmap](docs/ROADMAP.md)
