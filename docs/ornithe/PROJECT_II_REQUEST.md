# MISIÓN MAESTRA — TFL CLIENT
## PROJECT ORNITHE II — FULL FEATURE PARITY
### Complete Module Migration · Argentum Compatibility · Native UX · Performance Certification

**Organización:** TFLivesStudio  
**Producto:** TFL Client  
**Repositorio:** https://github.com/TFLivesStudio/TFLClientMOD  
**Rama de trabajo:** `feat/ornithe-1.8.9-poc`  
**PR existente:** https://github.com/TFLivesStudio/TFLClientMOD/pull/6  
**Versión de origen:** `v0.4.0-ornithe.1`  
**Commit inicial publicado:** `c6379617122ef175bbf5cb2875c67283836afb97`  
**Versión objetivo sugerida:** `v0.4.0-ornithe.2`  
**Minecraft:** Java Edition 1.8.9  
**Loader:** Ornithe Gen2 / Fabric Loader 0.19.5  
**Java:** 25  
**OSL:** 0.22.2  
**Legacy LWJGL3:** 1.4.10  
**Argentum:** 1.0.2  
**Modelo recomendado:** GPT-6.1 Sol, Medium.

---

# 0. CONTEXTO Y RESULTADO DE LA PRIMERA FASE

La primera versión experimental de TFL Client Ornithe fue publicada y funciona.

El product owner ya la probó personalmente y considera que su rendimiento comparado con Forge 1.8.9 es extraordinariamente bueno.

Esto es una señal positiva, pero no reemplaza benchmarks objetivos.

**La combinación Ornithe Gen2 + Legacy LWJGL3 + Argentum es nuestra principal candidata para TFL Client 1.8.9.**

El objetivo ahora es transformar la prueba de concepto de siete módulos en una implementación de TFL Client completa.

No quiero un cliente nuevo creado desde cero.

No quiero reemplazar el núcleo compartido.

No quiero abandonar la arquitectura multiversión.

Quiero que Ornithe tenga la misma potencia funcional que nuestras implementaciones existentes, conservando el rendimiento que nos impresionó.

Esta misión NO debe terminar después de añadir unos pocos módulos.

El alcance comprende **todo el inventario de 79 módulos**, sus configuraciones, herramientas, pantallas, interacciones y dependencias, con adaptación real a Minecraft 1.8.9.

La cobertura debe ser completa en su análisis y máxima en su implementación.

No inventes compatibilidad para funciones imposibles, servicios inexistentes o mecánicas introducidas después de Minecraft 1.8.9.

---

# 1. REVISIÓN OBLIGATORIA DEL TRABAJO EXISTENTE

Antes de implementar, analiza:

1. `docs/ornithe/README.md`
2. `docs/ornithe/VALIDATION.md`
3. `docs/ornithe/RELEASE.md`
4. `tools/ornithe/dependencies.json`
5. `vendor/trs-client/ornithe/build.gradle`
6. `vendor/trs-client/ornithe/src/main/java/`
7. `vendor/trs-client/common/`
8. `vendor/trs-client/legacy/`
9. `vendor/trs-client/fabric/`
10. `distribution/modules.json`
11. `distribution/module-audit.json`
12. `docs/FEATURE_MATRIX.md`
13. `docs/MODULE_CERTIFICATION.md`
14. PR #6, commits, CI y artefactos publicados.

Utiliza el código actual como referencia autoritativa.

Compara los adaptadores modernos y legacy.

No supongas que una característica de Fabric 26.3 funciona directamente en 1.8.9.

No copies implementaciones Forge sin traducir correctamente sus eventos, mappings y ciclo de vida.

No alteres el núcleo compartido de forma que rompa los targets existentes.

---

# 2. PRIMER OBJETIVO: ARQUITECTURA DE CAPACIDADES

He identificado una limitación arquitectónica concreta.

En `OrnitheClient.java` existe:

`OrnitheClient.SUPPORTED`

Actualmente registra exactamente siete IDs.

