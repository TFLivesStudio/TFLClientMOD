# Matriz de funcionalidades — TFL Core 0.3.0-alpha.1

**Leyenda de estado**

- ✅ **Implementado**: existe código funcional y conectado a la UI o al ciclo del cliente.
- 🟡 **Parcial**: hay una parte útil, pero falta el alcance completo descrito en la misión.
- ❌ **No implementado**: no existe en esta entrega.

**Validación**

- **C** — compila dentro de `.\gradlew.bat clean build`.
- **J** — cubierto por pruebas JUnit automatizadas.
- **G** — probado en una partida real de Minecraft 26.3. **Ninguna fila tiene G en esta entrega.**

> Esta matriz es deliberadamente conservadora. Un ✅ significa “implementado y compilado”, no “validado en juego”. La validación en partida sigue pendiente para toda la 0.3.0-alpha.1.

## Arquitectura

| ID | Funcionalidad | Estado | Validación | Notas |
|---|---|---|---|---|
| A01 | Módulos con id, categoría, activar/desactivar/estado | ✅ | C+J | `TflModule`, `ModuleState`, `ModuleManager`. |
| A02 | Registro con orden, dependencias y detección de ciclos | ✅ | C+J | 13 pruebas de `ModuleManager`. |
| A03 | Código desacoplado por dominio | ✅ | C | Paquetes `config`, `module`, `hud`, `ui`, `pvp`, `survival`, `keybind`, `profile`, `command`, `compat`. |
| A04 | Preferencias tipadas, schema v2 y migración | ✅ | C+J | `TflSettings`, `SettingsMigrator`; claves v1 desconocidas → `legacy`. |
| A05 | Guardado atómico, debounced y backups | ✅ | C+J | 12 pruebas de `SettingsStore`; sin escrituras por frame. |
| A06 | Perfiles de módulos | ✅ | C+J | PVP, SURVIVAL, BUILDER, EXPLORATION, MINIMAL, CUSTOM. |
| A07 | Perfil asignado por mundo/servidor | ❌ | — | El perfil es global por instalación. |
| A08 | Importar/exportar perfiles completos | 🟡 | C+J | Import/export de **ajustes** con validación; no hay archivos por perfil. |
| A09 | Gestor central de teclas, conflictos y reinicio | ✅ | C | `KeybindManager`; detección de conflictos contra vanilla/mods. |
| A10 | Modo de recuperación / safe mode | 🟡 | C | Fallos aislados, HUD desactivable y ajustes de solo lectura; no hay botón “volver a Vanilla”. |
| A11 | Error visible por módulo | ✅ | C+J | `ModuleError` en diagnóstico y comando `/tflcore diag`. |
| A12 | i18n inglés/español | ✅ | C | 168 claves en `en_us`, `es_es` y `es_mx`, JSON validado. |

## Visual / UI

| ID | Funcionalidad | Estado | Validación | Notas |
|---|---|---|---|---|
| V01 | Menú principal propio | ✅ | C | Tarjetas de modo, perfil, estado y pie de versión. |
| V02 | Sistema de diseño único | ✅ | C | Tokens en `TflTheme`; botones, toggles y sliders propios. |
| V03 | Temas y acentos configurables | ✅ | C | 5 temas × 6 acentos; tema personalizado importable: ❌. |
| V04 | Transiciones configurables | ✅ | C | `TflMotion` + slider 0–400 ms; `reducedMotion` las desactiva. |
| V05 | Escalado y layout responsive | 🟡 | C | Menú compacto, pestañas en 2 filas y HUD paginado; en alturas < 300 px algunas pestañas pueden recortarse. |
| V06 | Acceso rápido a ajustes | ✅ | C | Right Shift + botón en pausa + tarjeta del menú. |
| V07 | Integración con menú de pausa | ✅ | C | Botón “TFL Core” añadido al `PauseScreen`. |
| V08 | Pantalla de ajustes completa | ✅ | C | 6 pestañas, búsqueda, reinicio en dos pasos, import/export. |
| V09 | Previsualización y autoguardado | ✅ | C | Editor en vivo; guardado debounced. |
| V10 | Onboarding | 🟡 | C | Aviso único con teclas al entrar al primer mundo; no hay tour paso a paso. |
| V11 | Fondos estáticos de calidad | 🟡 | C | Gradiente + cuadrícula; no hay imágenes personalizadas. |
| V12 | Escenas 3D / skins | ❌ | — | Fuera de esta alpha. |
| V13 | Sistema de skins de cliente | ❌ | — | Fuera de esta alpha. |

## HUD

