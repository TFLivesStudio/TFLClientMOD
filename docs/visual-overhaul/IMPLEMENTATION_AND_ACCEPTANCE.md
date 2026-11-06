# TFL Core — Implementación y aceptación de Visual Overhaul

> **Estado:** planificación aprobada para **después de Audit 5**; NO iniciada.
> Propósito: transformar la dirección visual en tareas verificables y seguras, sin una nueva cadena infinita de auditorías.
> Leer [VISUAL_DIRECTION.md](VISUAL_DIRECTION.md) y [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md).

## 1. Estado inicial y prerrequisitos

**Lo que sí sabemos:**
- El responsable de producto considera bueno el menú principal en su composición general, pero duda de los botones.
- ESC gusta parcialmente y debe evolucionar sin perder funciones.
- El resto de GUI creada/modificada por TFL Core requiere una renovación completa.
- Targets obligatorios: Minecraft **1.8.9 Ornithe Gen2** y **26.3 Fabric** (Java 25).
- La rama de trabajo de Audit 5 es `feat/audit5-development`; su cierre puede cambiar la base real de esta etapa.
- Audit 4 documenta regresiones históricas de fuente/UI y el problema gráfico de 1.8.9. La validación nativa 1.8.9 no está terminada.

**Antes del primer commit de implementación:**
1. Confirmar el HEAD que realmente cierra Audit 5 y las mejoras incorporadas desde que se escribió esta documentación.
2. Elegir una rama de implementación distinta, partiendo de ese HEAD. No resetear ni reescribir historial.
3. Capturar baseline de todas las pantallas TFL de la versión disponible, en **ambos** targets y con ajustes conocidos.
4. Reunir referencias reales de Lunar, Laby y CMPack sin copiar recursos protegidos; crear una pequeña tabla «qué nos gusta / qué no copiaremos».
5. Registrar lista de pantallas y entradas reales con su clase fuente. No asumir que un fichero histórico corresponde al código vivo.
6. Guardar un tag/checkpoint no destructivo sobre el estado previo, y conservar JAR funcional + SHA-256 + respaldos de configuración.

**No iniciar esta fase por el simple hecho de fusionar esta documentación.** Requiere decisión explícita de comienzo posterior al cierre de Audit 5.

## 2. Bloques de trabajo — orden recomendado

### Etapa V0 — Baseline y dirección final (rápida, una sola tanda)

**Objetivo:** confirmar problema visual y elegir una de 2–3 propuestas coherentes, sin tocar código productivo.
- Screenshots de Main Menu, ESC, Right Shift, Graphics, Settings, HUD Editor, Mod Menu, world map, world selector, waypoints, clips/diálogos y cualquier pantalla TFL pendiente.
- Anotar diferencias visuales entre 1.8.9 y 26.3; comprobar resolución/UI Scale/idiomas.
- Mockups de Settings, Mod Menu y HUD Editor (mínimo): esas tres determinan el lenguaje visual del resto.
- Confirmar si se conserva Nunito como body o solo headings; mantener font de Minecraft para interfaces Vanilla.
- **Entregable:** conjunto de screenshots y dirección visual elegida. Evitar eternizar una etapa de research.

### Etapa V1 — Fundación de design system

**Objetivo:** unificar UI sin abrir migraciones de estado/configuración.
- Centralizar tokens y estilos de tema; adaptar los existentes `Theme/UiAppearance` en vez de duplicarlos.
- Unificar 6–10 componentes fundamentales primero (Frame, Button, SettingRow, Toggle, SearchField, Sidebar, Section, ScrollContainer, Tooltip, Modal).
- Arreglar hitboxes, clipping, foco y layout escalable desde el inicio.
- Conectar el mismo árbol lógico a Ornithe/Fabric manteniendo draw/hit-test/entrada nativa.
- Tests de tokens, layout, navegación, scroll y disabled/dirty states.
- **Entregable:** prototipo integrado de una pantalla simple y una compleja en ambos targets.

