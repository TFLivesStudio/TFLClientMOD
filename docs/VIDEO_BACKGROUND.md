# Fondo del menú — registro histórico dev.1

**Actualización dev.2:** existe un decoder real local Motion JPEG en worker. Ver [LOCAL_VIDEO.md](LOCAL_VIDEO.md). El texto siguiente documenta el estado anterior; sigue pendiente el clip cinematográfico final y su benchmark.

Incluido: PNG original de agujero negro, 1672×941, disco azul/blanco, cargado como textura de
recursos. No se denomina vídeo ni se anuncian 1080p de reproducción. El archivo está en
`common/src/main/resources/assets/trsclient/textures/menu/black-hole.png`.

`MenuBackground` ofrece HIGH (1920×1080/30 fps), BALANCED (1280×720/24), PERFORMANCE
(854×480/15), STATIC y OFF. Las tres primeras son **presupuestos para un decoder futuro**;
actualmente recurren a estático. `Player` exige latest() no bloqueante y un start/close explícito.
Los adaptadores Fabric/legacy cierran recursos al abandonar el título. Una excepción en start,
latest o close no debe impedir el fallback; hay pruebas de estos casos. El modo sin animación
impone STATIC. No se descarga vídeo, decoder ni ejecutable en runtime.

## Investigación realizada (2026-10-23)

- Upstream `MiyakoVideoScreen` reproduce una secuencia JPEG a 12 fps con STB y audio OpenAL;
  es un intro de un evento/usuario concreto con medios privados. No se reutilizan esos medios,
  su bloqueo de teclado ni la descarga asociada como wallpaper TFL.
- WaterMedia: el repositorio consultado redirige a https://github.com/SrRapero720/watermedia
  (rama 2.1.x). GitHub informa licencia NOASSERTION; no se adopta código/binario sin revisar
  sus términos reales y compatibilidad de loaders/versiones.
- FancyMenu: https://github.com/Keksuccino/FancyMenu, licencia no reconocida por la API
  consultada. No se asume permiso GPL-compatible ni soporte 26.3 por su nombre.
- JCodec: https://github.com/jcodec/jcodec, BSD-2-Clause según sus metadatos. Es una alternativa
  Java para decodificar contenedores/codecs concretos, no un reproductor Minecraft listo.
  Faltan validar sus restricciones, consumo de CPU, upload de texturas y 1080p sobre el target.

No se incorpora ninguna de esas dependencias en esta candidata. Para completar vídeo se necesita
un clip real con derechos de distribución documentados y un decoder probado: worker acotado,
un único frame pendiente, loop por tiempo monotónico, cancelación/cierre, límites de archivo,
y benchmark. No basta desplazar la imagen estática ni cambiar una bandera de modo.
