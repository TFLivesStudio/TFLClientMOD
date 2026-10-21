# TFL CORE 0.3 — PROJECT OVERDRIVE
## Misión maestra de desarrollo • TFL Selection • Fabric 26.3

**Organización:** TFLivesStudio
**Repositorio:** https://github.com/TFLivesStudio/TFLClientMOD
**Branch de entrega:** `main` (commits incrementales, verificados, sin force-push)
**Versión de origen:** 0.2.0-SNAPSHOT
**Versión objetivo:** 0.3.0 (alphas incrementales → candidata → versión testable)
**Target obligatorio:** Minecraft Java 26.3, Fabric, Java 25.
**Límite arquitectónico:** 100 % client-side, no requiere nada en el servidor.
**Marca:** TFL Selection / TFL Core. Identidad azul TFLives, varios temas configurables.

> **Misión:** transformar el núcleo 0.2 en un cliente multimodo, bonito, rápido, accesible y profundamente personalizable. Máximo número de funcionalidades terminadas de forma útil, nunca máximo número de checkboxes falsamente marcados. No reimplementar motores de optimización de terceros. La 1.8.9 se trabajará después.

## 0. Protocolo obligatorio para OpenCode

1. Comprobar `git status`, `git branch --show-current`, `git remote -v`, `git fetch origin`. No sobrescribir cambios de otra persona. Si `main` divergió, reconciliar de manera no destructiva.
2. Leer `README.md`, `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`, `docs/TESTING_0.2.md`, `gradle.properties` y el código actual. Auditar antes de reescribir.
3. Reproducir el build de origen: `gradlew.bat clean build` en Windows con **JDK 25**. Registrar fallos existentes sin atribuirlos a nuevos cambios.
4. Usar documentación/API **Minecraft 26.3** y Fabric actual. No copiar código de 1.20/1.21 sin migración. Este proyecto usa `GuiGraphicsExtractor` (no APIs viejas basadas en GuiGraphics.DrawContext); SDL3 usa `InputConstants.Type.KEYBOARD`.
5. Crear un **plan con dependencias**, matriz de funcionalidades y pruebas. Dividir en entregas pequeñas para asegurar builds verdes y revisar los cambios. Evitar un commit gigante.
6. Para trabajo de alto riesgo (render, mixins, shader/OptiFine/Sodium), trabajar primero en rama corta/PR o commit reversible. Los commits aprobados deben terminar en `main`. Nunca force-push, reset hard ni modificar secrets.
7. Tras cada vertical slice, ejecutar `gradlew.bat clean build`, revisar resultados JUnit y comprobar CI. Sin build/test exitoso, no hacer push a main.
8. Actualizar documentación, CHANGELOG y guía de pruebas con resultados objetivos. Diferenciar **compila**, **JUnit aprueba** e **in-game certificado**.
9. El build automático debe seguir publicando JAR instalable y reportes de test; no subir JARs a la rama como fuente.
10. Siempre priorizar que `main` sea compilable y no interrumpa el uso de la versión anterior. Abrir incidencias para módulos de riesgo que no pasen QA.

## 1. Arquitectura base (P0: prerrequisito de funciones)

- [ ] A01. Módulos con id estable, categoría, activación, desactivación, estado y dependencias.
- [ ] A02. Registro central: detectar duplicados, orden de inicialización y dependencias faltantes.
- [ ] A03. Desacoplar UI, HUD, gameplay, ajustes, performance e integraciones en paquetes y servicios.
- [ ] A04. Preferencias tipadas por módulo, con defaults, validación y schema/migración desde v1.
- [ ] A05. Guardado JSON atómico + copia segura antes de migraciones; no perder preferencias 0.2.
- [ ] A06. Perfiles globales: PvP, Survival, Builder, Exploration, Minimal y Custom.
- [ ] A07. Perfiles asignables manualmente a mundo/servidor, sin sincronización de datos privados.
- [ ] A08. Importación/exportación de perfiles con validación, límites de tamaño y sin ejecutar código.
- [ ] A09. Gestor central de atajos editable, con detección de conflictos y restauración de defaults.
- [ ] A10. Modo de recuperación: opción para desactivar render/UI de TFL y volver a Vanilla cuando falle un módulo.
- [ ] A11. Registro de errores por módulo con degradación progresiva, sin ocultar crashes críticos.
- [ ] A12. Integración de idioma mediante claves `en_us`, `es_es`, `es_mx` en todas las pantallas.