### Etapa V2 — Settings + Graphics (primer hito visible)

**Objetivo:** eliminar las pantallas más feas/saturadas, preservando backends reales.
- Rediseñar SettingsPanel y GraphicsUi con navegación, subgrupos, búsqueda y filas consistentes.
- Estados de aplicado/no aplicado, confirmaciones reales, error y dependencias.
- Probar aplicación/deshacer en Vanilla/Argentum/Sodium/Lucerna/Amplio según combinación.
- Mantener contratos de escalador/Colores, reinit, guardado y validación externa.
- **Entregable:** UI de ajustes realmente usable y atractiva en 1.8.9 y 26.3; comparación A/B.

### Etapa V3 — Right Shift + Mod Menu + HUD Editor

**Objetivo:** renovar el área TFL con mayor frecuencia de uso.
- ModMenu y ModulePanel: búsqueda, filtros, detalles, estados y navegación.
- Right Shift: abrir/cerrar fluidamente, recordar contexto, respetar keybind.
- HudEditor: biblioteca/preview/inspector; no romper drag, anclas, escala, perfiles, HUD ni persistencia.
- **Entregable:** paneles homogéneos y herramientas editables con interacción real.

### Etapa V4 — Mundo, mapas y pantallas complementarias

**Objetivo:** aplicar el sistema donde TFL intervenga visualmente.
- UI de World Map / Waypoints / selección de mundos de servidor y sus modales; preservar motor de Diorama y persistencia.
- Menús TFL de clips, capturas, perfiles, diálogos, keybind search, overlays y estados de error.
- Menús Vanilla de mundos/multijugador únicamente donde el Core ya tenga responsabilidad; respetar funcionalidad de Minecraft.
- **Entregable:** inventario completo de pantallas TFL actualizado y sin estilos heredados incoherentes.

### Etapa V5 — Pulido controlado de Main Menu + ESC

**Objetivo:** elevar la calidad de las superficies que el usuario ya valora.
- Main Menu: revisión de botones con alternativas, hitboxes, foco, vídeo, fallback, escala, transiciones y contraste.
- ESC: botones, spacing, iconografía, focus/hover, tooltips, input del chat y mundo visible.
- **Entregable:** mejoras visibles sin recrear su personalidad ni perder lo que funcionaba.

### Etapa V6 — QA completo + release candidata

**Objetivo:** una versión visual y funcional coherente con evidencia en ambas plataformas.
- Ejecutar la matriz de aceptación, comparar capturas y benchmarks.
- Recoger feedback en juego del responsable del producto; corregir hallazgos reales.
- Empaquetar candidatos, pruebas, hashes, rollback y changelog.
- Solo considerar una release cuando las rutas nativas y pruebas obligatorias del target estén verificadas; mantener 1.8.9 experimental si hay bloqueos pendientes.

## 3. Inventario de superficies de aceptación

**Los nombres son áreas funcionales; verificar y actualizar nombres de clases al empezar.**