Después, `PocMenu.java` utiliza esa lista para mostrar únicamente siete módulos.

Esta solución fue adecuada para la prueba de concepto, pero no sirve como arquitectura definitiva.

## Trabajo obligatorio

Eliminar gradualmente el modelo de capacidades basado en una lista estática de siete IDs.

Implementar un sistema real de disponibilidad por plataforma.

Debe distinguir:

- Disponible y operativo.
- Disponible con limitaciones.
- No disponible en esta versión de Minecraft.
- Dependiente de integración externa opcional.
- Dependiente de backend no configurado.
- Deshabilitado por conflicto.
- Experimental.
- Pendiente de implementación.

Las capacidades deben representar funcionalidades conectadas al runtime, no simplemente clases existentes.

La GUI debe construirse sobre este registro de capacidades.

Evitar divergencias entre:

- Estado del módulo.
- Disponibilidad.
- Ajustes.
- Eventos registrados.
- Persistencia.
- Renderizadores.
- Acciones.
- Compatibilidad.

No mostrar un módulo como funcional si carece de su implementación nativa.

No eliminar sus preferencias del archivo de configuración.

Las preferencias de módulos no disponibles deben preservarse sin provocar efectos colaterales.

---

# 3. REEMPLAZAR LAS PANTALLAS DE PRUEBA DE CONCEPTO

Actualmente tenemos:

- `PocMenu`
- `PocHost`
- `CoreScreen`
- `OrnitheCanvas`

Estos componentes permitieron demostrar que el núcleo compartido funciona.

Pero todavía no representan la experiencia completa de TFL Client.

## Objetivo

Adoptar las pantallas definitivas del núcleo compartido:

- Menú completo de módulos.
- Configuración avanzada.
- HUD Editor.
- Perfiles.
- Presets.
- Waypoints.
- World Map.
- Crosshair Editor.
- Emote Wheel.
- Captures.
- Diagnostics.
- Otras pantallas realmente aplicables.

Portar o implementar el adaptador necesario para todas las operaciones que requieren Minecraft.

Evitar duplicar componentes de UI.

El resultado debe mantener la identidad TFL Client y utilizar Nunito.

No quiero volver a tener una interfaz de prueba separada de nuestra arquitectura principal.

## Deficiencias conocidas de la PoC

`PocHost` mantiene implementaciones incompletas como:

- `playClick()` vacío.
- `openPacks()` vacío.
- `actions()` sin acciones.
- Etiquetas de teclas simplificadas.

Estos métodos deben recibir implementaciones apropiadas cuando la funcionalidad correspondiente exista.

No implementar sonidos, apertura de archivos o enlaces sin comprobar disponibilidad y permisos.

No rellenar métodos vacíos con comportamientos falsos.

---

# 4. PORT COMPLETO DE HUD

Este será uno de los primeros bloques importantes.

El núcleo compartido ya dispone de numerosos widgets.

Quiero reutilizar los renderizadores existentes siempre que sea posible.

## Widgets prioritarios

- FPS.
- CPS.
- Keystrokes.
- Coordinates.
- Ping.
- Armor.
- Potion Effects.
- Clock.
- Memory.
- Server Address.
- Active Resource Packs.
- Toggle Sprint.
- Toggle Sneak.
- Reach.
- Combo.
- Speed.
- Scoreboard.
- Boss Bar.
- Titles.
- Warnings.
- Item Counter.
- Pinned Note.
- Indicadores Redstone.
- Otros módulos HUD del inventario.

## Requisitos

Cada widget debe:

1. Renderizar datos correctos.
2. Respetar su configuración.
3. Respetar posición y anclaje.
4. Respetar escala.
5. Soportar cambio de resolución.
6. Mantener compatibilidad con HUD Editor.
7. Persistir correctamente.
8. Desactivarse sin efectos secundarios.
9. No provocar consumo elevado cuando está desactivado.
10. Restablecer recursos al cambiar de mundo.

No basta con utilizar rectángulos y texto como sustitutos permanentes cuando existe un renderer más completo en `common`.

## HUD Editor

