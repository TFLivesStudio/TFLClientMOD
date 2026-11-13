# Pruebas — TFL Core 0.3.0-alpha.1

## Resumen

| Nivel | Estado |
|---|---|
| Compilación limpia | ✅ `BUILD SUCCESSFUL` |
| Pruebas JUnit | ✅ 51 pruebas, 0 fallos, 0 errores |
| Artefactos | ✅ JAR normal + sources |
| Validación en Minecraft | ❌ **No realizada** |

La 0.3.0-alpha.1 se entrega como **alpha compilada y probada automáticamente**. No se ha lanzado un cliente de Minecraft 26.3 con esta build en este entorno, por lo que los riesgos de integración visual y de ciclo de vida siguen abiertos.

## Entorno de compilación

- Minecraft **26.3**
- Fabric Loader **0.19.5**
- Fabric API **0.163.0+26.3**
- Loom **1.18.4**
- Gradle **9.7.1** (wrapper)
- Java **25** (`C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot` en el equipo de compilación)

## Comando

Windows:

```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
.\gradlew.bat clean build
```

Linux/macOS:

```bash
./gradlew clean build
```

Resultado de la última ejecución:

```text
BUILD SUCCESSFUL
```

Artefactos:

```text
build/libs/tfl-core-0.3.0-alpha.1.jar
build/libs/tfl-core-0.3.0-alpha.1-sources.jar
```

## Pruebas automatizadas

| Suite | Pruebas | Qué cubre |
|---|---:|---|
| `TflSettingsTest` | 13 | Normalización, límites, enums, colores, waypoints, widgets, perfiles, telemetría forzada a off, presets, resumen. |
| `SettingsStoreTest` | 12 | Round-trip, debounce, flush, JSON corrupto, schema futuro de solo lectura, migración persistida, export/import, rutas inseguras, tamaño, esquema no soportado, backups, reset, escritura atómica. |
| `SettingsMigratorTest` | 6 | v2 intacto, v1 → v2, mapeo de widgets, claves desconocidas a `legacy`, schema 0, schema no numérico. |
| `ModuleManagerTest` | 13 | Registro, duplicados, orden de dependencias, dependencia ausente, ciclos, cascada, fallo en tick/enable, errores, info, enableAll, ids inválidos. |
| `ProfileManagerTest` | 7 | Aplicación de PVP/SURVIVAL/MINIMAL, detección, CUSTOM, módulos externos, rotación de perfiles. |
| **Total** | **51** | **0 fallos, 0 errores, 0 omitidas** |

Informe local: `build/reports/tests/test/index.html`.

### Lo que NO cubre JUnit

- Renderizado real de pantallas y HUD.
- Inyección del botón en el menú de pausa.
- Reemplazo del `TitleScreen` mediante `client.execute`.
- Reemplazo del crosshair con `HudElementRegistry`.
- Zoom/FOV y restauración de la tecla de sprint en un cliente real.
- Arrastre, hitboxes y teclado del editor de HUD.
- Cambio de idioma en el juego.
- Migración real de un `config/tflcore.json` de 0.2 en una instalación existente.
- Rendimiento (FPS, frametime) en escenas reales.

## Validación manual pendiente

Marca cada punto en un cliente 26.3 real. Hasta que estén todos verificados, la build sigue siendo alpha.