| ID | Funcionalidad | Estado | Validación | Notas |
|---|---|---|---|---|
| H01 | Widgets útiles | ✅ | C | 19 widgets registrados. |
| H02 | Editor arrastrable con preview | ✅ | C | Arrastre, selección, placeholders y botones. |
| H03 | Imanes, anclas y offsets | ✅ | C | Cuadrícula configurable, 7 anclas, escala y fondo. |
| H04 | Opacidad/fondo/modo compacto por widget | 🟡 | C | Opacidad y fondo se renderizan; el editor expone fondo y escala, no opacidad; “compact” solo lo usa coordenadas y se edita en JSON. |
| H05 | Layouts independientes por perfil | ❌ | — | Los perfiles cambian el conjunto de widgets, no su posición. |
| H06 | FPS | ✅ | C | Widget `fps`. |
| H07 | Frametime y percentiles | ✅ | C | Widget frametime (media/p95); diagnóstico y benchmark con p50/p95/p99. |
| H08 | CPS izquierdo/derecho | ✅ | C | Derivado del estado de los botones; límite por FPS documentado. |
| H09 | Keystrokes | ✅ | C | WASD, salto, agacharse y sprint. |
| H10 | Ping online / N/A offline | ✅ | C | Entrada del jugador local; `N/A` sin conexión. |
| H11 | Coordenadas configurables/compactas | 🟡 | C | Conversión Nether, modo streamer y compacto por JSON; sin editor numérico de coordenadas. |
| H12 | Dirección y bioma | ✅ | C | Widgets separados. |
| H13 | Reloj local y hora del mundo | ✅ | C | Hora local y día/hora del mundo. |
| H14 | Sprint/sneak/walk | ✅ | C | Widget `sprint`. |
| H15 | Armadura con durabilidad | ✅ | C | 4 piezas + porcentaje. |
| H16 | Pociones con duración | ✅ | C | Hasta 5 efectos, nivel y mm:ss. |
| H17 | Objeto en mano con cantidad/duración | ✅ | C | Nombre, cantidad y barra de durabilidad. |
| H18 | Cooldown / indicador de ataque | ❌ | — | No implementado. |
| H19 | Ocultar en F1/pantallas/streamer | 🟡 | C | F1 y pantallas sí; streamer oculta coordenadas y ping; no hay modo específico para capturas. |
| H20 | Trabajo acotado y cacheado | ✅ | C | Un elemento HUD, intervalos por widget, sin I/O por frame. |

## PvP

| ID | Funcionalidad | Estado | Validación | Notas |
|---|---|---|---|---|
| C01 | Crosshair personalizado | ✅ | C | 5 estilos, color, escala, tercera persona; sin mixin. |
| C02 | Toggle sprint | ✅ | C | Refleja la tecla de sprint; no automatiza paquetes. |
| C03 | Indicador de combo | ❌ | — | No implementado. |
| C04 | Color/feedback de impacto | ❌ | — | Campos `hitColor`/`hitFlash` reservados, sin render. |
| C05 | Partículas/sonidos de impacto | ❌ | — | No implementado. |
| C06 | Objeto, cooldown y estadísticas de sesión | 🟡 | C | Objeto en mano sí; cooldown y estadísticas no. |
| C07 | HUD de combate editable y perfil PvP | ✅ | C | Editor + conjunto PVP. |
| C08 | Sin cheats ni paquetes | ✅ | C | Solo APIs y estado local. |

## Supervivencia / exploración

| ID | Funcionalidad | Estado | Validación | Notas |
|---|---|---|---|---|
| S01 | Zoom configurable | ✅ | C | FOV 5–90°, suavizado, restauración exacta. |
| S02 | Waypoints persistentes por mundo/dimensión | ✅ | C+J | Guardado local; pruebas de filtrado/normalización en `TflSettingsTest`. |
| S03 | Waypoint de muerte | ✅ | C | Máximo 32 marcadores automáticos. |
| S04 | Brújula, distancia y orientación | ✅ | C | Widget con nombre, distancia y rumbo. |
| S05 | Bloque apuntado | ✅ | C | Bloque, coordenadas y emisión de luz del bloque. |
| S06 | Overlay de chunk | 🟡 | C | Widget con chunk/dimensión; sin overlay de límites. |
| S07 | Nivel de luz | 🟡 | C | Solo emisión del bloque apuntado; no nivel de luz del jugador. |
| S08 | Distancia entre dos puntos | ❌ | — | No implementado. |
| S09 | Contador de coordenadas/área | ❌ | — | No implementado. |
| S10 | Resaltado de bloques | ❌ | — | No implementado. |
| S11 | Minimapa | ❌ | — | No implementado (y no se promete rendimiento de minimapa). |
| S12 | Herramientas de inventario | ❌ | — | No implementado. |
| S13 | Herramientas de redstone | ❌ | — | No implementado. |

## Rendimiento y diagnóstico

| ID | Funcionalidad | Estado | Validación | Notas |
|---|---|---|---|---|
| P01 | Presets BALANCED/FPS/QUALITY/CUSTOM | ✅ | C | Solo afectan a TFL Core; no tocan opciones de otros mods. |
| P02 | Bajo consumo verificable | 🟡 | C | Diseño cacheado y sin I/O por frame; falta medición en partida. |
| P03 | Diagnóstico de FPS/frametime/RAM | ✅ | C | `TflDiagnosticsScreen` con media, min/max, p50/p95/p99 y memoria. |
| P04 | Detección de mods | ✅ | C | `ModDetector` informativo. |
| P05 | Matriz de compatibilidad con versiones | 🟡 | C | Versión de MC/loader y presencia de mods; no versión exacta de cada mod. |
| P06 | Benchmark reproducible | 🟡 | C | Captura temporizada con percentiles; no hay datos comparativos publicados. |
| P07 | Fallback ante conflictos | 🟡 | C | Aislamiento de fallos; no hay detección automática de conflictos gráficos. |
| P08 | Panel de estado y errores | 🟡 | C | Versión, loader, MC, módulos, HUD, almacenamiento y errores; faltan botones “abrir logs” y “exportar informe”. |
| P09 | Integración con Discord | ❌ | — | No implementado. |
| P10 | Manifiesto para el launcher | ❌ | — | No implementado en este repositorio. |
| P11 | Telemetría desactivada | ✅ | C+J | `telemetryConsent` se fuerza a `false` en `normalize()`. |

## Resumen

- **Implementado:** 45 funcionalidades.
- **Parcial:** 16 funcionalidades.
- **No implementado:** 16 funcionalidades.
- **Validación en juego:** 0 funcionalidades.

Las cifras son una guía de alcance, no una puntuación de calidad. La prioridad de la siguiente alpha es validar en partida lo ya implementado antes de añadir nuevas funciones.
