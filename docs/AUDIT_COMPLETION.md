# TFL Client 0.4.0-audit.1 — correcciones de la auditoría

Actualización de `0.4.0-stabilize.1` para Minecraft 1.8.9 con Ornithe Gen2 y Minecraft 26.3 con Fabric. Ambos JAR requieren Java 25. No son compatibles con Forge. Se conserva el vídeo, las animaciones y la calidad visual; los modos de ahorro siguen siendo opciones explícitas.

## Cambios implementados

### Colores y Amplio

La interfaz pide confirmación antes de activar un escalador con Colores habilitado. Al aceptar libera la cadena de postprocesado y desactiva Colores antes de aplicar la opción real de Amplio. En el sentido inverso, la confirmación aplica `OFF` mediante el controlador nativo antes de habilitar Colores. Si hay otros cambios gráficos pendientes se pide aplicarlos o descartarlos primero.

La protección de renderizado consulta tanto la configuración solicitada como el escalador efectivo: las modificaciones externas, los reinicios y los fallos de DLSS no pueden ejecutar ambos pases simultáneamente. Si la inspección del backend falla, Colores permanece en pausa. No se promete composición simultánea de Colores y FSR1/DLSS. En 26.3 Amplio no está presente y no se simulan controles de ese proveedor.

### Entrada y fuentes

Nunito queda limitada al menú principal y a Right Shift. Se retiró también la interceptación global que permanecía en 26.3; chat, inventarios, gráficos y pantallas de servidores conservan su fuente nativa. El atlas de 1.8.9 comprueba cobertura real y usa fallback cuando falta un glifo. Las texturas se invalidan al recargar recursos.

Los campos compartidos admiten selección, Ctrl+A, copiar/pegar, desplazamiento del cursor y borrado de selección. El adaptador moderno sincroniza explícitamente el foco con la entrada de texto nativa. Se conserva el manejo separado de caracteres y teclas en cada plataforma.

### Right Shift: causa medida y corrección

JFR identificó trabajo excesivo en el árbol de intersecciones de la GUI de 26.3 al insertar las tiras que formaban cada borde redondeado. Ahora se reutiliza una pequeña máscara suavizada con composición de nueve segmentos. La caché está acotada y se invalida al recargar recursos. No se eliminaron adornos, textos, controles ni animaciones.

En la comparación local del mismo escenario con Sodium y renderizado por software:

| Medida p95 | Antes | Después |
| --- | ---: | ---: |
| CPU del callback de interfaz | 72,80 ms | 1,48 ms |
| Intervalo entre callbacks | 736,64 ms | 174,71 ms |

Los JSON comparativos están en `docs/evidence/audit/right-shift-before.json` y `right-shift-after.json`. Son mediciones de este entorno, no FPS certificados en una GPU física. El mundo y el mapa siguen consumiendo recursos con llvmpipe. La sonda final también mide el mundo sin el menú para separar ese coste. JFR es optativo de las pruebas; no se inicia en el JAR de usuario. Solo se distribuyen eventos de rendimiento filtrados, sin los eventos de entorno o argumentos de inicio.

### Diorama 1.8.9

Cada bake registra los chunks que realmente consulta, incluidos los ausentes. Un cambio invalida sus dependencias concretas; las modificaciones ajenas no obligan a reconstruir todos los tiles. Las dependencias publicadas usan referencias débiles para no retener snapshots antiguos. Las pruebas incluyen cambios regionales, llegada de chunks antes ausentes y dependencias liberadas.

La máscara se reutiliza hasta que cambia el contenido subido a textura, origen, extensión, capa o LOD. Ya no se recalcula por el mero paso de 100 ms y reutiliza su búfer. La captura sigue acotada a un chunk por pasada; el bake permanece fuera del hilo de interfaz, con dos workers y colas acotadas. Se limita la subida a una textura por dibujo. La composición conserva supermuestreo 2× y ajusta su cadencia entre 60 y 30 Hz según su coste, sin reducir detalle.

Los informes separan captura, bake, máscara, subida y dibujo, además de profundidad de colas. Son tiempos CPU; no se etiquetan como tiempos GPU. La calidad de nubes y la del ray tracing permanecen independientes. 1.8.9 mantiene el port CPU de Diorama; no se afirma que tenga el mismo backend GPU de 26.3.

### Gráficos e idioma

La clasificación usa un catálogo revisado de 200 IDs exactos, sin coincidencias de palabras. Hay cinco categorías y subgrupos funcionales para pantalla, cámara, iluminación, geometría, entidades, chunks, memoria/procesamiento, escalado, shaders y diagnóstico. Las extensiones desconocidas siguen visibles en Avanzado. La búsqueda global conserva todas las opciones y el selector de subgrupos permite acotar cada categoría.

Los textos de pausa, confirmaciones y estados gráficos usan I18n. El idioma sigue Minecraft, salvo selección explícita de TFL; ya no hereda una preferencia obsoleta del launcher. Se añadieron etiquetas y ayudas funcionales propias en español para proveedores con redistribución restringida. Las traducciones de Amplio y Lucerna conservan sus avisos de licencia. Se revisaron advertencias de DLSS, memoria, conectividad y optimizaciones potencialmente incompatibles. Los nombres técnicos y valores de enum pueden conservar su denominación original.

### ESC con chat nativo

Ambas versiones abren una subclase del chat real del juego. Conserva envío, historial, comandos, completado y componentes interactivos nativos. Enter envía y mantiene la pausa abierta; Escape vuelve al juego. Ctrl+Tab alterna el foco entre chat y acciones; Tab conserva el completado nativo. La rueda sobre el panel de acciones desplaza sus botones; fuera de él conserva el historial nativo. Escribir con el chat enfocado no activa acciones del menú.

El mundo sigue visible, los botones quedan a la izquierda y la X se ancla a la pantalla completa. El historial ocupa una región inferior acotada. El scoreboard se oculta únicamente mientras está abierta esta pantalla, incluidos los datos del sidebar personalizado, sin borrar los datos del servidor.

## Validación y alcance de la entrega

Los artefactos se publican únicamente cuando aprueban las pruebas comunes y la matriz nativa: 1.8.9 baseline, Legacy LWJGL3 y Argentum; 26.3 baseline y Sodium. Las sondas se distribuyen como mod de pruebas separado y se excluyen de los JAR instalables. Los paquetes incluyen instrucciones, dependencias exactas, hashes, fuentes del commit y evidencia nativa.

Las pruebas cubren vídeo en movimiento, mapas, recarga de recursos, ESC e historial, cambios gráficos reales y persistencia, editores, cambio de resolución y fullscreen de 1.8.9. La ejecución local ampliada con Argentum, Lucerna y Amplio comprueba la exclusión de Colores y escalado, liberación de recursos y viewport en ambos sentidos. El fallback DLSS→FSR1 se verifica cuando lo proporciona llvmpipe.

**Límite pendiente de validación física:** este entorno no dispone de tu AMD A10/Radeon R7 ni de una segunda GPU física. No certifica sus FPS, tiempos GPU, funcionamiento real de DLSS o de shaderpacks ni compatibilidad universal con servidores/modpacks. Los avisos externos de controladores AMD, Lithium/ModernFix, firmas de skins y efectos ausentes requieren reproducción en su stack original; no se atribuyen sin evidencia a TFL. Iris en 26.3 permanece sin certificar en este backend. Esto no se sustituye con una compilación aprobada.

La implementación de los puntos de código de la auditoría está incluida; su aprobación final en hardware físico sigue necesitando esa comprobación. El PR permanece en borrador y no se fusiona en main.