| # | Prueba | Esperado | Estado |
|---|---|---|---|
| 1 | Arranque con Fabric API | El mod carga y el log muestra `TFL Core 0.3.0-alpha.1 initialized`. | Pendiente |
| 2 | Menú principal | Se muestra TFLClient; las 5 tarjetas y el selector de perfil responden. | Pendiente |
| 3 | Volver a Vanilla | Desactivar `customMainMenu` y volver al título restaura el menú vanilla. | Pendiente |
| 4 | Ajustes | Right Shift abre la pantalla; las 6 pestañas cambian sin recortes a 1920×1080 y a 1280×720. | Pendiente |
| 5 | GUI pequeña | Probar 320×240 y 854×480; comprobar que no hay controles inaccesibles. | Pendiente |
| 6 | Temas/acentos | Cambiar tema y acento actualiza menú, ajustes, HUD y editor. | Pendiente |
| 7 | Transiciones | El slider cambia la velocidad de hover; “movimiento reducido” la desactiva. | Pendiente |
| 8 | HUD | Activar/desactivar widgets; F1 oculta el HUD; “ocultar en pantallas” funciona. | Pendiente |
| 9 | Editor HUD | Arrastrar, imán, anclas, `+`/`-`, `B`, `G`, `R`, `Tab`, `Del`, clic derecho. | Pendiente |
| 10 | Escala GUI | Cambiar escala de Minecraft y confirmar que anclas/offsets se mantienen. | Pendiente |
| 11 | Crosshair | 5 estilos, color, escala, ocultar en tercera persona, restaurar vanilla al desactivar. | Pendiente |
| 12 | Toggle sprint | Avanzar mantiene sprint; agacharse, abrir pantalla o subir a un vehículo lo suelta. | Pendiente |
| 13 | Zoom | Mantener C aplica el FOV; soltar restaura exactamente el FOV original; suavizado on/off. | Pendiente |
| 14 | Waypoints | B crea punto; aparece distancia/rumbo; se filtra por mundo y dimensión; máximo 256. | Pendiente |
| 15 | Muerte | Al morir se crea marcador (si está activo), máximo 32. | Pendiente |
| 16 | Teclas | Rebindear, cancelar con Escape, detectar conflicto, reiniciar teclas. | Pendiente |
| 17 | Perfiles | PVP/SURVIVAL/BUILDER/EXPLORATION/MINIMAL cambian módulos y widgets; CUSTOM no se aplica desde el ciclo. | Pendiente |
| 18 | Diagnóstico | F9 muestra FPS, percentiles, RAM, mods, estado y errores sin I/O por frame. | Pendiente |
| 19 | Benchmark | `/tflcore bench 10` termina y registra avg/p50/p95/p99 en el log. | Pendiente |
| 20 | Import/export | Exportar, editar, importar; rechazar rutas fuera de `config`, archivos > 1 MiB y schema futuro. | Pendiente |
| 21 | Migración 0.2 | Copiar un `tflcore.json` v1 real; se crea backup, se migra a v2 y se conserva `legacy`. | Pendiente |
| 22 | Corrupción | Archivo JSON inválido: se usan defaults, se conserva el original y se avisa. | Pendiente |
| 23 | Idioma | Cambiar a `es_es` y `es_mx`; no deben aparecer claves literales. | Pendiente |
| 24 | Cierre | Salir del juego con zoom/sprint activos; las opciones temporales se restauran. | Pendiente |

## Benchmark

El benchmark no afirma mejoras de FPS. Es una herramienta de captura:

1. Ejecuta `/tflcore bench 30` en una escena estable (mismo mundo, misma posición, misma hora, mismos ajustes).
2. No muevas la cámara ni abras menús durante la captura.
3. Repite 3 veces y compara media, p50, p95 y p99.
4. Compara solo ejecuciones en el mismo equipo, resolución, escala GUI, versión de Minecraft y mods.

Los resultados aparecen en el log y en `/tflcore diag`. **No hay datos de comparación publicados en esta entrega** porque no se ha ejecutado una partida real.

## Riesgos conocidos de la alpha

1. **Integración de pantallas:** el reemplazo del título y la inyección del botón de pausa usan APIs de Fabric que no se han probado en un cliente real.
2. **Editor de HUD:** los hitboxes y el arrastre pueden necesitar ajustes tras la primera prueba visual.
3. **Zoom/sprint:** no se ha comprobado la interacción con otros mods que también modifican FOV o la tecla de sprint.
4. **GUI pequeña:** en alturas < 300 px algunas pestañas de ajustes pueden recortarse.
5. **CPS:** se muestrea por frame; con FPS muy bajos puede subestimar clics.
6. **Waypoints:** no hay render 3D en el mundo; solo guía en HUD.
7. **Migración real:** las pruebas usan JSON sintético, no un archivo generado por una partida 0.2.

## CI

`.github/workflows/build.yml` ejecuta `./gradlew --no-daemon --stacktrace clean build` en Ubuntu con Java 25, valida el wrapper, publica el informe de pruebas y sube el JAR de producción. Un JAR de una ejecución fallida no debe instalarse.
