# Guía de usuario — TFL Core 0.3.0-alpha.1

Esta guía describe la build **0.3.0-alpha.1**. Es una alpha: compila y pasa pruebas automáticas, pero **todavía no se ha validado en una partida real**. Si algo no coincide con lo descrito aquí, revisa primero la sección de limitaciones.

## Instalación

1. Instala **Minecraft Java 26.3** con **Fabric Loader 0.19.5** o superior.
2. Añade **Fabric API 0.163.0+26.3** (o la versión compatible con 26.3) a `mods/`.
3. Copia `tflcore-0.3.0-alpha.1.jar` a `mods/`.
4. Inicia el juego con **Java 25**.
5. Comprueba en el log:

```text
TFL Core 0.3.0-alpha.1 initialized.
```

No instales `-sources.jar` ni `-dev.jar`.

## Controles por defecto

| Acción | Tecla | Tipo |
|---|---|---|
| Abrir ajustes TFL Core | **Right Shift** | Pulsación |
| Editor de HUD | **H** | Pulsación (solo en mundo) |
| Zoom | **C** | Mantener |
| Añadir waypoint | **B** | Pulsación (solo en mundo) |
| Diagnóstico | **F9** | Pulsación |

Todas se pueden cambiar en **Ajustes → Teclas** o en los controles de Minecraft. La pantalla de teclas muestra si una tecla choca con otro atajo; **no bloquea** el cambio, solo lo advierte. Escape cancela la captura.

El botón **TFL Core** también aparece en el menú de pausa.

## Menú principal

Cuando `customMainMenu` está activo, TFL Selection reemplaza el título vanilla:

- **Un jugador**, **Multijugador**, **TFL Core**, **Opciones** y **Salir**.
- Selector de perfil en la parte inferior: PVP → SURVIVAL → BUILDER → EXPLORATION → MINIMAL → PVP.
- El panel de marca muestra el perfil actual, módulos activos, widgets activos y tema.
- En ventanas pequeñas se usa un layout compacto sin subtítulos.

Si algo va mal, desactiva **Menú personalizado** en General o edita `config/tflcore.json` con `"customMainMenu": false`.

## Ajustes

La pantalla tiene seis pestañas.

### General
- HUD activado/desactivado.
- Menú personalizado.
- Fondo animado y movimiento reducido.
- Modo streamer (oculta coordenadas y ping).
- Cambiar perfil.
- Cambiar preset de rendimiento (BALANCED → FPS → QUALITY → BALANCED).
- Abrir editor de HUD y diagnóstico.

### Apariencia
- Tema: BLUE, MIDNIGHT, FROST, EMBER, MONO.
- Acento: DEFAULT, CYAN, VIOLET, AMBER, LIME, ROSE.
- Duración de transición (0–400 ms).
- Alto contraste.
- Transparencia reducida.
- Fondo animado y movimiento reducido.
- Reiniciar apariencia.

### HUD
- Interruptor global del HUD.
- Buscador de widgets y paginación.
- Lista de los 19 widgets con su id.
- Reiniciar layout.
- Abrir el editor.

La lista se pagina automáticamente. En alturas GUI muy pequeñas (< 300 px) algunas pestañas pueden recortarse; usa una resolución/escala mayor.

### Juego
- Crosshair personalizado: activar/desactivar, estilo (CROSS, DOT, T, CIRCLE, SQUARE), escala y color.
- Toggle sprint.
- Zoom: activar/desactivar, FOV objetivo (10–90°) y suavizado.
- Waypoints de muerte.

### Teclas
- Rebindear las cinco acciones.
- Ver conflictos con teclas de vanilla u otros mods.
- Reiniciar todas las teclas de TFL Core.

### Avanzado
- Activar/desactivar cada módulo real (`crosshair`, `toggle-sprint`, `zoom`, `waypoints`).
- Exportar ajustes a `config/tflcore-export.json`.
- Importar desde `config/tflcore-import.json`.
- Reiniciar **todos** los ajustes (requiere dos clics en menos de 4 segundos).
- Estado del archivo: esquema, ruta, backups, solo lectura/corrupto/migrado.

## Editor de HUD

Ábrelo con **H** en un mundo o desde Ajustes.

| Acción | Cómo |
|---|---|
| Seleccionar | Clic izquierdo sobre un widget o en el panel superior. |
| Mover | Arrastrar con clic izquierdo. |
| Alternar widget | Clic derecho sobre él. |
| Nudge | Flechas; con Shift usa la cuadrícula. |
| Escala | `+` / `-` (50%–300%). |
| Ancla | `A` cicla entre las 7 anclas. |
| Fondo | `B`. |
| Cuadrícula/imán | `G` o botón. |
| Reiniciar posición | `R`. |
| Siguiente widget | `Tab`. |
| Desactivar | `Del` o `Backspace`. |
| Salir | Escape o Done. |

Los widgets desactivados o no disponibles se muestran como placeholders para poder colocarlos sin activarlos. Los cambios se guardan con debounce.

## Módulos

| Id | Nombre | Por defecto | Qué hace |
|---|---|---|---|
| `crosshair` | Crosshair | Activado | Reemplaza el crosshair vanilla por uno configurable. |
| `toggle-sprint` | Toggle sprint | Desactivado | Mantiene sprint mientras avanzas. |
| `zoom` | Zoom | Activado | Zoom con FOV configurable al mantener C. |
| `waypoints` | Waypoints | Activado | Puntos locales y marcador de muerte. |

