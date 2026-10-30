# TFL Client 0.4.0-audit.2 — mapa, fuentes y ESC

Esta revisión se limita a los problemas reportados después de audit.1. No cambia otros módulos, ajustes gráficos, vídeo ni perfiles.

- **Fuentes al activar el minimapa (1.8.9):** el mapa usa un ámbito explícito para programa, framebuffers, viewport, matrices, unidades de textura, samplers, buffers, máscara de escritura de color y estados de dibujo. Se retira `glPushAttrib/glPopAttrib` del target del minimapa: restaurar atributos por esa vía puede dejar la caché de Minecraft desincronizada. El ámbito también reconcilia los enlaces de textura antes de cargar las texturas propias del mapa. El target propio habilita la escritura de alfa y su mezcla antes de dibujar, y restaura ambos estados después: heredar la máscara del framebuffer principal podía dejar el minimapa transparente. No se sustituyen fuentes ni se modifica el atlas de Minecraft.
- **Nubes del port de Diorama (1.8.9):** se eliminan las tres capas de textura plana que se dibujaban detrás de la niebla. El origen, el crecimiento del ruido al alejar el zoom y sus parámetros siguen el cálculo de Diorama; la máscara incluye el alcance de sus consultas. Se conserva su integración de ocho capas y la vista oblicua. El port continúa usando el trazador CPU de Diorama; no se presenta como el backend GPU moderno.
- **ESC, ambas versiones:** velo negro de 64/255 (25,1 %), botones en la distribución lateral anterior y X anclada a la pantalla. El historial y el campo de texto ocupan la esquina inferior derecha. Las coordenadas de los componentes clicables siguen ese desplazamiento; el chat vuelve a su disposición habitual al cerrar ESC. No se cambian las preferencias guardadas de chat/HUD.

## Comprobaciones

La sonda de 1.8.9 compara los píxeles de texto dibujado por el renderer nativo antes y después de alternar el minimapa y de dibujar el mapa con niebla. Incluye una unidad de textura activa distinta de cero y comprueba su restauración. Las imágenes del texto se incluyen en la evidencia. El texto incluye números y caracteres españoles; una imagen vacía no cuenta como aprobada.

Ambas sondas comprueban el campo de texto en el lado derecho, el envío e historial nativos y la detección de componentes clicables en la región inferior derecha. Las capturas muestran el menú, su velo y el chat con mensajes; 1.8.9 incluye además una vista de nubes en la frontera explorada. Las pruebas comunes cubren los límites del panel en distintos tamaños GUI y el valor de opacidad.

La publicación se condiciona a la matriz nativa de 1.8.9 baseline, LWJGL3 y Argentum, y 26.3 baseline y Sodium, además de las pruebas comunes. La ejecución local ampliada de 1.8.9 utiliza también Lucerna y Amplio. Las sondas están separadas y excluidas del JAR instalable.

## Instalación

Java 25. Minecraft 1.8.9: Ornithe Gen2 y OSL; Minecraft 26.3: Fabric y Fabric API 0.161.0+26.3. Sustituye el JAR anterior de TFL; no instales ambos a la vez. No se necesita borrar la configuración ni los datos del mapa. Los optimizadores se instalan por separado.

Las pruebas de esta publicación utilizan Mesa llvmpipe/Lavapipe en Linux. No sustituyen una comprobación del controlador de Windows ni certifican todas las combinaciones de mods o shaderpacks. Los hashes y las fuentes corresponden al commit indicado en `build.json`.
