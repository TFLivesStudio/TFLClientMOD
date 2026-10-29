# TFL Client 0.4.0-unified.1 — candidata para pruebas

Minecraft **1.8.9: Ornithe Gen2**. Minecraft **26.3: Fabric**. Ambos requieren Java 25. Se retiraron Forge, NeoForge, los demás adaptadores legacy y el antiguo `src/` raíz; permanecen en el historial de Git. El desarrollo continúa en una rama y PR en borrador, sin integrar en main.

## Interfaz y animación

- ESC usa navegación de texto a la izquierda, sin imagen, panel central, desenfoque ni oscurecimiento del mundo. Conserva las acciones nativas y la navegación por teclado.
- Nunito con filtrado lineal, superficies oscuras translúcidas y acento azul en menús compartidos, pantallas nativas, Diorama y los controles de gráficos de Argentum/Sodium. Las opciones continúan siendo las del optimizador instalado.
- Identidad TFL durante el arranque de 1.8.9, la carga de recursos moderna y la entrada a mundos. El proceso previo a crear una ventana pertenece al launcher; esta entrega es el mod, no un launcher independiente.
- Vídeo azul: 500 fotogramas, 16,67 s, **30 FPS nominales** en Calidad (1920×1080), Equilibrado (1280×720) y Rendimiento (854×480). Se inicia en Calidad en perfiles nuevos; los ajustes ya guardados se respetan. No hay reducción silenciosa de calidad.
- Límite de animaciones del menú: 60 FPS iniciales, configurable entre 30 y 240. Se sustituye el límite vanilla de 30 FPS del título de 1.8.9. Mini Player usa 60 FPS inicialmente; calidad del mapa alta y movimiento de nubes activado.
- Decodificación paralela acotada, dos fotogramas pendientes como máximo, reutilización de búferes y una textura de vídeo. 26.3 usa el decodificador JPEG nativo distribuido con Minecraft; 1.8.9, ImageIO. Gradiente lateral del título en una textura reutilizable, evitando decenas de capas por fotograma.

El archivo MP4 original se verificó con ffprobe: 30/1 FPS. Se conserva esa tasa sin interpolación. Los archivos personalizados anteriores a 24 FPS mantienen 24 FPS. Las cifras nominales son objetivos. El indicador del menú y los informes cuentan fotogramas nuevos realmente presentados, sin contar redibujos de la misma textura. Una máquina lenta puede presentar menos; no se certifica un mínimo universal de 30 FPS ni se acelera artificialmente el clip para simularlo. Las animaciones del HUD se actualizan en el renderizado, sujetas a los FPS reales del juego.

## Diorama

