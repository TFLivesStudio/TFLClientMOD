# Arquitectura — TFL Core 0.3.0-alpha.1

TFL Client es el instalador y actualizador. TFL Core es un mod **estrictamente client-side** para Minecraft Java 26.3 (Fabric, Java 25). La optimización gráfica profunda pertenece a los mods especializados distribuidos por el launcher; el Core no reimplementa Sodium, Iris ni motores de render.

## Principios

1. **Sin red propia.** Sin telemetría, HTTP, sockets, cuentas, tokens, publicidad ni código remoto.
2. **Client-only.** No requiere nada en el servidor ni envía paquetes propios.
3. **Opcional por diseño.** Todo módulo y widget puede desactivarse; un fallo se aísla y queda visible en diagnóstico.
4. **Sin OpenGL directo.** Se usa `GuiGraphicsExtractor`, `HudElementRegistry` y APIs públicas de Fabric.
5. **Mínimos mixins.** La versión 0.3 no añade mixins; el crosshair usa `replaceElement` y el zoom usa la opción de FOV de vanilla.
6. **Rendimiento medido.** Muestreo cacheado, intervalos por widget y escrituras de configuración debounced/atómicas.

## Paquetes

```text
online.tflives.tflcore.client
├── TflCoreClient              arranque, wiring, ciclo de vida, teclas, perfiles
├── BuildInfo                  versión de compilación
├── command/TflCommands        /tflcore diag|hud|profile|waypoint|bench|export
├── compat/ModDetector         mods cargados (informativo, sin integraciones)
├── config/
│   ├── TflSettings            schema v2, normalización, presets, waypoints
│   ├── SettingsStore          debounce, escritura atómica, backups, import/export
│   ├── SettingsMigrator       v1 → v2, claves desconocidas → legacy
│   ├── WidgetPlacement        ancla, offset, escala, fondo, opacidad
│   └── Waypoint               punto local por mundo/dimensión
├── hud/
│   ├── HudManager             un único elemento Fabric para todo el HUD
│   ├── HudWidget              base con caché por intervalo
│   ├── Diagnostics            muestras de frame, p50/p95/p99, memoria
│   ├── Benchmark              captura temporizada explícita
│   ├── InputTracker           CPS desde el estado de los botones
│   └── widget/                19 widgets
├── keybind/KeybindManager     Right Shift, H, C, B, F9; conflictos y reinicio
├── module/
│   ├── TflModule              contrato de módulo
│   ├── ModuleManager          registro, dependencias, ciclos, fallos, cascada
│   ├── ModuleCategory/State
│   └── ModuleError/ModuleInfo
├── profile/                   PVP, SURVIVAL, BUILDER, EXPLORATION, MINIMAL, CUSTOM
├── pvp/                       CrosshairModule, ToggleSprintModule
├── survival/                  ZoomModule, WaypointModule, WaypointKeys
└── ui/
    ├── TflTheme               tokens de diseño, 5 temas, 6 acentos
    ├── TflDraw / UiClock      utilidades de dibujo y tiempo de UI
    ├── TflMotion              duración de transición / movimiento reducido
    ├── TflMainMenuScreen
    ├── TflSettingsScreen      6 pestañas
    ├── TflHudEditorScreen     arrastre, anclas, cuadrícula, escala, fondo
    ├── TflDiagnosticsScreen
    └── widget/                TflButton, TflToggle, TflSlider
```

## Arranque

```text
Fabric ClientModInitializer
        │
        ▼
SettingsStore.load ──► SettingsMigrator (si schema < 2)
        │                 └── backup previo + dirty para guardar v2
        ▼
TflSettings.normalize (telemetría off, límites, enums, waypoints, widgets)
        │
        ├── KeybindManager.register
        ├── HudManager.initialize/attach  (un HudElement)
        ├── ModuleManager.register + initialize
        ├── ProfileManager.detect
        └── eventos: tick, world join/leave, screen init, commands, shutdown
```

En el cierre del cliente se restauran los cambios temporales (FOV de zoom, tecla de sprint) y se hace `flush` de la configuración.

## Ciclo de un módulo

`TflModule` declara `id`, `category`, `requires`, `enabledByDefault`, `configurable` y callbacks `onEnable`, `onDisable`, `onTick`, `onWorldJoin`, `onWorldLeave`.

`ModuleManager` aplica estas reglas:

- ids duplicados o inválidos se rechazan al registrar;
- las dependencias se activan antes que el dependiente;
- una dependencia ausente deja el módulo en `MISSING_DEPENDENCY`;
- un ciclo se detecta y los implicados quedan desactivados;
- desactivar un módulo desactiva en cascada a sus dependientes;
- una excepción en un callback desactiva solo ese módulo, registra `ModuleError` y el resto sigue;
- `ModuleManager.headless(...)` permite probar el ciclo de vida sin instanciar Minecraft.

El estado persistido es `settings.modules` (`id → boolean`). Los flags heredados `pvp.toggleSprint` y `survival.zoom` se espejan para compatibilidad, pero la fuente de verdad es el módulo.

## Configuración

`config/tflcore.json` usa schema 2:

```json
{
  "schemaVersion": 2,
  "ui": { "theme": "BLUE", "accent": "DEFAULT", "transitionMillis": 160 },
  "hud": { "enabled": true, "widgets": { "fps": { "anchor": "TOP_LEFT", "x": 6, "y": 6 } } },
  "pvp": { "customCrosshair": true },
  "survival": { "zoomFov": 25, "waypoints": [] },
  "perf": { "preset": "BALANCED" },
  "modules": { "crosshair": true, "zoom": true },
  "legacy": {}
}
```

Reglas de persistencia:

- **Debounce** (350 ms por defecto): varios cambios seguidos producen una sola escritura.
- **Escritura atómica**: archivo temporal en el mismo directorio + `ATOMIC_MOVE` cuando el sistema lo soporta.
- **Backup** antes de migrar o importar, y al recuperar un archivo corrupto.
- **Schema futuro**: si `schemaVersion > 2`, el archivo se carga en modo solo lectura y nunca se sobrescribe.
- **Import/export**: solo rutas dentro de `config/`, tamaño máximo 1 MiB, JSON de objeto, esquema soportado.
- **Recuperación**: JSON inválido → valores por defecto + copia del original; el archivo original no se borra.
- **Sin escrituras por frame**: el HUD, el editor y el diagnóstico no leen ni escriben archivos en el bucle de render.

## HUD

`HudManager` registra **un solo** `HudElement` de Fabric. En cada frame:

1. decide visibilidad global (F1, pantalla abierta, modo streamer);
2. recorre widgets y llama `collect` solo si venció su intervalo de refresco;
3. calcula posición desde ancla + offset + escala;
4. dibuja fondo (opcional, con opacidad) y contenido.

En el editor se dibujan también placeholders de widgets desactivados o no disponibles, para poder colocarlos sin activarlos. La caché evita reconstruir texto o consultar el mundo en cada frame.

## Interfaz

`TflTheme` centraliza colores y estados. Los widgets de UI (`TflButton`, `TflToggle`, `TflSlider`) son `AbstractWidget` propios, navegables por teclado y traducidos con `Component.translatable`.

`TflMotion` convierte `ui.transitionMillis` en un paso por frame usando el partial tick; `reducedMotion` fuerza transiciones instantáneas. `highContrast` refuerza el texto y `reducedTransparency` hace opacas las superficies de panel.

El menú principal sustituye a `TitleScreen` cuando `ui.customMainMenu` está activo, mediante `ScreenEvents.AFTER_INIT` y `client.execute`, sin mixin.

## Gameplay sin trampas

- **Crosshair**: `HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, ...)`; al desactivar el módulo se restaura el elemento vanilla.
- **Toggle sprint**: mantiene `options.keySprint.setDown(true)` solo cuando el jugador avanza, no se agacha, no usa un vehículo y no hay pantalla abierta. No envía paquetes.
- **Zoom**: guarda `options.fov().get()`, aplica el FOV configurado y restaura el valor exacto al soltar. `smoothZoom` interpola por tick.
- **Waypoints**: datos locales; mundo/servidor y dimensión forman la clave. El HUD muestra nombre, distancia y rumbo; no se renderiza geometría 3D en el mundo.

## Seguridad y privacidad

- No hay telemetría; `telemetryConsent` se fuerza a `false`.
- No se aceptan rutas absolutas ni `..` en import/export.
- No se ejecuta contenido de la configuración: solo se leen valores JSON y se normalizan.
- Un archivo de esquema futuro no se modifica.
- No se accede a credenciales, tokens, cuentas ni direcciones de servidor.

## Validación

- **Compilación:** `./gradlew clean build` (Java 25, Loom 1.18.4, MC 26.3, Fabric API 0.163.0).
- **JUnit:** 51 pruebas en 5 suites (config, store, migrador, módulos, perfiles).
- **En partida:** no realizada para 0.3.0-alpha.1. Ver [TESTING_0.3.md](TESTING_0.3.md).