Portar todas sus capacidades aplicables:

- Drag-and-drop.
- Snap.
- Guías.
- Undo/Redo.
- Presets.
- Perfiles.
- Escalado.
- Colisiones.
- Zonas seguras.
- Configuración individual.

Corregir layouts que se desorganizan a resoluciones pequeñas.

No mostrar previews de módulos no disponibles.

---

# 5. PORT COMPLETO DE FUNCIONALIDADES PVP

Portar:

- Zoom completo.
- Toggle Sprint.
- Toggle Sneak.
- Crosshair.
- CPS.
- Keystrokes.
- Reach informativo.
- Combo.
- Hit Color.
- Hit Feedback.
- Freelook.
- 1.7 Animations.
- Low Fire.
- Hitboxes visuales.
- No Hurt Cam.
- Item Counter.
- Otras capacidades PvP existentes.

## Zoom

Actualmente solo existe soporte parcial para:

- Factor.
- Smoothing.

Completar las opciones existentes que sean aplicables:

- Scroll Zoom.
- Mouse Slowdown.
- Cinematic Zoom.
- Keybinds configurables.
- Restauración del FOV.
- Integración con cambios de cámara.

El zoom nunca debe dejar el FOV alterado tras:

- Soltar la tecla.
- Morir.
- Cambiar de mundo.
- Desconectarse.
- Deshabilitar el módulo.
- Abrir una GUI.

## Toggle Sprint / Sneak

Conservar la corrección ya implementada para pulsaciones breves.

Probar estados de:

- Tecla mantenida.
- Tecla pulsada brevemente.
- Repetición del sistema operativo.
- GUI abierta.
- Cambio de mundo.
- Falta de comida.
- Colisiones.
- Movimiento hacia atrás.
- Desactivación.
- Cambio de keybind.

No romper el comportamiento Vanilla.

## Funcionalidades dependientes de versión

Minecraft 1.8.9 no dispone de los escudos de combate introducidos en 1.9.

`shieldPosition` no debe simular funcionalidades inexistentes.

Documenta su incompatibilidad y conserva su configuración para otros targets.

No implementar automatizaciones abusivas ni modificar el protocolo de combate para obtener ventajas ilegítimas.

---

# 6. MINIMAP, WORLD MAP Y WAYPOINTS

Este es uno de los mayores subsistemas que necesitamos portar.

El código compartido ya contiene lógica de mapas, regiones, persistencia, render y waypoints.

No desarrolles otro minimapa desde cero salvo que exista una imposibilidad técnica demostrada.

## Objetivos

- Minimap.
- World Map.
- Waypoints.
- Death Waypoints.
- Marcadores por dimensión.
- Mapas persistentes.
- Coordenadas Nether/Overworld.
- Zoom.
- Búsqueda.
- Exportación.
- Carga progresiva.
- Diferentes estilos y formas.
- Opciones de Fair Play.

## Trabajo técnico

Investiga cuidadosamente las APIs de mundo y chunks de Ornithe 1.8.9.

Diseña adaptadores específicos para:

- Acceso a chunks cargados.
- Información de biomas.
- Altura.
- Bloques.
- Texturas.
- Dimensiones.
- Jugador.
- Entidades visibles.
- Identidad de servidor.
- Identidad de mundo.

No reveles información de chunks no recibidos por el cliente.

No utilices información de entidades que el servidor no ha comunicado.

Respeta las reglas de Fair Play.

## Rendimiento

No recalcular el mapa completo en cada frame.

No realizar operaciones de disco costosas en el render thread.

No generar subidas de texturas innecesarias.

Limitar colas y memoria.

Comprobar funcionamiento con Argentum y cambios de dimensión.

Validar mundo nuevo, exploración rápida, teletransporte, Nether y desconexión.

---

# 7. CHAT Y UTILIDADES

Portar las funciones offline aplicables:

- Chat Improvements.
- Chat Timestamps.
- Chat History.
- Chat Heads.
- Chat Filter.
- Chat Mentions.
- Auto GG.
- Text Hotkeys.
- Auto Reconnect.
- Queue Alerts.
- Streamer Mode.
- Tooltips.
- Notes.
- Pinned Notes.
- Server Profiles.
- Screenshots.

## Requisitos

Conservar el comportamiento real de Minecraft 1.8.9.

Evitar:

- Duplicación de mensajes.
- Alteración de firmas o formatos incompatibles.
- Intercepción excesiva de teclas.
- Pérdida del historial.
- Envío accidental de mensajes.
- AutoGG activado sin consentimiento.
- Exposición de direcciones sensibles en Streamer Mode.

Cualquier función que envíe mensajes automáticamente debe ser configurable y fácil de desactivar.

---

# 8. REDSTONE Y HERRAMIENTAS TÉCNICAS

TFL Client hereda herramientas de Redstone que deben recibir su propia adaptación.

Portar cuando sea posible:

- Signal Strength.
- Signal Overlay.
- Clock Meter.
- Circuit Library.
- Indicadores y herramientas técnicas relacionadas.

Minecraft 1.8.9 tiene diferencias relevantes con versiones modernas.

Por tanto, deben adaptarse correctamente:

- Block states.
- Señales.
- Repetidores.
- Comparadores.
- Tick timing.
- Información de bloques.
- APIs de mundo.

No copiar cálculos modernos que no sean equivalentes para 1.8.9.

Separar la obtención de información del mundo de la presentación del widget.

Los análisis complejos no deben ejecutarse continuamente cuando las herramientas están desactivadas.

---

# 9. EMOTES, CAPES Y PERSONALIZACIÓN

Portar el sistema local de emotes ya desarrollado para TFL Client.

Queremos:

- Rueda radial.
- Emotes locales.
- Animaciones.
- Keybinds.
- Configuración.
- Cancelación.
- Restauración de cámara.
- Vista previa cuando sea viable.

Debe funcionar offline.

No depender de TRS Launcher ni de cuentas TRS.

## Capes

Portar Cape Physics cuando sea compatible.

Respetar capas propias del jugador y sus recursos locales autorizados.

No simular desbloqueos de cosméticos propietarios.

No establecer conexiones no autorizadas con infraestructura TRS.

La sincronización con otros jugadores es una función separada que puede requerir compatibilidad entre clientes o un servicio adicional.

No declarar que existe si todavía no está implementada.

---

# 10. RENDIMIENTO Y ARGENTUM

Este es el requisito técnico más delicado de toda la misión.

**No quiero perder el rendimiento extraordinario que Ornithe + Argentum está mostrando en las pruebas del usuario.**

TFL Client no debe introducir un segundo motor de renderizado de chunks.

No portar indiscriminadamente las optimizaciones de Forge que puedan solaparse con Argentum.

## Revisar individualmente

- FPS Boost.
- Dynamic FPS.
- Entity Culling.
- Builtin Optimizations.
- Low Latency.
- Net Optimize.
- Fast Connect.
- Particles.
- World Details.
- Borderless Fullscreen.
- Cualquier otra función de optimización.

Para cada función determinar:

1. Qué optimiza exactamente.
2. Qué modifica.
3. Si Argentum ya implementa lo mismo.
4. Si Legacy LWJGL3 altera su compatibilidad.
5. Si debe permanecer activa.
6. Si debe ofrecer un fallback.
7. Si necesita un mecanismo de exclusión.
8. Cómo se prueba.
9. Qué beneficio medible aporta.

## Políticas

No duplicar Entity Culling.

No instalar optimizadores incompatibles.

No alterar automáticamente configuraciones de Argentum.

No introducir wrappers OpenGL innecesarios.

No realizar lecturas masivas del mundo desde render.

No cargar recursos en cada frame.

No crear texturas dinámicas cuando pueden reutilizarse.

No realizar allocations excesivas.

No utilizar reflexión costosa repetidamente en el render loop.

## Modos

TFL debe funcionar en:

A. Ornithe + OSL.

B. Ornithe + OSL + Legacy LWJGL3.