Se incorpora el código fuente MIT de [Diorama](https://github.com/SunkenInTime/diorama), commit `4ce572ea5ceac7974fe2bb1b344c52ff2e55acd4`. En 26.3 se conserva su motor nativo: proyección oblicua, terreno con volumen, sombras/AO, niebla de ocho capas, caché, almacenamiento, cuevas, radar y waypoints. TFL controla la activación y posición del minimapa mediante su HUD Editor. M abre el único mapa activo; no hace falta instalar otro JAR Diorama.

El puente importa los waypoints antiguos con UUID deterministas y sin borrar sus archivos. Los ajustes compartidos de tamaño, forma, opacidad, zoom, coordenadas, bioma, brújula, radar, nubes y cuevas se conectan al nuevo mapa. Los controles avanzados de 26.3 abren los ajustes nativos de Diorama. Las opciones del mapa anterior que ya no tienen efecto se ocultan y sus valores se conservan para migración.

En 1.8.9 se portaron Projection, MapStyle, TileBaker, VoxelReader, MapPick, Mipmaps y el algoritmo de niebla de Diorama. Se copian únicamente chunks recibidos, en el hilo del cliente; dos trabajadores trazan el volumen sobre snapshots inmutables. Hay almacenamiento comprimido por mundo/dimensión, invalidación al cambiar bloques, LOD, caché limitada, radar y minimapa circular con suavizado y resolución interna duplicada. F3+T reconstruye los recursos.

Controles 1.8.9: arrastrar para desplazar; rueda para zoom anclado al cursor; Espacio para volver al jugador; C para alternar superficie/cuevas; RePág/AvPág para cambiar la altura de corte; clic derecho para crear un waypoint sobre terreno trazado; clic en un waypoint para editarlo o eliminarlo. Se respetan las restricciones de mapa/cuevas/radar recibidas del servidor.

**El port legacy es experimental:** incluye las formas nativas de losas y escaleras, pero aproxima conexiones de vallas, paredes y escaleras interiores/exteriores. Los colores de bioma y algunas formas no tienen aún la fidelidad del motor moderno. No se han portado a 1.8.9 las integraciones de mods modernos (FTB Chunks, Waystones, etc.), el horneado GPU experimental, los importadores de otros mapas ni todos los controles avanzados de la interfaz moderna. Esto no es una certificación de paridad completa entre los dos mapas. El antiguo escáner no se ejecuta en paralelo; se retienen clases compartidas para migraciones, datos y pruebas.

## Capturas y clips locales

Capturas: herramientas nuevas de línea, elipse, relleno opaco para ocultar información y resaltador translúcido; mantienen deshacer/rehacer y guardan copias. Vista previa hasta 2560×1440 y barra en dos filas.

Clips: biblioteca local en `clips/`, miniaturas y vista previa por secuencia de imágenes. El editor permite inicio/fin en segundos, silencio, velocidad 0,5×/1×/2×, recorte vertical 9:16, giro de 90° y exportación a un MP4 nuevo. Requiere **FFmpeg y ffprobe en PATH**, con libx264 y AAC; no descarga ejecutables ni usa servicios de TRS. Hay cancelación y limpieza de temporales. La vista previa interna es un storyboard, no reproducción MP4 completa a 30 FPS. La grabación/captura instantánea del launcher anterior sigue sin backend autorizado.

## Comprobaciones

La suite común ejecutó **1.157 pruebas: cero fallos y dos omisiones de funciones opcionales**. Los nueve contratos del adaptador Ornithe y las seis pruebas Python también pasaron. Los tests de edición comprueban opacidad real de la ocultación, deshacer, geometría rotada y una exportación FFmpeg real con duración correcta, sin modificar el MP4 original.

Se ejecutaron los JAR instalables con sondas separadas que no se distribuyen: 1.8.9 con Argentum y 26.3 con Sodium, a 1920×1080 y escala GUI 4. Las sondas comprueban avance real del vídeo, sesión/terreno Diorama, propiedad de la pantalla del mapa, pausa transparente y conservación de los menús nativos. La moderna comprueba también recarga de recursos; ambas abren los editores de capturas y clips para revisar su distribución visual. El escenario de clips nativo revisa la interfaz; la exportación real se verifica en las pruebas comunes.

Los informes y capturas finales están en `docs/evidence/unified-client/` y en el ZIP de evidencia de cada publicación. Las métricas de vídeo corresponden a renderizado **por software**, Mesa llvmpipe/lavapipe, con cuota de cuatro CPU. La tabla de resultados local se guarda en `validation.json`. CI ejecuta además baseline/LWJGL3/Argentum en 1.8.9 y baseline/Sodium en 26.3; la publicación automática requiere todos esos escenarios y las pruebas comunes aprobados.

No hay certificación con GPU física, Iris/shaders ni de todos los módulos. Iris no pudo iniciar correctamente su contexto en este backend. La auditoría de las 80 entradas del registro separa inspección de código, pruebas y ejecución nativa; conserva las limitaciones de los demás adaptadores y no convierte un menú abierto en prueba de todo el gameplay.

## Instalación y licencias

1.8.9: Ornithe Gen2, Fabric Loader 0.19.5, OSL 0.22.2. Argentum 1.0.2 requiere Legacy LWJGL3 1.4.10. 26.3: Fabric Loader 0.19.5 y Fabric API 0.161.0+26.3; Sodium 0.9.2 es opcional. Los JSON incluidos contienen URLs y hashes exactos. Los binarios de optimizadores se instalan aparte.

Usa una instancia de pruebas y un solo JAR TFL de la plataforma adecuada. Las publicaciones incluyen instrucciones, hashes, evidencia y el código fuente correspondiente completo. TFL/TRS mantienen GPL-3.0-only; Diorama conserva MIT y su atribución a Dara Adedeji. No es necesario cambiar la licencia de todo TFL para incorporar código MIT. Se conservan también los avisos de fuentes, iconos, Chat Heads y fastutil.