| Pantalla/área | Cambio esperado | Invariantes de comportamiento | Estado |
| --- | --- | --- | --- |
| Main Menu / `TitleUi` | Pulir botones y consistencia | Vídeo real, fallback, pie versión, navegación | PENDIENTE |
| ESC / `PauseOverlay` | Pulido visual | Chat nativo abajo-derecha, mundo visible, HUD oculto | PENDIENTE |
| Right Shift / configuración TFL | Rediseño completo | Hotkey, foco, módulos, persistencia | PENDIENTE |
| Mod Menu / `ModMenu` | Rediseño completo | Inventario real de módulos, toggles, dependencia | PENDIENTE |
| Detail / `ModulePanel` | Rediseño completo | Ajustes vinculados, guardado, disable reasons | PENDIENTE |
| Settings / `SettingsPanel` | Rediseño completo | Búsqueda, navegación, settings, backups | PENDIENTE |
| Graphics / `GraphicsUi` | Rediseño completo | Backends reales, aplicar/deshacer, shaders | PENDIENTE |
| HUD Editor / `HudEditor` | Rediseño completo | Drag/escala/anclas/guardar/perfiles | PENDIENTE |
| Diorama World Map / UI | Revestimiento de controles | Render/proyección/zoom/rotación/selección | PENDIENTE |
| Waypoints / editor / mundos | Rediseño de paneles | Importación, carpetas, mundo origen, datos | PENDIENTE |
| Capturas / clips | Rediseño | Export/edición/archivos existentes | PENDIENTE |
| Perfiles / keybinds / diálogos | Rediseño | Guardado, shortcuts y confirmaciones | PENDIENTE |
| Multiplayer / worlds intervenidos | Condicional | Vanilla conexión/saves/autocompletado | POR INVENTARIAR |
| Overlays TFL y notificaciones | Unificación | No tapar juego, estados honestos | POR INVENTARIAR |

No marcar «COMPLETADO» por existir una clase o una captura de mockup: exige interacción real aprobada.

## 4. Matriz visual y funcional mínima

**Targets:** 1.8.9 baseline y con optimizador aprobado; 26.3 baseline y con Sodium. Pruebas ampliadas con mods/shaders del modpack final según disponibilidad, sin modificar su catálogo.

**Resoluciones:** 854×480, 1280×720, 1920×1080; fullscreen y ventana, GUI Scale pequeño/mediano/grande. Usar las resoluciones efectivas disponibles y reportar las omitidas.

**Aspectos a probar por pantalla:**
- Apertura, navegación, atrás/cerrar y reentrada.
- Ratón, teclado, rueda, tabulación, acción de foco y atajos.
- Texto con ES/EN, títulos largos, Unicode, fallback y truncado.
- Hover, pressed, focus, selected, disabled, loading, vacío, error y cambios pendientes donde existan.
- Layout, clipping, superposición de tooltips y campos, scroll sin desbordamientos.
- Valor actual del backend y persistencia después de reiniciar el cliente.
- Reduced motion, baja transparencia/alto contraste cuando exista soporte.
- Comportamiento con vídeo habilitado y desactivado (Main Menu).
- Flujo de drag, resize y reset controlado (HUD Editor).
- Conexión/salida/cambio de mundo y ESC/chat en servidor cuando corresponda.
- Renderizado estable, restauración OpenGL y compatibilidad con shaders/mods del stack.
- Sin desbloqueo involuntario de funciones TRS online/descontinuadas.

**Imagen de evidencia:** una captura limpia por estado representativo, con build hash, target, resolución, GUI Scale, lengua y condiciones del perfil. No utilizar mockups como prueba de runtime.

**Performance:** registrar p50/p95/p99 de UI si hay instrumentación disponible, carga de texturas/asignaciones, coste de abrir pantalla y frame time en escenarios repetibles. No inferir FPS desde llvmpipe de CI. Repetir en GPU real de rango alto y PC modesto antes de prometer rendimiento general.

## 5. Checklist de aceptación por componente

Para cada componente nueva implementación:
- [ ] Comparte tokens y semántica común; cualquier excepción justificada.
- [ ] Hover/focus/pressed/disabled visibles y correctos.
- [ ] Hitbox corresponde al contenido real y no intercepta controles vecinos.
- [ ] Mouse, teclado y scroll disponibles con foco correcto.
- [ ] Texto internacional sin clipping ni fuente corrupta.
- [ ] Layout escalable y legible en resolución pequeña.
- [ ] No altera valores sin acción del usuario.
- [ ] Restauración limpia si el contexto/UI/renderer pierde el foco.
- [ ] Consume recursos de forma razonable; no recarga assets por frame.
- [ ] Comportamiento real probado tanto en Ornithe como en Fabric si aplica.
- [ ] Screenshot de runtime comparada y aceptada, no solo test unitario.

