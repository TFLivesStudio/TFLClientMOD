# TFL Client

Cliente de Minecraft en desarrollo, con una interfaz nativa compartida, HUD configurable, vídeo azul y mapas basados en **Diorama**.

| Minecraft | Plataforma | Java | Dependencias |
|---|---|---|---|
| 1.8.9 | Ornithe Gen2 | 25 | Fabric Loader 0.19.5, OSL 0.22.2 |
| 26.3 | Fabric | 25 | Fabric Loader 0.19.5, Fabric API 0.161.0+26.3 |

Argentum 1.0.2 + Legacy LWJGL3 1.4.10 en 1.8.9 y Sodium 0.9.2 en 26.3 son opciones separadas. Sus binarios no se incluyen. Forge y NeoForge se han retirado; el historial de Git conserva las versiones anteriores.

La candidata actual es **0.4.0-audit.3**. Consulta [corrección del mapa, las fuentes y ESC](docs/MAP_PAUSE_FIX.md). Una compilación o una prueba de menú no certifica todos los módulos ni garantiza una tasa de FPS en cualquier equipo.

## Uso

Instala un solo JAR TFL, correspondiente a tu versión y plataforma. Conserva una copia de tus ajustes antes de probar una candidata. `Right Shift` abre la configuración; `M`, el mapa. El editor del HUD permite mover y escalar sus elementos. ESC usa navegación transparente a la izquierda.

El vídeo se distribuye a 30 FPS en 1080p, 720p y 480p. Calidad es el valor inicial; los modos de rendimiento, estático y desactivado son opciones explícitas. La tasa realmente presentada depende del equipo; las métricas quedan en los diagnósticos. No se cambian silenciosamente tus preferencias guardadas.

Capturas: `screenshots/`. Clips locales: `clips/`. La vista previa y edición de MP4 requieren FFmpeg y ffprobe instalados en PATH. No requieren servicios del launcher anterior y no descargan programas. Las ediciones se exportan a archivos nuevos.

## Compilación

Con JDK 25:

```sh
vendor/trs-client/gradlew -p vendor/trs-client :common:test :fabric:26.3:build -PtrsVersions=26.3
vendor/trs-client/ornithe/gradlew -p vendor/trs-client/ornithe assemble test
```

El código activo está en `vendor/trs-client/common`, `vendor/trs-client/fabric`, `vendor/trs-client/ornithe` y `vendor/diorama`. Las rutas Fabric históricas se conservan como referencia de portabilidad; la integración de Diorama y la certificación actual se dirigen a 26.3.

## Licencias

TFL deriva de TRS Client MOD, © Ohev Tamerin, bajo GPL-3.0-only. Diorama, © Dara Adedeji, usa MIT y conserva su licencia y procedencia en [vendor/diorama/UPSTREAM.md](vendor/diorama/UPSTREAM.md). Se mantienen COPYING, los avisos originales y las licencias de las dependencias. Los recursos y paquetes internos `trsclient` se conservan para mantener la compatibilidad de datos.
