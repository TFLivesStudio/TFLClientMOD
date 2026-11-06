# Visual Overhaul — prototipo aislado 0.5.0-visual-preview.1

**No es el cierre de la actualización 0.5.0.** Preparación en `feat/tfl-visual-overhaul`, sin modificar Audit 5, main, Selection o launcher. Sin release oficial ni promoción de canal.

Base de código de la RC2: `7503775f8ed6a160348a0e5c3afe4b2e906fcb87`; HEAD inicial `1f6d77d` añade únicamente su publicación autorizada. Checkpoint local: `checkpoint/pre-visual-overhaul-7503775`. La issue #14 sigue abierta y `AUDIT5_STATUS.md` no declara terminada la matriz. La condición del encargo es «Audit 5 finalizado o formalmente congelado»; se ha solicitado autorización para congelar RC2, sin asumir respuesta afirmativa. Este trabajo puede revisarse y compilarse aisladamente, pero no se presenta como integración aprobada post-Audit 5.

## Cambios ejecutables

- `Theme`, `SelectionStyle`/`Redstone`, `WindowUi`: superficies neutrales, botones estables, foco de teclado compartido y encabezados comunes. Se conserva el acento/configuración existentes.
- `ModMenu`: listado virtualizado de módulos con descripción, interruptores y detalle; cachea resultados por búsqueda/categoría/idioma. La búsqueda de ajustes abre su control real y conserva la consulta al volver. Se conserva la gestión de configuraciones y perfiles anterior.
- `SettingsPanel`: filas más espaciadas, selectores legibles y énfasis en el ajuste encontrado. Restablecer un módulo requiere una segunda pulsación explícita.
- `GraphicsUi`: perfiles primero, previsualización, aplicación y restauración; navegación por función, incluyendo iluminación y entidades/terreno. Usa los objetos originales de configuración.
- `GraphicsProfiles`: capacidades tipadas, lista explícita de IDs, validación previa y antes de escribir, snapshots, detección de vistas previas caducadas y rollback con error explícito si la restauración falla. El perfil efectivo se deriva de valores actuales, también al reabrir.
- `HudEditor`: biblioteca de widgets, activación/selección, inspector desplazable y mantenimiento de arrastre, snapping, guías, perfiles e historial. No cambia posiciones automáticamente.
- `OrnitheDioramaUi`: barra de acciones adaptable y localizada; no cambia proyección, LOD, mapas, caché ni datos de waypoints. `ScreenArt` unifica la selección de la UI moderna.
- `ClipEditorUi`: recorte/audio/formato organizados, exportación fija y contenido desplazable, ES/EN; conserva FFmpeg y la exportación a archivo nuevo. Otras pantallas heredan componentes; eso no equivale a reconstruirlas completamente.
- `TitleUi` y `PauseActionsUi`: superficies/hover discretos manteniendo sus posiciones, vídeo y callbacks. ESC no cambia el chat nativo ni la ocultación del HUD.
- Tipografía: alcance explícito y temporal en Canvas para las superficies TFL, restaurado con `finally`; no se intercepta la fuente general de Minecraft. Los saltos de párrafo se separan antes de dibujar, evitando glifos de control. Nueva preferencia persistente de movimiento reducido.

## Política real de perfiles

Se administran exclusivamente distancia de terreno, gráficos clásicos, nubes, partículas, iluminación suave, mezcla de biomas y mipmaps cuando existe binding validado. Los valores son explícitos (distancia 16/10/6, mipmaps 4/2/0, etc.), no máximos inferidos del hardware. El proveedor puede rechazarlos: aparecen como omitidos.

1.8.9 Vanilla usa setters nativos; Argentum usa su controlador/bindings. 26.3 Vanilla usa OptionInstance y su validación; Sodium usa opciones/validadores propios. Los cambios diferidos se aplican mediante el proveedor y los inmediatos se guardan mediante Minecraft. La restauración se ofrece mientras la pantalla conserve su snapshot y los valores no hayan cambiado después; no sobrescribe una edición posterior.

Lucerna/shaderpack, Amplio/DLSS/Frame Generation/Colores, límites de FPS, API gráfica, fullscreen y resolución se conservan. No hay automatización adicional de Extras/Sodium Extra/Sarcio: los ajustes manuales existentes siguen disponibles, pero no se atribuyen valores/unidades o APIs no verificados. La confirmación Amplio/Colores continúa vigente. Los perfiles no certifican un hardware concreto ni fuerzan shaders.

ES y EN tienen textos propios; las demás lenguas mantienen fallback inglés para claves nuevas. No se presentan esas claves como traducciones profesionales a todos los idiomas.