C. Ornithe + OSL + Legacy LWJGL3 + Argentum.

Argentum continúa siendo opcional desde el punto de vista del mod.

La distribución optimizada puede recomendarlo como parte del perfil de modpack.

Nunca copiar ni redistribuir su implementación propietaria sin permisos adecuados.

---

# 11. AUDITORÍA DEL RENDERER ORNITHE

El adaptador actual es una base muy compacta, pero todavía contiene limitaciones.

Revisar:

`OrnitheCanvas.java`

`CoreScreen.java`

`GameGuiMixin.java`

`GameRendererMixin.java`

`MinecraftMixin.java`

`KeyBindingMixin.java`

## Fuente Nunito

Actualmente OrnitheCanvas genera un atlas tipográfico limitado y dibuja caracteres individualmente.

Necesitamos investigar:

- Cobertura Unicode.
- Español y acentos.
- Fuentes de reserva.
- Batching.
- Cantidad de draw calls.
- Caché de glifos.
- Escalado.
- Clipping.
- Métricas.
- Liberación de texturas.
- Resource reload.
- Compatibilidad con GL state de Argentum.

No implementar una solución más compleja que perjudique el rendimiento sin justificarla.

Medir antes y después.

## OpenGL

Auditar:

- Blend state.
- Depth test.
- Scissor.
- Texture bindings.
- Matrices.
- Shaders.
- Alpha.
- Estado previo al render.
- Estado restaurado al terminar.

Evitar fugas de estado gráfico que afecten entidades, GUI Vanilla o Argentum.

## Pantallas

No forzar una apariencia pixelada por estar en Minecraft 1.8.9.

Reutilizar el design system moderno cuando resulte posible.

---

# 12. EXPERIENCIA VISUAL TFL CLIENT

El nombre público definitivo es TFL Client.

No TRS Client.

No TFL Core como nombre comercial.

No Classic Menu.

## Debe incluir

- Menú principal TFL.
- Nunito.
- Navegación izquierda.
- Agujero negro.
- ESC personalizado.
- Options moderno.
- Panel de módulos completo.
- Editor HUD.
- Perfiles.
- Mapas.
- Emotes.
- Capturas.
- Diálogos.
- Pantallas de carga.
- Notificaciones.
- Todos los controles necesarios.

Mantener un único menú principal normal.

Conservar una ruta técnica de recuperación si falla la interfaz TFL.

No reemplazar funcionalidades Vanilla de manera que se rompa el juego.

## Prioridad

Primero completar una infraestructura visual consistente.

Después adaptar las diferentes pantallas.

No crear 79 menús de configuración separados.

El sistema debe ser escalable.

---

# 13. FUNCIONALIDADES ONLINE Y EXCEPCIONES

La meta es portar todos los módulos, pero no podemos inventar servicios externos.

Los siguientes necesitan tratamiento especial:

- `trsOnline`.
- `social`.
- `clips` dependientes del launcher.
- Hosting externo.
- Amigos online.
- Cosméticos y cuentas remotas.
- Servicios o eventos estacionales que requieran recursos externos.

Si TFL no dispone de un backend implementado y autorizado, estas funciones deben permanecer deshabilitadas de manera segura.

No mostrar botones muertos.

No conectar a servidores TRS.

No simular funciones.

No copiar tokens ni credenciales.

Conservar el código reutilizable para una futura integración TFL legítima.

Las funciones offline equivalentes, cuando existan, sí deben estar disponibles.

Ejemplo: capturas y emotes locales no necesitan TRS Launcher.

---

# 14. INTEGRIDAD DE CONFIGURACIONES

Todas las nuevas funciones deben utilizar la arquitectura de configuración existente.

No crear otro sistema de preferencias separado para Ornithe.

Mantener:

- IDs de módulos.
- ConfigStore.
- Perfiles.
- HUD.
- Waypoints.
- Backups.
- Migraciones.
- Exportación/importación.
- Protección contra corrupción.
- Protección contra versiones futuras.

## Caso especialmente importante