## 2. Visual y experiencia de cliente (P0 salvo lo señalado)

- [ ] V01. Rediseñar el menú inicial como interfaz propia de TFL Selection, no simplemente botones Vanilla encima de gradiente.
- [ ] V02. Sistema de diseño consistente: tipografía, spacing, tarjetas, estados hover/focus y botones.
- [ ] V03. BLUE, MIDNIGHT, FROST pulidos; colores acento ajustables y tema propio importable con validación.
- [ ] V04. Transiciones suaves configurables y **reduced motion** real.
- [ ] V05. Soporte de escalado GUI, resolución ultrawide, 16:9, pantallas pequeñas y teclado.
- [ ] V06. Acceso rápido a modos Vanilla y configuración de Core, siempre con fallback.
- [ ] V07. Menú de pausa enriquecido con acceso al Core; opción de desactivar sin eliminar opciones Vanilla.
- [ ] V08. Pantalla de configuración completa: pestañas, categorías, buscador, tooltips, restablecer.
- [ ] V09. Vista previa de cambios y guardado automático sin escribir en disco cada frame.
- [ ] V10. Pantalla inicial de bienvenida/onboarding opcional y saltable.
- [ ] V11. Fondos de pantalla estáticos de excelente calidad y transiciones de bajo coste.
- [ ] V12. **STRETCH**: escenas 3D animadas auténticas; no declararlas implementadas hasta medir presupuesto GPU, acceso seguro al motor y fallback. Nunca cargar mundos completos para decorar.
- [ ] V13. **STRETCH**: presentación de skin en el menú usando APIs seguras, sin consultar datos externos por defecto.

## 3. HUD modular + editor avanzado (P0)

- [ ] H01. Sistema de widgets con identidad, draw/update hooks, visibilidad y ajustes propios.
- [ ] H02. Editor de HUD arrastrable (drag) con preview, límites de pantalla y escala.
- [ ] H03. Snap a cuadrícula, alineación, anchors y valores editables.
- [ ] H04. Opacidad/fondo/colores/compact mode por widget.
- [ ] H05. Diseños diferentes por perfil: PvP, Survival, Builder, Minimal, Custom.
- [ ] H06. FPS (incluido en 0.2; mantener y mejorar posición/configuración).
- [ ] H07. Frametime básico y métricas de estabilidad con muestras limitadas; no confundir FPS instantáneo con percentiles.
- [ ] H08. CPS L/R calculado solo desde input del cliente.
- [ ] H09. Keystrokes WASD + salto/sprint y opción de ocultar inputs sensibles.
- [ ] H10. Ping cuando realmente existe una conexión con latencia conocida; `N/A` offline.
- [ ] H11. Coordenadas precisas y compactas, configurable.
- [ ] H12. Brújula/orientación y bioma actual cuando datos están disponibles.
- [ ] H13. Hora local opcional y día/hora del mundo.
- [ ] H14. Estado sprint/sneak.
- [ ] H15. Estado de armadura + durabilidad del equipo propio.
- [ ] H16. Efectos de pociones activos con duración.
- [ ] H17. Ítem sostenido, durabilidad y contador de stacks/hotbar cuando es válido.
- [ ] H18. Indicador de cooldown o ataque para combate.
- [ ] H19. Ocultar elementos según F1, chat/screenshots/streamer mode.
- [ ] H20. Limitar trabajo por tick/frame; textos cacheados; profiler/contadores opcionales para coste del HUD.

## 4. PvP y combate (P1, sin ventajas ilegítimas)

