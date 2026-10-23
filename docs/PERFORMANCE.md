# Rendimiento

No se publican ganancias de FPS ni benchmarks inventados. La suite comprueba matemáticas de
frametimes/zoom/mapas y mecanismos offline; no mide una escena real representativa.

Los builds se ejecutan con dos workers y se reutilizan caches. Las superficies Selection usan
un número limitado de scanlines (radio <=5) sin asignar texturas por botón. Inter usa el proveedor
nativo de Minecraft y un cache acotado de componentes (512). El fondo es una textura estática,
no una decodificación continua; OFF evita dibujarla. Cerrar el título libera un reproductor
opcional, si algún día se instala uno. El controlador no descarga código ni medios.

Antes de beta: medir un mundo fijo con/sin TFL y con cada optimizador por separado, mantener
resolución, distancia, VSync, FPS cap y Java idénticos. Registrar tiempo de arranque, memoria,
FPS y frametimes p50/p95/p99 durante al menos 60 s por escenario. Medir menú estático/off;
cuando exista vídeo real, añadir sus modos y comprobar que no sigue decodificando en gameplay.
Los runners upstream de benchmark se conservan en fuentes de desarrollo, excluidos del JAR de usuario.

El smoke nativo de mapas emitió contadores y tiempos de tick/dibujo/exportación, con render
Vulkan por CPU. Demuestra instrumentación y finalización de exportes, pero no se usa para
comparar FPS contra vanilla ni predecir rendimiento con GPU/Sodium. La evidencia del runner
se resume en docs/evidence/build-validation.json.