**Notas importantes**

- `crosshair` sigue además el ajuste `pvp.customCrosshair`. Si migras desde 0.2, se desactiva para conservar el aspecto vanilla.
- `toggle-sprint` solo refleja la tecla de sprint; no envía paquetes. Si otro mod gestiona sprint, pueden interferir.
- `zoom` guarda y restaura el FOV exacto; si sales al menú o cambias de mundo, se restaura.
- `waypoints` no dibuja geometría 3D en el mundo; solo guía en el HUD.

## Waypoints

- **B** crea un waypoint en tu posición actual (nombre por defecto).
- Los waypoints se filtran por **mundo/servidor** y **dimensión**.
- El widget `waypoint` muestra nombre, distancia y rumbo.
- Al morir se crea un marcador rojo si `deathWaypoint` está activo (máximo 32).
- Máximo total: 256 waypoints.
- Los datos quedan en `config/tflcore.json`; no se envían al servidor.

Comandos:

```text
/tflcore waypoint add [nombre]
/tflcore waypoint list
/tflcore waypoint remove <índice>
```

El índice es el que muestra `list` (empieza en 1).

## Presets y perfiles

**Presets** (BALANCED/FPS/QUALITY) solo cambian el comportamiento visual de TFL Core: fondo animado, movimiento reducido, transparencia y HUD. **No** modifican Sodium, Iris ni opciones gráficas de otros mods.

**Perfiles** cambian módulos y widgets:

| Perfil | Módulos | Enfoque |
|---|---|---|
| PVP | crosshair, toggle-sprint, zoom, waypoints | Combate |
| SURVIVAL | zoom, waypoints | Supervivencia general |
| BUILDER | waypoints | Construcción |
| EXPLORATION | zoom, waypoints | Exploración |
| MINIMAL | ninguno | Sin módulos opcionales |
| CUSTOM | — | Detectado automáticamente; no se aplica desde el ciclo. |

Los perfiles no guardan posiciones de HUD independientes.

## Diagnóstico y benchmark

**F9** abre el diagnóstico:

- FPS, muestras, media, p50/p95/p99, mínimo y máximo.
- Memoria usada/máxima.
- Versión de Minecraft, Fabric Loader y Java.
- Módulos y widgets activos.
- Mods detectados (informativo).
- Estado del archivo, backups y errores de módulos.

Botones: iniciar benchmark, reiniciar muestras, abrir ajustes y cerrar.

Benchmark:

```text
/tflcore bench 30
```

Duración 3–120 s. Al terminar, el resultado (media, p50, p95, p99 y muestras) se escribe en el log. No se publican comparativas de FPS sin mediciones reales.

## Comandos

| Comando | Efecto |
|---|---|
| `/tflcore diag` | Abre diagnóstico. |
| `/tflcore hud` | Abre el editor de HUD (requiere mundo). |
| `/tflcore profile <pvp\|survival\|builder\|exploration\|minimal>` | Aplica un perfil. |
| `/tflcore waypoint add [nombre]` | Añade waypoint en la posición actual. |
| `/tflcore waypoint list` | Lista waypoints del mundo/dimensión. |
| `/tflcore waypoint remove <índice>` | Borra un waypoint. |
| `/tflcore bench [segundos]` | Inicia benchmark. |
| `/tflcore export` | Exporta ajustes a `config/tflcore-export.json`. |

## Configuración y copias

Archivo: `config/tflcore.json` (schema 2).

- Guardado debounced (350 ms) y atómico.
- Backups en `config/tflcore-backups/` antes de migrar, importar o recuperar un archivo corrupto.
- Si el archivo tiene un `schemaVersion` mayor que 2, se abre en **solo lectura** y no se sobrescribe.
- Si el JSON está corrupto, se usan valores por defecto y se conserva una copia del original.
- Import/export solo acepta rutas dentro de `config/`, archivos de hasta 1 MiB y esquemas soportados.

Para reiniciar de cero: Ajustes → Avanzado → Reiniciar todo (dos clics), o cierra el juego y renombra `config/tflcore.json`.

## Privacidad

Sin telemetría, HTTP, sockets, cuentas, tokens, publicidad ni código remoto. `telemetryConsent` se fuerza a `false` en cada normalización. Los waypoints y ajustes nunca salen del equipo.

## Problemas frecuentes

**El menú no cambia**
Comprueba `customMainMenu` y que el mod aparezca cargado. Revisa el log.

**El HUD no aparece**
Activa el HUD global, revisa F1, el modo streamer y que no estés en una pantalla con “ocultar en pantalla”.

**El zoom no restaura el FOV**
Sal y vuelve a entrar al mundo; el módulo restaura al desactivarse, al salir del mundo y al cerrar el cliente. Si persiste, reinicia Minecraft (las opciones de FOV pueden haberse guardado).

**El toggle sprint no se queda activo**
Asegúrate de que no estás agachado, en un vehículo o con una pantalla abierta. Otros mods de sprint pueden competir por la tecla.

**Waypoints que no aparecen**
Están filtrados por mundo/servidor y dimensión. Comprueba que estás en el mismo mundo y dimensión.

**Ajustes de solo lectura**
El archivo es de un esquema futuro o demasiado grande. No se sobrescribe; usa una copia o actualiza TFL Core.

**Errores tras actualizar**
Revisa Ajustes → Avanzado y F9. Los backups están en `config/tflcore-backups/`. Para volver atrás, cierra el juego y restaura un backup.