La primera versión Ornithe conserva datos de módulos todavía no disponibles.

Al habilitarlos ahora, no debes convertir automáticamente configuraciones antiguas en estados peligrosos.

Verifica cómo se interpretan:

- Módulos activados previamente.
- Ajustes desconocidos.
- Opciones de versiones modernas.
- Keybinds incompatibles.
- Datos de mapas.
- Preferencias de render.

Los módulos nuevos deben recibir defaults sensatos.

No sobrescribir ni eliminar información desconocida.

No modificar instalaciones Forge/Fabric existentes al probar Ornithe.

---

# 15. CERTIFICACIÓN POR MÓDULO

Genera una matriz de portabilidad usando el inventario real de `distribution/modules.json`.

Para cada uno de los 79 IDs, exigir:

- Identidad.
- Categoría.
- Aplicabilidad a Minecraft 1.8.9.
- Estado de implementación Ornithe.
- Hooks nativos.
- GUI y ajustes.
- Activación.
- Desactivación.
- Persistencia.
- Restauración.
- Dependencias.
- Compatibilidad con Argentum.
- Pruebas unitarias.
- Pruebas nativas.
- Limitaciones.
- Estado final.

## Estados aceptados

- Implementado y probado.
- Implementado con limitaciones verificadas.
- No aplicable a Minecraft 1.8.9.
- Requiere infraestructura TFL no disponible.
- Bloqueado por conflicto técnico documentado.
- Pendiente de implementación.

Ningún módulo podrá aparecer como probado si únicamente fue activado en el registro.

El estado pendiente debe representar un trabajo real por hacer, no una excusa para reducir artificialmente el alcance.

El objetivo es minimizar los pendientes mediante implementación efectiva.

---

# 16. PLAN DE PORTABILIDAD

Organiza el trabajo internamente por dependencias técnicas:

## Etapa A — Infraestructura

- Capability Registry.
- MenuHost completo.
- GUI definitiva.
- Keybinds.
- Recursos.
- Adaptadores de mundo.
- Adaptadores de render.
- Lifecycle.
- Configuración.

## Etapa B — HUD

- Widgets.
- Editor.
- Presets.
- Perfiles.
- Layout.

## Etapa C — PvP

- Cámara.
- Zoom.
- Sprint.
- Sneak.
- Combate informativo.
- Animaciones.
- Crosshair.

## Etapa D — Mundo

- Minimap.
- World Map.
- Waypoints.
- Biomas.
- Dimensiones.

## Etapa E — Utilidades

- Chat.
- Capturas.
- Notas.
- Redstone.
- Perfiles de servidor.
- Reconexión.

## Etapa F — Personalización

- Emotes.
- Capes.
- Temas.
- Galería.
- Opciones avanzadas.

## Etapa G — Rendimiento

- Compatibilidad con Argentum.
- Exclusión de optimizadores redundantes.
- Benchmarks.
- Estabilización.

## Etapa H — Certificación y publicación

- Tests.
- CI.
- Pruebas nativas.
- Distribución.
- Prerelease.

No detengas la implementación al terminar las primeras etapas.

Trabaja hasta alcanzar la máxima cobertura funcional verificable.

Si algún subsistema se vuelve bloqueante, registra el problema y continúa con los subsistemas independientes.

---

# 17. PRUEBAS OBLIGATORIAS

El CI actual ya dispone de tres escenarios:

- Baseline.
- Legacy LWJGL3.
- Argentum.

Debes conservarlos y ampliarlos.

## Tests

Ejecutar:

- Common JUnit.
- Tests Ornithe.
- Tests de persistencia.
- Tests de módulos.
- Tests de perfiles.
- Tests de HUD.
- Tests de mapas.
- Tests de entrada.
- Tests de render.
- Tests de recursos.
- Tests de compatibilidad.

## Pruebas dentro de Minecraft

Verificar:

- Arranque.
- Menú.
- Mundo.
- Multijugador cuando exista entorno de prueba.
- HUD.
- PvP.
- Cámara.
- Emotes.
- Mapas.
- Waypoints.
- Capturas.
- Configuración.
- Cambio de mundo.
- Reinicio.
- Cierre.
- Restauración de preferencias.

