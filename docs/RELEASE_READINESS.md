# Release readiness — historial dev.1

**Estado vigente dev.2:** [FINAL_EXPERIENCE.md](FINAL_EXPERIENCE.md). Release preliminar autorizada para pruebas internas de ambos targets. Nunito, emotes locales y decoder Motion JPEG sustituyen los pendientes equivalentes de este registro; la certificación completa y el clip cinematográfico siguen abiertos.

**Estado: candidata de desarrollo para validación interna. No beta, RC ni stable.**
La misión todavía no cumple su definición completa de terminado y no se recomienda fusionar a main.

## Verificado

- Un build activo basado en TRS common/Fabric/legacy; la raíz ya no compila el Core antiguo.
- Fabric 26.3 adaptado compila; JAR instalable con identidad tflcore, dependencias, entrypoints,
  mixins, recursos y avisos inspeccionados. Bytecode Java 25 (major 69).
- Forge 1.8.9 adaptado compila con bytecode Java 8 (major 52); 50 pruebas sin fallos.
- Common: 1104 pruebas detectadas, 1102 aprobadas, 2 opt-in omitidas, 0 fallos/errores.
- Herramientas de distribución: 6 pruebas aprobadas; detección de clientes duplicados y rechazo
  de fuentes/dev como artefacto instalable. Escaneo básico de secretos sin hallazgos.
- Smoke real **de desarrollo Forge**: título, menú de módulos, zoom, perfiles, diálogos,
  mundo local y pausa; 17 capturas. No equivale a validar el JAR instalado en una instancia limpia.
- Fabric 26.3 **en desarrollo** completó menús y un smoke de mapas: minimapa, waypoints,
  dimensiones, mapa mundial y exportación PNG con comprobación de archivos.
- JAR Fabric **de producción** arrancó en una instancia separada con solo TFL + Fabric API;
  título/Core/Configuration/créditos visibles, export/import/reset verificados por clicks reales
  y assertions de archivos/backups, cierre normal y BUILD SUCCESSFUL. No contiene el harness.
- Código de servicios TRS conservado pero integración cerrada; física de capas offline conservada.
- Configuración separada, backups, protección de versiones futuras, preservación de claves,
  importación de widgets/waypoints compatibles y export/import/reset desde Configuration.
- Manifiesto local, hashes, packager mrpack, inventario de 79 módulos y disponibilidad real de
  optimizadores externos. Source ZIP y licencias forman parte de la entrega de desarrollo.
- Workflow adaptado incluye ambos builds, tests, licencias/recursos/metadatos, secretos, hashes
  y artifacts de desarrollo. El resultado del CI remoto no se infiere de los checks locales.

## Bloqueos de publicación

1. **Certificación funcional incompleta.** Hay smokes reales de desarrollo y arranque/operaciones
   del JAR instalado, pero no una validación exhaustiva de todos los módulos, teclas, HUD/editor,
   restauración de estados, servidores y ciclo de vida. Completar TESTING.md antes de beta/RC.
2. **Rework visual necesita revisión final multiplataforma.** Inter y las pantallas principales se
   verificaron en Fabric usando Vulkan/Lavapipe software. OpenGL del host no admite el visual
   solicitado. Probar GPU real, OpenGL, distintas resoluciones y escalas. Forge conserva tipografía
   fallback y algunos elementos legacy. Las previews software están separadas de capturas reales.
3. **Migración parcial deliberada.** No se inventan offsets proporcionales sin resolución original;
   keybinds de options.txt y presets sin equivalencia requieren revisión manual. Datos originales
   siguen preservados. Probar configuraciones reales y casos de downgrade antes de upgrade público.
4. **No hay vídeo real.** Existen contrato de player, presets, estático/off, cierre y fallback;
   no existe decoder ni clip de vídeo distribuible validado. El PNG no se presenta como vídeo.
5. **Sin certificación del modpack.** Disponibilidad de Sodium/Lithium/etc. comprobada por API,
   pero no se probaron juntos ni con shaders. No hay benchmarks de partida.
6. **Integraciones no aprobadas permanecen fuera de servicio**, incluyendo cuentas, social,
   cosméticos que necesitan proveedor no disponible y grabación del launcher. No sustituir estos
   sistemas por endpoints ficticios ni habilitar TRS para aparentar completitud.

El objetivo inmediato viable es crear un modpack interno de pruebas con la candidata y resolver esos
bloqueos. La ausencia inicial de drivers Vulkan se corrigió con Lavapipe; no queda como bloqueo de arranque. No hay feed/URL de release inventado ni publicación estable. La rama mantiene rollback a
main y a los backups de configuración. El entorno cloud guarda instrucciones reutilizables en un
borrador separado; publicar ese entorno no publica una release de TFL Core.
