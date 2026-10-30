# TFL Client 0.4.0-audit.4 — servidores, cámara y marcadores de Diorama

Esta candidata se centra en el cierre durante cambios de mundo y en el mapa. Conserva el ESC de audit.3.

## Transiciones y separación de mundos

En 1.8.9 una sección recibida puede no tener todavía buffers de bloques o luz. El port comprueba los buffers antes de leerlos: aplaza una sección sin bloques válidos y aplica valores de luz definidos cuando el almacenamiento es nulo. Tampoco acepta chunks nulos ni mezcla resultados asíncronos de sesiones distintas.

Una dirección y una dimensión no bastan para identificar un mundo. Ambas versiones mantienen un catálogo de submundos. Los identificadores que proporciona el protocolo de Xaero/MapModCompanion permiten recuperar el mundo automáticamente; 1.8.9 registra los canales y responde a su handshake. No envía comandos de chat ni consulta servicios externos.

**Servidores sin identificador de mundo:** al cambiar de mundo se abre un espacio nuevo e independiente. En **M → Worlds / Mundos** selecciona el mapa guardado al que estás regresando y puedes renombrarlo. No se fusionan mundos basándose solo en que sean Overworld, en su semilla o en que su terreno se parezca. Si el servidor no comunica un identificador, no se puede distinguir con certeza dos copias idénticas sin esa elección. Esto también afecta a redes con lobbies, instancias o mapas que reciclan la misma dimensión.

Los datos anteriores se conservan. Cuando corresponde, el selector ofrece **Previous maps (unassigned)**: esa caché podría contener mundos ya mezclados por versiones anteriores, por lo que su adopción es explícita. Los waypoints anteriores también se pueden importar al mundo elegido.

## Caché y cambios de terreno

1.8.9 conserva snapshots y tiles por mundo, dimensión, recursos y orientación. Mantiene un conjunto limitado de mundos en memoria y guarda los datos en disco. Al recibir otra vez un chunk idéntico conserva sus dependencias y evita hornearlo de nuevo. Las modificaciones invalidan los tiles que realmente consultaron esos chunks; los bloques y la luz forman parte de la comparación. Las tareas de disco y de horneado pertenecen a su sesión original.

El servidor seguirá enviando sus chunks según el protocolo normal. La caché evita repetir el trabajo del mapa y permite mostrar terreno conocido mientras llegan actualizaciones. El motor moderno conserva sus tiles nativos de Diorama, separados también por mundo y orientación.

## Cámara y marcadores

- **Rotate +90 / Girar +90** alterna 0°, 90°, 180° y 270°. Cambia desde qué lado se trazan los bloques; no gira una imagen plana. Minimapa y mapa comparten la orientación guardada del mundo.
- Arrastre, zoom, flecha, brújula, entidades y selección de coordenadas usan la transformación correspondiente. Cada orientación tiene su propia caché.
- 1.8.9 usa los generadores originales de Diorama para la flecha voxel y los pines. Las caras usan las texturas recibidas por Minecraft, con fallback nativo; los NPC de tipo jugador no dependen exclusivamente de seguir presentes en TAB.
- Waypoints de 1.8.9: UUID, iniciales, iconos originales de Diorama, colores, carpetas y visibilidad; su archivo por mundo usa el formato de Diorama. Se conserva la migración desde los datos anteriores. Un editor abierto queda vinculado al mundo de origen incluso durante una transición. En 26.3 continúa el editor nativo de Diorama.

## Validación e instalación

Las pruebas incluyen buffers nulos, diez identificadores de mundo en un mismo servidor, selección explícita de mundos no identificados, separación A/B en la misma dimensión, restauración de caché, las cuatro orientaciones y sus coordenadas inversas, formato de waypoints y regresiones de fuentes/ESC.

Java 25. 1.8.9: Ornithe Gen2 y OSL 0.22.2. 26.3: Fabric y Fabric API 0.161.0+26.3. Sustituye el JAR anterior sin borrar configuración, mapas ni waypoints. Los optimizadores se instalan por separado.

La validación nativa se realiza en Linux con renderizado por software. No se ha realizado una sesión AFK autenticada en Hypixel ni se certifica el controlador de Windows. Las evidencias y hashes publicados corresponden al commit indicado en `build.json`.
