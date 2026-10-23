# Continuación concreta

Último commit de implementación verificado: `41aaf4e` (TFL identity/offline/config/Selection).
Rama: `adopt/trs-client-foundation`; main no se modifica ni fusiona. El commit posterior añade
herramientas, CI, fuentes de evidencia y documentación de distribución.

## Estado

Builds adaptados Fabric 26.3 y Forge 1.8.9 aprobados. Common: 1102 passed + 2 skipped;
legacy: 50 passed; packaging: 6 passed. Artefactos y source ZIP se generan en
`distribution/build/` (ignorado). Hay capturas reales Forge en docs/evidence/forge-1.8.9,
y previews Canvas/Java2D separadas. Hay smokes nativos Fabric de menús/mapas y operaciones del JAR de producción con Vulkan/Lavapipe.

## Próxima acción principal

Completar en GPU real/OpenGL y con sesión online el checklist de TESTING.md. El arranque en
esta máquina ya funciona con Xvfb + Lavapipe; source de `/workspace/.cloud-setup/activate-graphics.sh`
y `bash /workspace/.cloud-setup/start-graphics.sh` preparan el display. No se necesitan servicios TRS.
Los smokes actuales no certifican todos los módulos, audio, servidores o un pack de optimizadores.

Después: validar migraciones con archivos reales, completar conversiones seguras y verificar
configuración avanzada, HUD/editor, mapas/waypoints y módulos principales. Incorporar decoder
solo tras auditar licencia/medio y medir rendimiento; conservar fallback sin descargas automáticas.
Añadir optimizadores uno por uno según distribution/optimization-availability.json.

## Reanudar builds y empaquetado

```sh
# En esta máquina cloud:
source /workspace/.cloud-setup/activate.sh
cd /workspace/TFLClientMOD
./gradlew --no-daemon build
./gradlew --no-daemon legacyBuild
python3 -m unittest discover -s tools -p 'test_*.py'
python3 tools/scan_secrets.py
python3 tools/distribution.py verify vendor/trs-client/fabric/versions/26.3/build/libs/tflcore-fabric-26.3-0.4.0-dev.1.jar
# Solo con todos los cambios comprometidos:
mkdir -p distribution/build
git archive --format=zip --output=distribution/build/tflcore-corresponding-source.zip HEAD
python3 tools/distribution.py package vendor/trs-client/fabric/versions/26.3/build/libs/tflcore-fabric-26.3-0.4.0-dev.1.jar
python3 tools/distribution.py mrpack distribution/build/tflcore-fabric-26.3-0.4.0-dev.1.jar distribution/build/tflcore-corresponding-source.zip
```

Para repetir el smoke legacy: iniciar Xvfb de la carpeta .cloud-setup/tools/xvfb con DISPLAY propio,
activar `LIBGL_ALWAYS_SOFTWARE=1`, y ejecutar desde vendor/trs-client/legacy
`./gradlew :1.8.9:runClient -PtrsVersions=1.8.9 -PtrsAutotest -PtrsAutotestOnly=menus`.
No se supone que los procesos de displays anteriores sobrevivan a una tarea nueva.
Los logs íntegros de esta sesión están en /workspace/.cloud-setup; el resumen durable está en
`docs/evidence/build-validation.json`. No publicar ni llamar stable a esta candidata mientras
RELEASE_READINESS.md conserve bloqueos críticos.

Para el smoke Fabric usar también `VK_ICD_FILENAMES=/workspace/.cloud-setup/lvp-icd.json`
(o source activate-graphics.sh). Comandos desde vendor/trs-client:
`./gradlew :fabric:26.3:runClient -PtrsVersions=26.3 -PtrsAutotest -PtrsAutotestOnly=menus`
y después `-PtrsAutotestOnly=maps`. `runProdClient` ejecuta el JAR real y se cierra desde la UI;
el harness no se incluye allí. No interpretar un timeout de una ventana abierta como un test pasado.
