# TFL Client 0.4.0-stabilize.1 — candidata de estabilización

Candidata de pruebas para Ornithe Gen2 1.8.9 y Fabric 26.3, basada en `0.4.0-identity.1`. Incluye cambios iniciales derivados de la auditoría; no representa el cierre de toda la auditoría.

- La fuente Nunito se restringe al menú principal y a Right Shift. Las demás superficies TFL utilizan la fuente nativa; las pantallas externas de 1.8.9 ya no quedan interceptadas globalmente por el mixin de texto.
- Los campos de texto compartidos admiten selección completa y reemplazo, y los adaptadores reenvían el portapapeles a los campos activos. En 26.3 se sincroniza el foco SDL al cambiarlo mediante ratón o teclado.
- En 1.8.9, Colores se pausa y libera su cadena de postprocesado mientras Amplio solicita un escalador; la preferencia guardada no se cambia. Se reanuda cuando el escalador vuelve a OFF. La interfaz de gráficos indica la pausa. Esta protección conservadora no es todavía el diálogo de transición bidireccional solicitado.
- Cambiar la calidad de nubes del mapa ya no cambia la calidad de trazado de tiles ni descarta la caché de tiles. La calidad visual de trazado conserva su configuración habitual.

Instala **Java 25** y un solo JAR de la versión correspondiente. 1.8.9 requiere Ornithe Gen2, Fabric Loader 0.19.5 y OSL 0.22.2. 26.3 requiere Fabric Loader 0.19.5 y Fabric API 0.161.0+26.3. Los optimizadores son instalaciones independientes. Conserva una copia de tus ajustes.

Pendiente de la auditoría: perfilado JFR y mediciones p95/p99 en el AMD A10/Radeon R7 y otra GPU; invalidación de mapa por región y diagnóstico de cargas; chat interactivo y ocultación del scoreboard en ESC; organización gráfica por subgrupos, traducciones completas y transición interactiva Colores/escalador. La protección de Colores es una medida preventiva; la corrección de viewport y la ejecución DLSS física no están certificadas. La navegación y el texto deben probarse con distribuciones de teclado y servidores reales. El backend de vídeo y las licencias se conservan de la candidata anterior.

La validación y las capturas se publican como artefactos de CI junto a los JAR. Las pruebas con renderizado software no predicen FPS en GPU física. Código, procedencia y licencias en `complete-source.zip`; detalles de la base en `docs/VISUAL_IDENTITY.md`.
