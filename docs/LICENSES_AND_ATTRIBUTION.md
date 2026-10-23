# Licencias, atribución y fuente correspondiente

TFL Core es una obra derivada de TRS Client MOD, © 2026 Ohev Tamerin (Theredstonee),
GPL-3.0-only. Fuente upstream fijada: c52087ddb6b73b9d1287ef6d7f9d31010fcb5702.
Las modificaciones TFL se identifican en `vendor/trs-client/TFL-NOTICE.txt` y en este historial.
No se adoptó la aplicación TRS Launcher. Los paquetes internos originales no implican afiliación.

Se conserva COPYING, `vendor/trs-client/LICENSE`, el aviso upstream completo y los encabezados
originales. Las partes Chat Heads de dzwdz mantienen MPL-2.0 y su texto en
`licenses/chat-heads-MPL-2.0.txt`, incluido en META-INF de ambos targets.
El puente e4mc conserva su licencia MIT y fuentes bajo `e4mc-bridge`; su servicio no se habilita.

Inter está bajo SIL OFL 1.1 (`licenses/Inter-OFL.txt`, incluido en el JAR), obtenido de
Google Fonts/ofl/inter. SHA-256 del TTF:
`29160a80ff49ddcab2c97711247e08b1fab27a484a329ce8b813d820dc559031`.
No se modifica ni cambia el nombre reservado de la fuente. El font provider usa el archivo original.

El fondo PNG se generó para esta misión como imagen original, sin vídeo o marca ajenos;
se entrega dentro de las fuentes del proyecto bajo los términos de la distribución GPL.
Es un fondo estático de 1672×941, no un vídeo 1080p. No se añade material audiovisual remoto.

Los optimizadores externos **no se incluyen por defecto**. Sus licencias y dependencias se
registran en el inventario, pero esa lista no sustituye auditar la redistribución de cada binario.
Los mods descargados por una instancia también pueden tener dependencias transitivas propias.

Para distribuir TFL, acompañar cada JAR del source ZIP completo generado del mismo commit con
los scripts y recursos de build. Un sources JAR con algunas clases Java no basta como fuente
correspondiente de toda la distribución. `git archive HEAD` incluye las fuentes adoptadas,
adaptadores, wrapper, recursos y cambios TFL; no incluye Minecraft, caches ni secretos.
Los fixtures de circuitos se recuperaron del mismo commit upstream y solo se usan en pruebas.

No se cambia la visibilidad del repositorio privado. El ZIP se entrega directamente al receptor
del binario (por ejemplo junto al artefacto o dentro del pack de validación). Si se publica después
una release, ese canal debe ofrecer también la fuente correspondiente y los avisos. Revisar la
integridad de esta entrega antes de cualquier distribución pública.

## Nunito en dev.2

Fuente oficial de googlefonts/nunito, SIL OFL 1.1. SHA del commit y del TTF variable en
`vendor/trs-client/licenses/Nunito-PROVENANCE.txt`. Se generan instancias estáticas 400/600/800
con fontTools, sin editar contornos. Licencia y procedencia se incluyen en ambos JAR. Los seis
emotes locales reutilizan tracks de animación distribuidos como código GPL en el snapshot;
no se descargan medios, compras, permisos de cuenta ni cosméticos privados.

## Iconos de interfaz dev.3

Lucide 0.468.0, commit `f12b0de177fbc2a6795e99be065887e72b237123`: ISC y porciones Feather MIT. Avisos completos en `vendor/trs-client/licenses/Lucide-LICENSE.txt`, incluidos en ambos JAR. SVG y hashes en `vendor/trs-client/artwork/lucide/`; atlas de 122 iconos generado con `tools/generate_ui_icons.py`. La rasterización ocurre al preparar los assets, no durante el juego.