Las pruebas deben comprobar resultados visibles y estados reales, no solamente supervivencia durante varios ticks.

## Argentum

Repetir pruebas relevantes con Argentum presente.

Comprobar interferencias entre sus optimizaciones y:

- HUD.
- Minimap.
- Entidades.
- Item rendering.
- Fonts.
- Overlays.
- Menús.
- Crosshair.
- World Map.
- Partículas.

---

# 18. BENCHMARKS Y PRESERVACIÓN DEL RENDIMIENTO

El usuario ya ha comprobado personalmente que Ornithe ofrece un rendimiento excelente frente a Forge.

Quiero conservar esa ventaja.

Pero debemos medirla correctamente.

## Métricas

- FPS.
- Frame time p50.
- Frame time p95.
- Frame time p99.
- Memoria heap.
- Consumo de texturas.
- Tiempo de arranque.
- Tiempo de carga de mundo.
- Impacto con módulos desactivados.
- Impacto con HUD completo.
- Impacto de minimapa.
- Impacto de emotes.
- Impacto de interfaces.

## Escenarios

- Ornithe sin TFL.
- Ornithe + TFL.
- Ornithe + LWJGL3 + Argentum sin TFL.
- Ornithe + LWJGL3 + Argentum + TFL.
- Forge + TFL, cuando existan datos comparables.

No interpretar resultados de Mesa llvmpipe como benchmarks de hardware real.

No declarar mejoras de FPS sin datos.

Registrar hardware, resolución, Java y configuración de la prueba.

Si no existe acceso a GPU física, preparar un protocolo para que el product owner realice la medición en Windows.

---

# 19. EVITAR REGRESIONES MULTIVERSIÓN

Este repositorio también contiene:

- Fabric 26.3.
- Forge 1.8.9.
- Otros targets TRS no certificados como TFL.

No romper el núcleo compartido.

No cambiar APIs comunes únicamente para favorecer Ornithe.

Preferir interfaces, adaptadores y contratos reutilizables.

Ejecutar al final:

- Tests comunes.
- Build Ornithe.
- Build Fabric 26.3.
- Build Forge 1.8.9.

Los builds originales deben continuar pasando.

No modificar `main` indiscriminadamente.

No eliminar Forge todavía.

El product owner decidirá la migración definitiva después de evaluar esta versión.

---

# 20. LICENCIAS

Conservar GPL-3.0-only y atribuciones del código derivado de TRS Client.

Conservar las licencias correspondientes a Nunito, iconos y bibliotecas incorporadas.

Ornithe, OSL, Legacy LWJGL3 y Argentum deben cumplir sus propias condiciones de licencia.

Argentum se utiliza como dependencia externa opcional y no debe copiarse, modificarse ni distribuirse sin permiso.

No generar builds que incorporen binarios propietarios indirectamente.

No depender de endpoints privados de TRS.

Preparar código fuente correspondiente para los JARs publicados.

---

# 21. RELEASE

Tienes autorización para publicar una nueva prerelease experimental cuando el resultado esté suficientemente verificado.

Versión sugerida:

`v0.4.0-ornithe.2`

La versión definitiva debe determinarse conforme a los cambios y a la política del repositorio.

## Entregables

- JAR instalable Ornithe Gen2.
- Sources JAR.
- ZIP de código fuente correspondiente.
- Dependency lock.
- Checksums.
- Instrucciones de Prism Launcher.
- Changelog.
- Feature Matrix.
- Informe de compatibilidad.
- Resultados de pruebas.
- Evidencias visuales.
- Informe de rendimiento.
- Problemas conocidos.

Actualizar el PR #6 con los cambios.

Mantenerlo en borrador mientras no exista una certificación suficiente.

No fusionar automáticamente a `main`.

No marcar la release como Stable.

No publicar artefactos rotos.

No anunciar una función como operativa sin evidencia.

---

