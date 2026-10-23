# TFL Client · Final Experience · 0.4.0-dev.2

Candidata **development**, publicada como prerelease por solicitud del product owner. No RC ni stable.
La misión visual y funcional completa conserva pendientes; esta entrega mejora dev.1 sin certificar
funciones que no se probaron. Se mantienen ID `tflcore`, config/tflcore y namespaces técnicos.

## Transformación comprobable

- Nunito local oficial, OFL, instancias 400/600/800 y proveedores nativos con oversampling 4 en
  Fabric. Forge ahora dibuja los menús propios mediante un atlas AWT a 4× y filtrado lineal;
  usa vanilla si el atlas falla. No se renderiza la interfaz completa en un framebuffer pequeño.
- Causa visual localizada: geometría en píxeles GUI enteros, fuente vanilla en legacy y
  componentes vanilla, y sampling sin blur del fondo. Ahora se subdividen las curvas, se carga
  Nunito y el PNG declara filtrado suave. No se afirma que todas las superficies vanilla estén
  sustituidas: algunos títulos, widgets especiales e iconos requieren revisión adicional.
- Menú a la izquierda, tipografía y controles compartidos; título de ventana Fabric TFL Client.
  Pausa Fabric reordenada manteniendo callbacks originales, iconos compactos, navegación/narración
  de widgets. Carga de resource packs con identidad propia y progreso expuesto por Minecraft.
  Se conserva el motivo de redstone de carga de mundos, sin demoras artificiales.
- Nombres visibles/localizaciones y encabezados de galería/editor/ventanas adaptados. Avisos
  GPL, créditos de TRS/Ohev y licencias permanecen; no se renombra código/hosts/IDs a ciegas.
- Options.load en Fabric omite la presentación introductoria y marca el tutorial básico como NONE.
  No cambia narrador, opciones de accesibilidad, seguridad, cuentas ni sistema general de toasts.
- Seis emotes locales, rueda y persistencia ya existente de ajustes/slots. Estado/playback local
  independiente de cuenta, catálogo remoto y transportes. Movimiento/ataque/agacharse cancela;
  restaura cámara y limpia al salir del mundo. No se sincroniza con otros clientes.
- Decoder real Motion JPEG baseline local, worker acotado, un frame pendiente, upload en render,
  sin audio, límites de archivo/dimensiones y cierre/fallback. Falta vídeo cinematográfico final;
  se entrega el PNG existente con mejor sampling. No confundir fixtures de diagnóstico con arte final.

## Repositorios y evidencia

Mod: rama `adopt/trs-client-foundation`, PR #5. Launcher: rama `polish/tfl-client-identity`, commit
`b06fee1`, PR https://github.com/TFLivesStudio/TFLClient/pull/12. Launcher auditado desde ae24e91:
no se encontraron marcas TRS en src/src-tauri; se actualiza la identidad activa del catálogo y
se usa Nunito por defecto sin sobreescribir preferencias. Se conserva historial de release notes.
No hay IPC nuevo ni credenciales/configuración compartidas. No se sustituye el launcher.

Launcher: instalación Bun 1.4.4 con lockfile congelado; svelte-check 0 errores, 7 warnings en
componentes existentes; build Vite aprobado. No se validó runtime de escritorio Tauri.
Mod: recuentos finales, hashes y resultados nativos nuevos en `evidence/final-experience/validation.json`.
Las capturas dev.1 permanecen en `evidence/fabric-26.3/`; las nuevas en `evidence/final-experience/`.
Son capturas reales de Minecraft, no renders Java2D. El renderer cloud es software, no GPU física.

## Pendientes antes de RC

- Arte cinematográfico final, benchmark vídeo 1080p/4K, memoria y frametimes en GPU real.
- Revisión exhaustiva de 79 módulos y HUD/editor, skins/cosméticos locales, restauración de estados,
  perfiles/configuraciones reales, servidores autenticados, shaders/optimizadores y audio.
- Escalas GUI/resoluciones 1440p/4K/ultrawide, consistencia de widgets especiales y traducciones;
  varias descripciones nuevas usan inglés como fallback. No se declara toda la interfaz definitiva.
- Introducción/tutorial específico en instalación limpia Fabric, manteniendo accesibilidad;
  comprobar también los casos con resource packs externos. La lógica se apoya en campos/métodos
  reales de 26.3; falta certificar todas las combinaciones de primer inicio.
- Forge conserva adaptaciones visuales propias y superficies vanilla; no tiene el rework completo
  de pausa/sliders/primera ejecución de Fabric. No se certifican otros targets multiversión.
- La sincronización multiplayer de emotes necesita protocolo explícito y clientes compatibles o
  extensión de servidor futura. Vanilla no puede mostrar estas animaciones a todos por sí solo.

