# Changelog

Todas las fechas usan el formato AAAA-MM-DD. Las versiones alpha pueden cambiar ids o formato de configuración; los cambios destructivos se documentan aquí.

## [0.3.0-alpha.1] — 2026-10-21 — PROJECT OVERDRIVE: fundación

### Añadido

- **Sistema de módulos**: `TflModule`, `ModuleManager`, categorías, estados, dependencias, detección de ciclos, desactivación en cascada y aislamiento de fallos con errores visibles.
- **Módulos reales**: `crosshair`, `toggle-sprint`, `zoom`, `waypoints`.
- **Configuración schema v2** con secciones tipadas, normalización estricta y `legacy` para claves desconocidas.
- **Migración v1 → v2** con backup, conservación de datos y escritura diferida del esquema nuevo.
- **SettingsStore** con debounce de 350 ms, escritura atómica, backups, recuperación de JSON corrupto, modo solo lectura para esquemas futuros e import/export restringido a `config/` (1 MiB, validación de esquema).
- **Perfiles** PVP, SURVIVAL, BUILDER, EXPLORATION, MINIMAL y detección CUSTOM.
- **Temas** BLUE, MIDNIGHT, FROST, EMBER, MONO; acentos DEFAULT, CYAN, VIOLET, AMBER, LIME, ROSE.
- **Alto contraste**, transparencia reducida, movimiento reducido y duración de transición configurable.
- **Menú principal rediseñado** con tarjetas, selector de perfil, estado y animación de bajo coste.
- **Pantalla de ajustes** con 6 pestañas, búsqueda/paginación del HUD, sliders, rebinding, import/export y reinicio en dos pasos.
- **Editor de HUD** con arrastre, imán, anclas, escala, fondo, teclado y placeholders.
- **HUD de 19 widgets** en un único elemento Fabric con caché por intervalo:
  FPS, frametime, coordenadas, dirección, bioma, reloj local, hora del mundo, CPS, keystrokes, ping, movimiento, velocidad, armadura, pociones, objeto en mano, bloque apuntado, chunk, waypoint y rendimiento.
- **Crosshair personalizado** con 5 estilos, color, escala y opción de tercera persona, sin mixin.
- **Toggle sprint** que refleja la tecla de sprint sin enviar paquetes.
- **Zoom** con FOV configurable, suavizado y restauración exacta de la opción.
- **Waypoints** locales por mundo/servidor y dimensión, marcador de muerte (máx. 32) y límite total de 256.
- **Diagnóstico** con FPS, frametime (media, min, max, p50/p95/p99), memoria, mods, módulos y estado del archivo.
- **Benchmark** temporizado (`/tflcore bench [segundos]`) con percentiles.
- **Comandos** `/tflcore diag|hud|profile|waypoint|bench|export`.
- **i18n**: `en_us`, `es_es`, `es_mx` (168 claves cada uno).
- **Pruebas JUnit**: 51 pruebas en 5 suites (configuración, store, migrador, módulos, perfiles).

### Cambiado

- Versión `0.2.0-SNAPSHOT` → `0.3.0-alpha.1`.
- El HUD 0.2 monolítico (`TflHud`) se sustituye por `HudManager` + 19 widgets.
- La configuración schema v1 se migra a schema v2; el archivo nuevo se guarda en el siguiente tick.
- El estado de los módulos es la fuente de verdad; `pvp.toggleSprint` y `survival.zoom` quedan como espejo heredado.
- El perfil MINIMAL ya no contiene módulos; CUSTOM solo se detecta y no se aplica desde el ciclo.
- `telemetryConsent` se fuerza a `false` en cada normalización.
- Los ajustes de solo lectura por esquema futuro ya no se sobrescriben.

### Corregido

- Migración repetida: tras migrar, los ajustes se marcan dirty para persistir schema 2 y no volver a crear backups en cada arranque.
- Cancelar un rebind con Escape ya no muestra “tecla actualizada”.
- La pestaña Avanzado cachea ruta y número de backups en lugar de consultar el disco cada frame.
- Acciones de import/export/reset se compactan en tres columnas en ventanas bajas.
- La transición configurada se aplica de verdad a botones/toggles; el movimiento reducido la desactiva.
- La transparencia reducida hace opacas las superficies de panel.
- Los módulos con fallo se desactivan sin tumbar el cliente.

### Validación

- `.\gradlew.bat clean build`: **BUILD SUCCESSFUL**.
- JUnit: **51 pruebas, 0 fallos, 0 errores**.
- JAR: `build/libs/tfl-core-0.3.0-alpha.1.jar`.
- **Sin validación en Minecraft 26.3.** Consulta `docs/TESTING_0.3.md`.

### Limitaciones conocidas

- No hay escenas 3D, skins, minimapa, combo, cooldown de ataque, resaltado de bloques, contador de área, herramientas de inventario/redstone ni integración con Discord/launcher.
- Sin layouts de HUD por perfil.
- Opacidad y compacto por widget se editan en JSON; el editor expone escala y fondo.
- En alturas GUI < 300 px algunas pestañas de ajustes pueden recortarse.
- El CPS depende del ritmo de frames.
- No hay mediciones de rendimiento publicadas.

## 0.4.0-dev.1 — Operation Modpack Ready (development only)

- Adopt TRS common/Fabric/legacy as the only active build; retain original TFL sources as migration reference.
- Use loader ID tflcore with explicit conflict against original trsclient, retaining internal resource namespaces.
- Disable TRS/backend/account/launcher integrations; retain offline cape physics and client modules.
- Protect future configurations, preserve unknown keys, back up and import TFL schema v2/TRS data into separate paths.
- Introduce shared Selection controls, native Inter on Fabric and a static original black-hole menu background.
- Add distribution verification, hashes, real registry/dependency inventories and a local mrpack packager.
- Add migration/privacy/fallback/packaging tests and correct omitted pinned circuit test fixtures.
- No stable release, no video decoder and no claim of full in-game or optimization-pack certification.

## 0.4.0-dev.2 — TFL Client Final Experience

- Identidad pública TFL Client, Nunito local (Fabric nativo y atlas Forge) y navegación izquierda.
- Geometría más fina, sliders/pausa Fabric, recarga de recursos y primer inicio sin introducción redundante.
- Seis emotes locales independientes, cancelación/cámara y pruebas reales.
- Reproductor local Motion JPEG en worker, sin audio/descargas; clip cinematográfico final pendiente.
- Release preliminar de ambos targets con fuentes y hashes; sin merge a main ni certificación stable.