## Inventario real y alcance restante

| Superficies activas | Clases | Estado |
| --- | --- | --- |
| Inicio / pausa | `TitleUi`, `PauseActionsUi`, adaptadores de chat nativo | Pulido conservador; aceptación visual del usuario pendiente |
| Centro / módulos / ajustes | `ModMenu`, `ModulePanel`, `SettingsPanel`, `GraphicsUi` | Prototipo funcional; no todas las agrupaciones/entradas de teclado reconstruidas |
| HUD | `HudEditor`, `HudItem`, `SettingsPanel` | Biblioteca/inspector; QA en partida y densidad pendientes |
| Perfiles, configuración, notas | `ServerProfilesPage`, páginas de perfiles/CONFIG de `ModMenu`, `NotesPage`, `PacksPage` | Conservadas, reciben componentes; composición específica pendiente |
| Mapas y waypoints | `OrnitheDioramaUi`, `OrnitheWaypointUi`, `OrnitheWaypointListUi`, `WorldCatalogUi`, `WaypointSaveUi`, Diorama `MapScreen` y `Waypoint*Screen` | Barra/selección/componentes; rediseño integral y QA de navegación pendiente |
| Medios | `ClipsUi`, `ClipEditorUi`, `ScreenshotEditorUi` | Editor de clips reorganizado; biblioteca/editor de imágenes conservan su composición |
| Diálogos / servidor / compartir | `WindowUi`, `ActionListUi`, `ServerInfoUi`, `WorldShareUi` | Marco/botones compartidos; revisión individual pendiente |
| Vanilla intervenida | `MenuSkin` y hooks existentes de mundos, multijugador, opciones, controles/keybinds | Botones comunes; lógica Vanilla intacta; revisión completa pendiente |
| Overlays | screenshots/panorama/toasts y otros componentes registrados | Sin renovación completa |

`AccountsUi`, `FriendsUi`/`FriendsPanel`, `SocialUi`, `QuickReplyUi`, `HostingUi`/`JoinUi`/`WorldsPanel`, `WardrobeUi`, `BugReportPage` y `HalloweenPage` existen en fuente: su disponibilidad depende de Product/evento/capacidades. No se reactivan servicios online ni se usan como nuevas pantallas ficticias.

V0 aún carece de una baseline completa RC2 y de referencias/múltiples propuestas aceptadas. V1–V3 tienen implementación aislada; V4 es parcial; V5 conserva composición; V6 no está cerrado. No se marcan los criterios de aceptación del documento maestro como aprobados por compilar.

## Verificación y recuperación

Los informes/capturas del paquete identifican cada recorrido nativo. El probe es un JAR de desarrollo separado, excluido de los mods entregados. Pruebas de UI no certifican mapas/shaders/mundos, GPU física, comportamiento completo de teclado ni el cierre de Audit 5. No se ejecutan matrices Actions.

Para probar: duplica tu instancia, conserva config/mapas/waypoints, retira el JAR anterior de esa copia e instala el de su target con Java 25 y las mismas dependencias de RC2. No elimines datos. La UI no aplica un perfil al abrirse; primero debes previsualizarlo y confirmarlo. Vuelve al JAR RC2 y a la copia de configuración para rollback del cliente.

### Resultados de esta tanda

- Suite común completa: **1.223 casos, cero fallos/errores, dos omitidos**. Se incluyeron regresiones de transacciones, búsqueda exacta, teclado, clipping y restauración del alcance tipográfico al fallar el dibujo.
- Ornithe: `test` y `remapJar` aprobados. Fabric 26.3: build aprobado. Escáner de secretos y `git diff --check` aprobados.
- Recorridos nativos UI: 1.8.9 baseline **13 checks**, Argentum **15**; 26.3 baseline **11**, Sodium **11**. Aplicación/restauración de opciones reales aprobada. Las imágenes son capturas nativas, no mockups.
- 1.8.9: capturas 1920×1080, 1280×720 y 854×480; 26.3: 1280×720 y 854×480. Escala GUI 2 en el recorrido y textos ES/EN; no matriz de todas las escalas/fullscreen. 26.3 necesitó `SDL_VIDEO_FORCE_EGL=1`: GLX no pudo crear el contexto. Drivers de software; sin métricas extrapoladas de FPS.
- El último empaquetado 1.8.9 recompila la política más restrictiva que excluye las distancias de Extras. Los recorridos son de integración de UI, no una certificación de cada byte y cada módulo del cliente completo.
- Evidencias representativas: `docs/evidence/visual-overhaul/`; más capturas e informes en el paquete local. Permanecen pendientes las condiciones y superficies enumeradas arriba.
