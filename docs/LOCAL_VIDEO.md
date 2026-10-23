# Reproductor local de vídeo

TFL Client dev.2 implementa Motion JPEG baseline (flujo de imágenes JPEG, no MP4/H.264),
a 24 fps. No ejecuta ffmpeg ni descarga decoders, ejecutables o medios durante el juego.
Cada preset limita resolución y frecuencia de uploads; un worker lee y decodifica, el render
consume solo el último frame. Se conservan aspect ratio, cancelación y fallback estático.

Instalación opcional de un vídeo propio y con derechos: convierte fuera del juego con tu
instalación de ffmpeg, por ejemplo:

```sh
ffmpeg -i video-autorizado.mp4 -vf fps=24 -an -c:v mjpeg -q:v 4 -f mjpeg menu.mjpeg
```

Copia el resultado en `config/tflcore/menu.mjpeg` y elige Quality/Balanced/Performance en
el módulo del menú. Static/Off y Reduced Motion nunca arrancan el decoder. El vídeo debe
tener menos de 256 MiB, frames baseline de menos de 16 MiB, hasta 4096 por dimensión y
8 388 608 píxeles. No se admiten symlinks del archivo. No hay audio. Archivo ausente, inválido
o recurso gráfico fallido conserva la imagen estática y el menú utilizable. El caso de archivo
ausente permanece en fallback hasta cambiar de modo o reiniciar.

La candidata no incluye un clip de agujero negro final. Los fixtures de vídeo de las pruebas
son imágenes sintéticas de diagnóstico, no material de marketing ni evidencia de calidad
cinematográfica. Quedan pendientes benchmark 1080p/4K en GPU real y elección de medio.

Prueba de producción aislada (desde vendor/trs-client):
`./gradlew :fabric:26.3:runProdClient -PtrsVersions=26.3 -PtflProdGameDir=run/prod-26.3-dev2-clean`.
El override solo cambia el directorio de la instancia de prueba; no se usa el harness en producción.