# 22. DEFINICIÓN DE TERMINADO

La misión habrá avanzado satisfactoriamente cuando:

- [ ] TFL Client Ornithe use el sistema de módulos completo.
- [ ] Las capacidades estén declaradas correctamente.
- [ ] No exista la limitación artificial de siete módulos.
- [ ] Todos los módulos aplicables tengan adaptación nativa.
- [ ] Sus configuraciones sean accesibles.
- [ ] El HUD completo esté integrado.
- [ ] El editor HUD esté operativo.
- [ ] Las funciones PvP estén adaptadas.
- [ ] Minimap y World Map funcionen.
- [ ] Los waypoints persistan correctamente.
- [ ] Chat y utilidades estén adaptados.
- [ ] Los emotes offline funcionen.
- [ ] Las herramientas Redstone aplicables funcionen.
- [ ] Los servicios externos no disponibles permanezcan bloqueados de manera clara y segura.
- [ ] La interfaz conserve la identidad moderna TFL.
- [ ] Los 79 IDs estén auditados individualmente.
- [ ] El cliente funcione con Argentum.
- [ ] No se observen regresiones graves de rendimiento.
- [ ] Las configuraciones anteriores se conserven.
- [ ] Fabric 26.3 continúe compilando.
- [ ] Forge 1.8.9 continúe compilando.
- [ ] CI esté aprobado.
- [ ] Exista un nuevo JAR experimental verificable y descargable.
- [ ] La documentación distinga correctamente funciones probadas, limitadas y no disponibles.

Si un requisito no puede cumplirse, documenta la razón técnica exacta y continúa con las demás tareas.

No declares cobertura completa mientras existan módulos aplicables pendientes.

---

# 23. INFORME FINAL

Al finalizar quiero:

1. Versión publicada.
2. Enlace de GitHub Release.
3. Rama y commits.
4. Estado del PR #6.
5. Número de módulos completamente implementados.
6. Número de módulos probados dentro de Minecraft.
7. Número de módulos parcialmente implementados.
8. Número de módulos no aplicables.
9. Número de módulos bloqueados por servicios externos.
10. Estado de HUD y editor.
11. Estado de PvP.
12. Estado de mapas y waypoints.
13. Estado de emotes.
14. Estado de utilidades.
15. Estado de Argentum.
16. Resultados de pruebas.
17. Métricas de rendimiento disponibles.
18. Resultados de Fabric 26.3 y Forge 1.8.9.
19. Limitaciones.
20. Archivos descargables.
21. Pasos de instalación en Prism.
22. Recomendación para adoptar Ornithe como plataforma definitiva.

No quiero un informe genérico.

Quiero evidencia reproducible.

---

# 24. INSTRUCCIÓN FINAL DEL PRODUCT OWNER

La primera versión Ornithe nos ha sorprendido enormemente por su rendimiento.

Ese resultado merece que llevemos esta plataforma hasta el máximo de sus posibilidades.

Tenemos un núcleo compartido muy completo, 79 módulos registrados, un sistema de configuración existente y una arquitectura multiversión.

Ahora necesitamos integrar todo ese trabajo en Minecraft 1.8.9 sobre Ornithe.

**Quiero un TFL Client 1.8.9 completo, no una demo de siete módulos.**

Quiero aprovechar su rendimiento.

Quiero una interfaz moderna.

Quiero poder personalizarlo tanto como las otras versiones.

Quiero que los módulos funcionen realmente.

Quiero conservar Argentum como motor externo.

Quiero evitar dependencias de TRS.

Quiero una implementación mantenible.

Y quiero una nueva prerelease descargable que pueda instalar en Prism Launcher para comprobar personalmente los resultados.

Puedes tomar decisiones técnicas justificadas, implementar adaptadores, reorganizar la estructura específica de Ornithe y ejecutar pruebas extensas.

No te quedes únicamente planificando.

**Audita, implementa, prueba, corrige, vuelve a probar y publica resultados verificables.**

# BEGIN — PROJECT ORNITHE II: FULL FEATURE PARITY.