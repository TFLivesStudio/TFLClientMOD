# Pruebas de Operation Modpack Ready

Se separan cuatro evidencias: build upstream importado, build TFL adaptado, lógica automatizada
y ejecución real de Minecraft. El workflow histórico #39906626198 solo demuestra compilación
upstream; no se usa como certificación de esta versión.

## Comandos y resultados locales

- Raíz: `./gradlew --no-daemon build` delega a common + Fabric 26.3.
- Directo: desde `vendor/trs-client`, `./gradlew --no-daemon --max-workers=2 :common:test :fabric:26.3:build -PtrsVersions=26.3`.
- Legacy: desde `vendor/trs-client/legacy`, `./gradlew --no-daemon --max-workers=2 build -PtrsVersions=1.8.9`.
- Distribución: `python3 -m unittest discover -s tools -p 'test_*.py'`.
- Artefacto: `python3 tools/distribution.py verify <JAR instalable>`.

Los recuentos finales y hashes están en `docs/evidence/build-validation.json`. Los informes XML
se encuentran en common/build/test-results/test y legacy/versions/1.8.9/build/test-results/test.
No se contabilizan pruebas omitidas como aprobadas. La suite upstream contiene dos pruebas
opt-in para E4mcLiveTest (servicio externo) y NetBench (benchmark de red); se conservan como omitidas con su razón en los XML.

Las nuevas pruebas comprueban bloqueo previo a conexión, destinos loopback del harness,
default offline, configuración futura en solo lectura, copias previas no destructivas,
preservación de claves desconocidas, reset de campos conocidos, waypoints server:/sp:,
conflictos de destino, import/export/traversal, fallback y cierre de decoder, créditos locales,
layout del título y rechazo de clientes duplicados/metadatos de artefactos.

Los tests de lógica online que se conservan utilizan servidores falsos/loopback explícito,
no habilitan endpoints TRS reales. Los 18 fallos iniciales de circuitos se debían a fixtures
omitidos del upstream padre, recuperados desde el mismo commit y verificados por Git blob ID.
No se deshabilitaron esas pruebas para conseguir un build verde.

## Ejecuciones reales

Se prepararon Xorg dummy/Xvfb y Mesa Lavapipe con paquetes Debian verificados por APT. OpenGL
no pudo crear el visual GLX solicitado; el primer Vulkan falló por falta de un ICD. Instalar el
ICD Lavapipe resolvió la preparación: Vulkan 1.4.305 con render software llvmpipe. No se desactivó
TLS, checksums ni la verificación de paquetes. Instrucciones reutilizables guardadas en el entorno.

Fabric de desarrollo completó `runClient -PtrsAutotest -PtrsAutotestOnly=menus`: título,
Core, zoom, perfiles, formularios, pausa y mundo local. `-PtrsAutotestOnly=maps` también completó:
minimapa, waypoints, mundo/dimensiones, zoom del mapa, Fair Play y exportación PNG (archivos
verificados por el runner). Estas son pruebas de recorrido y algunas assertions, no tests exhaustivos.
Las pantallas sociales del harness usan datos falsos y no certifican servicios reales.

El primer arranque detectó un recurso Inter mal resuelto (`font/font/inter.ttf`), corregido y
cubierto ahora por el verificador del JAR. Probar el JAR separado del classpath dev detectó una
referencia residual a AutoTest: se aisló mediante DevelopmentHooks y se movieron los contadores
de hooks a perf. El verificador ahora rechaza enlaces a clases dev ausentes. Ambos defectos se
corrigieron; no se confunde el primer build con la candidata verificada.

El **JAR Fabric de producción** se abrió con `runProdClient` sin harness en `run/prod-26.3`,
con TFL + API. Se comprobaron título, Core, Configuration y créditos. Los botones reales de
export/import/reset se ejercitaron con eventos de ratón y assertions del JSON y backups:
exportó 79 módulos, importó FPS=false, restableció FPS=true y conservó las copias. Se cerró por
Quit Game y el proceso terminó con BUILD SUCCESSFUL. Capturas `production-*.png` en
`docs/evidence/fabric-26.3/`. No se certifica con esto la partida completa del JAR ni un servidor.

Forge 1.8.9 completó su smoke de menús/mundo de desarrollo: 17 capturas, título, Core, zoom,
perfiles, diálogos y pausa. Seis capturas inspeccionadas en `docs/evidence/forge-1.8.9/`.
No se certifica el JAR Forge fuera del entorno dev. Se observaron advertencias del runtime
legacy (FML dev, appender/Twitch) y autenticación no disponible, sin impedir completar el recorrido.

Minecraft vanilla emitió errores de autenticación/Realms sin sesión online; en producción también
faltó un dispositivo OpenAL y el juego desactivó audio. No son servicios TFL habilitados.
No se probó multijugador autenticado ni audio. Un intento UI se interrumpió por timeout y dejó
un capturador X11 bloqueado; se cerró ese proceso de prueba y se repitió el flujo completo con éxito.
Las previews `software-previews/` son Java2D/Canvas y **no son capturas de Minecraft**.

## Checklist indispensable antes de beta/RC

- [x] JAR instalable + dependencias exactas, instancia separada sin harness: arranque/Core/config/cierre.
- [ ] Partida y multijugador completos con sesión normal y audio.
- [ ] Título: fondo, Inter, foco/teclado y botones a 1920×1080, 1280×720 y GUI pequeña.
- [ ] Right Shift: buscar/activar/desactivar módulos, ajustes avanzados, ayuda/créditos y navegación.
- [ ] Mundo: HUD/editor, anclas, escala, mover/cerrar pantallas, cambio de idioma y pausa.
- [ ] Zoom, sprint/sneak, crosshair, PvP informativo; comprobar restauración al cerrar/cambiar mundo.
- [ ] Minimapa/mapa, dimensión, waypoints manuales/de muerte y persistencia entre reinicios.
- [ ] Servidor: entrar/salir, resource pack aceptado/rechazado, keybinds y límites del servidor.
- [ ] Migración con configuraciones reales TFL v2/TRS y rollback con copias intactas.
- [ ] Confirmar que no hay login/sync/friends/hosting/TRS IPC automáticos.
- [ ] Un optimizador cada vez; luego combinación completa con shader si corresponde.
- [ ] Medir arranque, RAM y frametimes; no atribuir ganancias sin comparación reproducible.
- [ ] Fuente correspondiente, licencias, hash, instalación limpia y canales antes de distribuir.

## Actualización Final Experience dev.2

Ver FINAL_EXPERIENCE.md y evidence/final-experience/validation.json. Nuevos tests de emotes
locales sin objeto online, cámara al salir, parser MJPEG con marcador en APP, streams truncados,
aspect ratio y loop en worker sin contexto gráfico. Common: 1111/1109 passed/2 skipped.
Se repitieron smokes Fabric/Forge y se amplió Fabric con assertions reales de emote/cámara.
La primera instancia de producción nueva aún mostraba el onboarding: Options.load retornaba
antes del TAIL si no había options.txt. Se inyecta en todos los RETURN y al final del constructor;
otra carpeta nueva llegó directamente al título y guardó tutorialStep:none/onboardAccessibility:false.
El JAR final se cerró por Quit Game con BUILD SUCCESSFUL. Audio y autenticación siguen sin
validarse. Un fallo intermitente ShareTest (closed en lugar de not_allowed al cerrar el servidor
falso) no se ocultó: se repitió la suite completa al corregir los cambios de branding y pasó.