Se congela el alcance de dev.2 tras sus correcciones y se recomienda QA interno con instancia nueva,
no merge a main ni actualización automática estable. La release adjunta ambos targets por separado,
fuentes GPL completas, licencias y hashes. Las fuentes automáticas del tag se complementan con el
ZIP explícito correspondiente al commit de esta entrega.

## Informe solicitado (21 puntos)

1. **Transformaciones:** identidad TFL Client, Nunito, navegación izquierda, pausa/sliders Fabric,
   carga de recursos, primer inicio, emotes locales y decoder local descritos arriba.
2. **Git:** mod `16b999e` en adopt/trs-client-foundation; launcher `b06fee1` en polish/tfl-client-identity.
   El commit de documentación/evidencia posterior queda en el tag de la candidata. No se fusiona main.
3. **Antes/después:** [título dev.1](evidence/fabric-26.3/production-title.png) →
   [título dev.2](evidence/final-experience/production-fresh-title.png);
   [pausa dev.1](evidence/fabric-26.3/dev-pause.png) → [pausa dev.2](evidence/final-experience/fabric-pause.png).
4. **Capturas reales:** carpeta evidence/final-experience; se incluyen Fabric producción/dev y Forge dev.
5. **Main menu:** alineado a la izquierda, responsive en tamaños unitarios y ejecutado a 854×480;
   falta certificar escalas extremas y aceptación artística final.
6. **Vídeo:** decoder Motion JPEG implementado y probado con frames reales de diagnóstico;
   worker/loop/límites/aspect probados, GPU/upload y 1080p/4K en hardware real pendientes.
7. **Nunito:** recursos oficiales/instancias/licencia en ambos JAR. Proveedores Fabric y atlas Forge
   comprobados en Minecraft; widgets y títulos vanilla especiales pueden conservar su tipografía.
8. **Pantallas:** título, módulos, configuración, perfiles, sliders, capturas/editor, pausa y recarga
   adaptados. El HUD/editor/waypoints conserva la base común; no se certifica todo el rework final.
9. **Tutoriales:** primer arranque Fabric nuevo comprobado tras corregir la salida temprana sin
   options.txt; tutorialStep:none y onboardAccessibility:false guardados, narrador accesible y sin
   bloqueo general de toasts. No es prueba exhaustiva de todas las variantes de tutorial.
10. **Cargas:** recursos tiene motivo de órbita/progreso real; mundo conserva redstone. Conexiones
    conservan fases/textos reales del juego; falta una revisión visual específica de cada servidor.
11. **Branding eliminado:** textos de producto/localizaciones, encabezados de ventanas/editor,
    cargas y título de ventana. IDs, archivos persistentes y namespaces no se alteran.
12. **Branding conservado:** créditos y derivación TRS/Ohev, copyright, avisos GPL/MPL, procedencia
    de fuentes y snapshot histórico. No se declara autoría original TFL del código adoptado.
13. **Emotes:** seis locales; rueda/reproducción/cancelación/cámara verificadas por assertions y
    capturas reales Fabric. Sin red/cuenta; sincronización multiplayer no implementada.
14. **Paridad:** 79 módulos conservados en modules.json/FEATURE_MATRIX; validación por módulo
    exhaustiva pendiente. No se consideran operativos servicios remotos por conservar sus clases.
15. **Repos separados:** mod PR #5; launcher PR #12. Identidad/default Nunito probados con build/check
    web; no se ejecutó el launcher Tauri ni se creó una nueva integración IPC.
16. **Builds:** Fabric 26.3 Java 25 y Forge 1.8.9 Java 8 aprobados; auditoría de JARs y licencias.
17. **Tests:** common 1111 detectados, 1109 passed, 2 opt-in skipped; Forge 50 passed; Python 6 passed.
    Smokes reales de menús/mundo en ambos; emotes en Fabric y arranque Fabric de producción limpio.
18. **Artefactos:** release v0.4.0-dev.2 con ambos JAR separados, mrpack Fabric, manifiestos,
    código correspondiente completo, licencias y SHA-256. No hay release del launcher en esta tarea.
19. **Límites:** audio/sesión autenticada no disponibles en cloud; render software, sin benchmarks
    de GPU ni combinación certificada de optimizadores; pendientes visuales/multiversión arriba.
20. **Bloqueos siguientes:** clip final con derechos, QA de 79 módulos/HUD/editor/migraciones,
    resolución/idiomas y servidores reales. Forge requiere más paridad visual antes de RC.
21. **Recomendación:** feature freeze de dev.2 y QA interno con instancia nueva, pruebas por módulo
    y logs reproducibles. Release preliminar autorizada; mantener stable/main protegidos.
