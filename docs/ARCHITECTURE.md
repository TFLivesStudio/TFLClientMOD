# Arquitectura adoptada

`vendor/trs-client/common` es la única implementación activa de módulos, estado, configuración,
HUD, matemáticas, perfiles, mapas y lógica offline. Sus fuentes se compilan dentro de cada JAR.
Se conserva Java 8 en common para compartirlo con Forge 1.8.9; no usar APIs modernas de Gson
que no existan en el Gson 2.2.4 de ese target.

`fabric/src` usa Stonecutter para aislar adaptaciones de Minecraft, entrada, render y mixins.
El objetivo principal es `:fabric:26.3`; la versión VCS 1.21.1 también se configura por requisitos
de Stonecutter, pero no se declara como candidata TFL probada. `legacy` es otro build independiente.
Los demás adaptadores upstream se conservan, sin afirmar soporte TFL.

`Product` concentra identidad y política de servicios. El mod ID público es `tflcore`;
los paquetes Java, mixins y namespace de recursos `trsclient` se conservan deliberadamente.
`TrsClient.id()` sigue resolviendo ese namespace, no el mod ID nuevo. Fabric declara conflicto
con el mod upstream `trsclient`. El detector de instancias encuentra duplicados leyendo metadatos.

`ConfigStore` guarda `config/tflcore/client.json`; `TflMigration` importa el archivo antiguo sin
sobrescribirlo. Las claves desconocidas sobreviven a las escrituras. Datos de versión futura o
importaciones bloqueadas ponen el almacén en solo lectura. Los waypoints nuevos van a
`config/tflcore/waypoints.json`; mapas, notas y varios ajustes upstream conservan rutas `trsclient`
para evitar migraciones masivas no verificadas.

`Canvas` sigue siendo el límite entre Minecraft y UI. `SelectionStyle` suministra superficies,
botones, radios y espaciado comunes a través de los métodos de compatibilidad de `Redstone`.
El título y cabecera del menú usan identidad TFL. Fabric aplica Inter mediante el proveedor TTF
nativo; Forge conserva el fallback compatible. `MenuBackground` maneja estático/off y el ciclo
de vida de un reproductor opcional. No existe backend de vídeo instalado.

Los servicios remotos no se inicializan: no hay cuenta TFL ni endpoints sustitutos. La física
local de capas mantiene sus hooks con una instancia offline. HTTP, streams, IPC y relays tienen
barreras adicionales. Las pruebas de transporte permiten únicamente IPs loopback literales mediante
una propiedad exclusiva del harness; nunca habilitan destinos externos.

Las tareas de la raíz delegan al proyecto adoptado. El árbol antiguo `src/` no es un source set.
La versión funcional anterior permanece en `main` mientras esta rama no cumpla las condiciones de merge.
