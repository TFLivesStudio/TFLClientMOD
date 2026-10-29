# Cierre de auditoría — seguimiento

Base: 0.4.0-stabilize.1. Esta rama continúa la implementación; no certifica el cierre de la auditoría completa.

## Cambios de esta rama

- La máscara de niebla de 1.8.9 se actualiza cuando cambian su origen, extensión, capa, LOD o contenido efectivo de tiles. Ya no se reconstruye por el mero paso de 100 ms.
- La revisión de contenido se incrementa al subir el resultado a textura en el hilo cliente, en lugar de usar el contador de trabajos terminados en workers. Esto evita almacenar una máscara anterior con una revisión más reciente.
- Recargas, cambios de mundo y cambios de calidad invalidan la máscara como antes. La expulsión de tiles ocurre durante la subida que incrementa su revisión de contenido.
- Las etiquetas principales de ESC usan I18n en ambas plataformas. Inglés y español tienen textos propios; otras lenguas reciben provisionalmente los textos ingleses para conservar la integridad del catálogo.

## Pendientes que siguen bloqueando el cierre

- Chat interactivo integrado con historial, comandos y componentes nativos en ESC; scoreboard oculto únicamente en esa pantalla.
- Invalidación regional de tiles, presupuesto y métricas separados de captura, bake, máscara, subida y composición.
- Perfilado JFR del menú Right Shift, métricas de asignaciones y frametimes con escenarios comparables. La prueba de vídeo no certifica el rendimiento del menú de módulos.
- Grupos funcionales explícitos y subgrupos en gráficos, traducciones de textos del proveedor y mensajes incrustados restantes.
- Transición interactiva Colores/escalador y verificación de recursos y viewport en ambos sentidos.
- Verificación física en AMD A10/Radeon R7 y una segunda GPU. El entorno actual solo dispone de renderizado software; no permite afirmar esos resultados.

No se deben marcar estos requisitos como completados por haber aprobado una compilación o las sondas generales existentes.