## 6. Criterios de aceptación de la actualización completa

**Un producto visual terminado debe cumplir TODOS:**
1. Las pantallas TFL inventariadas usan un lenguaje visual coherente, con excepciones documentadas.
2. El menú principal sigue siendo reconocible y su sistema de botones está aprobado.
3. ESC conserva chat funcional inferior derecho, X, velo y navegación izquierda; se oculta el HUD temporalmente sin cambiar settings.
4. Las opciones gráficas aplican/deshacen con su backend original y muestran restricciones reales.
5. El usuario puede editar, activar y configurar módulos/HUD sin perder ajustes.
6. Los mapas y waypoints de Audit 4 se conservan; sin mezcla de mundos ni pérdida de carpetas.
7. Se ve bien en resolución reducida, escalado alto y distintas lenguas; no hay texto fuera de pantalla.
8. Navegación de teclado, estados de foco y reduced motion son utilizables.
9. No hay regresiones de GL/texto/vídeo, especialmente en 1.8.9 con Diorama.
10. Hay JARs, hashes, changelog, capturas reales y validación explícita por target.
11. El responsable del producto revisa capturas y realiza prueba manual de las pantallas principales.
12. El inventario registra qué pasó, qué falla y qué continúa experimental; no se certifica una pantalla solo por compilar.

## 7. Separación y recuperación de versiones

- Mantener Audit 4 y cierre de Audit 5 identificables por commit/tag; no reutilizar nombres de releases para builds distintos.
- Rama visual propia, cambios pequeños por pantalla o familia de componentes, commits atómicos.
- **Checkpoint inicial** sobre último commit de Audit 5; otro checkpoint antes de cambiar renderers compartidos.
- Preservar JAR verificable y hash del último build aceptado antes de cada cambio invasivo.
- Mantener compatibilidad de datos y backups; no indicar al usuario borrar config, mapas o waypoints para probar skins.
- Si una migración de configuración es imprescindible, hacerla no destructiva, versionada y con tests de rollback.
- **Rollback de código:** revertir commits concretos o cambiar al tag previo sin `reset --hard` en ramas compartidas.
- **Rollback de cliente:** reinstalar un JAR conocido; considerar compatibilidad de configuración y copias de seguridad, ya que Git no revierte los datos del jugador.
- No hacer force-push, merges a main, automatizaciones costosas, releases ni tags de producción sin autorización.
- No introducir un toggle «Legacy UI» incompleto solo para esconder fallos; si se necesita fallback, diseñarlo y probarlo como alternativa real.

## 8. Evidencia y responsabilidad

Cada bloque entrega un informe breve de:
- Pantallas/componentes cambiados (rutas de código).
- Capturas antes/después con detalles de entorno.
- Pruebas realmente ejecutadas y limitaciones.
- Regresiones observadas, mediciones y no-certificaciones.
- Commit/branch y rollback.
- Decisiones de diseño que necesita aprobar el responsable.

**Ritmo recomendado:** construir una tanda → probar → guardar → continuar. Revisiones independientes solo ante riesgo gráfico crítico, migración de datos o release; evitar detener producción por revisiones redundantes.

## 9. Definition of Ready para comenzar post-Audit 5

- [ ] Audit 5 terminado/congelado, bloqueos conocidos y commit base registrado.
- [ ] Rama visual creada desde HEAD correcto, checkpoint seguro.
- [ ] Capturas base de las pantallas TFL y lista real de fuentes.
- [ ] Referencias visuales verificadas, sin copiar assets de terceros.
- [ ] Prototipos de Settings, ModMenu y HUD Editor elegidos.
- [ ] Estrategia de fuentes y renderers confirmada en ambos targets.
- [ ] No hay tareas urgentes de estabilidad de Audit 5 que queden inadvertidamente absorbidas por el rediseño.

**Estado actual (2026-11-04): documentación preparada; todas las casillas de implementación y aceptación siguen pendientes.**