- [ ] C01. Crosshair completamente personalizable (forma, grosor, color, escala, condiciones).
- [ ] C02. Toggle sprint controlado por opciones de cliente sin automatización de movimientos indebida.
- [ ] C03. Indicador visual de combo **estimado** usando eventos legítimos del cliente (no estadística falsa).
- [ ] C04. Hit color y feedback visual opcional; comprobar compatibilidad con Sodium/Iris.
- [ ] C05. Partículas/sonidos de golpe configurables sin suplantar eventos del servidor.
- [ ] C06. Indicador del ítem en mano/cooldown y estadísticas de sesión observables localmente.
- [ ] C07. Editor compacto de HUD de combate, perfiles específicos.
- [ ] C08. Evitar autoclick, reach, movimiento automatizado o exposición de datos invisibles del servidor.

## 5. Survival, exploración y técnico (P1)

- [ ] S01. Zoom configurable con transición opcional y compatibilidad de FOV.
- [ ] S02. Waypoints personales persistentes por mundo/servidor; sin datos compartidos remotos.
- [ ] S03. Waypoint de última muerte (solo cuando el cliente conoce correctamente la posición).
- [ ] S04. Brújula hacia waypoint activo, distancia y orientación.
- [ ] S05. Información básica de bloque mirado: id, nombre, coordenadas y propiedades visibles al cliente.
- [ ] S06. Overlay configurable de chunks y delimitaciones técnicas.
- [ ] S07. Nivel de luz disponible localmente, sin afirmar datos de chunks no cargados.
- [ ] S08. Medidor de distancia entre dos puntos elegidos.
- [ ] S09. Contador/ayuda de coordenadas para construir y medir áreas.
- [ ] S10. Resaltado visual opcional de bloques seleccionados, sin x-ray/visión ilegal.
- [ ] S11. **STRETCH**: minimapa básico del terreno **ya renderizado o conocido** y sin revelar información desconocida.
- [ ] S12. **STRETCH**: contador de recursos del inventario, buscador local y ayudas de organización que no envíen movimientos de inventario automáticamente.
- [ ] S13. **STRETCH**: herramientas de ayuda a redstone, estrictamente según datos cliente; no inspección oculta del servidor.

## 6. Diagnóstico, rendimiento e integración (P0/P1)

- [ ] P01. Mantener perfiles BALANCED / FPS / QUALITY / CUSTOM pero convertirlos en opciones explícitas de módulos TFL, no promesas sobre otros mods.
- [ ] P02. Perfil de bajo consumo: desactivar animación, sombras/efectos decorativos del Core, actualizar widgets a frecuencia razonable.
- [ ] P03. Diagnóstico visible: FPS, frametime (mediciones p50/p95/p99 si se implementa buffer correcto), RAM y tiempos cuando accesibles.
- [ ] P04. Detección opcional de mods instalados (`FabricLoader.isModLoaded`) para informar al usuario, sin exigir mods ajenos.
- [ ] P05. Matriz de compatibilidad con la versión exacta de Sodium/Iris/FerriteCore y demás **solo tras comprobar publicaciones oficiales y sus dependencias**.
- [ ] P06. Benchmark reproducible: menú, mundo nuevo, exploración; capturar baseline Vanilla, Fabric API + Core y modpack optimizado, mismas condiciones.
- [ ] P07. Si aparece incompatibilidad, desactivar decoraciones TFL o funcionalidad conflictiva, nunca editar sin permiso configuraciones de Sodium u otros mods.
- [ ] P08. Panel de estado del Core: versión, loader, Minecraft, módulos activos, errores detectados; botón abrir logs/carpeta si API segura.
- [ ] P09. **STRETCH**: integración opcional Discord Rich Presence desactivada por defecto y protegida por permiso explícito.
- [ ] P10. **STRETCH**: importar manifiesto local autorizado del launcher TFL Client para mostrar identidad de edición; no leer tokens ni escribir fuera de la carpeta dedicada.
- [ ] P11. Telemetría desactivada; no introducir envío automático de datos en la 0.3.

## 7. Matriz de priorización

### Gate 0 — baseline y arquitectura
A01–A12, migración del config 0.2, pruebas, CI, fallback.

### Gate 1 — super update visual
V01–V11 con pantallas y escalas probadas, UI accesible, menú recuperable.

### Gate 2 — HUD completamente útil
H01–H20 (elegir implementaciones reales compatibles con 26.3 y documentar no aplicables). Editor drag/resize y presets son requisito central.

### Gate 3 — utilidad multi-modo
Implementar máxima cantidad viable de C01–C07 y S01–S10, con pruebas funcionales. Mantener independientes los módulos.

### Gate 4 — métricas reales y compatibilidad
P01–P08, escenarios reproducibles. Solo integrar funcionalidades externas con APIs reales.

### Gate 5 — stretch
V12–V13, S11–S13, P09–P10 únicamente cuando Gates 0–4 estén estables. No sacrificar FPS o estabilidad por checkboxes.

**El plan contiene más de 70 tareas atómicas de arquitectura, UI, HUD, PvP, survival, builders, diagnóstico y funcionalidades condicionales.** Una tarea no equivale necesariamente a una función visible. La versión pública solo debe declarar funcionalidades que funcionen en Minecraft.

## 8. Criterios de calidad

- El mod debe cargar y jugarse sin una instalación del lado servidor.
- Build y unit tests reproducibles en GitHub Actions, Java 25, cero errores de compilación.
- Pruebas de configuración: valores malformados, versionado, persistencia, downgrades, export/import y rechazo de rutas inseguras.
- Test de pantallas: tamaño pequeño, 1080p, 1440p, 4K, ultrawide y varias escalas GUI.
- Pruebas de atajos: Right Shift, controles vanilla, keybinds duplicados, abrir/cerrar en mundos y servidores.
- Pruebas HUD: F1, pausa, redimensionar ventana, entrar/salir de mundos, HUD con decenas de widgets, desactivar todos.
- Evaluar consumo CPU/GPU y allocations antes de concluir que algo es “ligero” o “mejor FPS”.
- Pruebas con shaders/render mods instalados y sin ellos; fallback visible y no-crash.
- Diferenciar funciones client-side permitidas de funciones que servidores pueden restringir.
- Sin credenciales, servicios remotos obligatorios, telemetría involuntaria ni código remoto.
- Preferir API Fabric pública, minimizando mixins; cada mixin requiere justificación y test.
- Entregables: JAR, reporte de CI, changelog, matriz de features realmente completas, screenshots del test manual, incidencias conocidas, checklist de aceptación.

## 9. Secuencia de commits recomendada

1. `refactor(core): modularize features and migrate settings v2`
2. `feat(ui): rebuild navigation, themes and accessibility`
3. `feat(hud): introduce configurable draggable widget editor`
4. `feat(hud): add metrics, status and survival widgets`
5. `feat(pvp): introduce configurable combat toolkit`
6. `feat(exploration): add waypoints, zoom and technical overlays`
7. `perf(core): add diagnostics and compatibility guards`
8. `test(core): extend automated and manual QA matrix`
9. `docs(release): prepare 0.3 candidate and accurate changelog`

Cada commit es una orientación, no una excusa para esconder grandes cambios. Dividir aún más donde convenga.

## 10. Entrega y aprobación

Primero entregar una `0.3.0-alpha.1` funcional con sistema de módulos + interfaz. Después incorporar HUD, PvP, exploración y rendimiento en alphas posteriores, hasta una candidata verificable. Mantener CI verde y **no fusionar implementaciones inacabadas que rompan el cliente**. Avisar con precisión cuando una tarea sea experimental, esté incompleta o necesite test manual.

### Primer mensaje de arranque para OpenCode
> Lee `docs/MISSION_0.3_OVERDRIVE.md`. Haz una auditoría real de `main`, produce un plan técnico por dependencias y comienza por Gate 0. Tienes autorización de realizar commits y push a `main` tras builds y tests verdes, sin sobrescribir trabajo remoto. Sigue hasta completar tantos Gates como sea técnicamente viable con calidad, documenta cada entrega y no prometas que los módulos aún no implementados funcionan.
