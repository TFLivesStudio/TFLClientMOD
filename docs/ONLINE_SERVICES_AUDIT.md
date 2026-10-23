# Auditoría de servicios y capacidades

Política de esta distribución: no hay backend TFL aprobado y no se usa TRS como infraestructura
TFL. `Product.onlineServices()` devuelve false sin configuración de usuario que lo reactive.

| Capacidad upstream | Sitio de conexión/inicialización | Tratamiento TFL |
|---|---|---|
| Badges, capes remotas, presencia, friends, entitlements/emotes | `OnlineFeatures.create`, `TrsOnline`, `Http` | Instancia offline, sin login/sync; física local de capas conservada |
| Sincronización de configuración y notas | `ClientSync`, `NotesSync` factories | No se crean desde la instancia offline; notas locales conservadas |
| Cuentas Microsoft y tokens | `AccountManager.init`, `MsAuth` | Inicialización deshabilitada; no se lee cuenta del launcher ni se inicia OAuth |
| Bug reports con logs/capturas | `BugReports.init` | No se crea el servicio ni la acción remota |
| TRS Launcher IPC, grabación y clips del launcher | `TrsLink.initShared/session` | No inicia thread IPC; sesión rechazada; F2/capturas locales conservadas |
| Hosting, relays, e4mc y descargas de código | `OnlineHooks/LegacyOnline`, `Relay.connect`, `E4mcRuntime` | No instala hosting; transportes/creación de túnel rechazados |
| Event streams | `PlayerEventStream.UrlOpener` | Bloqueo previo a conectar |
| Imágenes/cosméticos remotos | `Http.UrlConnection`, `RawHttp`, `SafeFetch` | Bloqueo previo a DNS/conexión en los transportes reales |
| Resource packs de un servidor al que entra el jugador | `connect/PackDownloader` | Se conserva el flujo de aceptación y límites upstream; no es un servicio TFL |
| Ping al servidor seleccionado | `net/StatusPing` | Se conserva la función explícita del usuario |
| Browser, portapapeles, selector de archivos y GPU info | `Links`, `PlatformOpen`, `ImageClipboard`, `FileChooser`, `GpuInfo` | Capacidades locales upstream conservadas; requieren revisar sus flujos antes de RC |

Endpoints encontrados: `trs-launcher.theredstonee.de`, `api.theredstonee.de`, `trs-launcher.wiki`,
Microsoft/Xbox/Minecraft auth y texturas, `broker.e4mc.link`, Modrinth para consultas de pack.
Se conservan como referencias históricas en código inactivo o funciones que necesitan acción del
usuario; no se crean endpoints ficticios. La ayuda principal muestra créditos y rutas de documentación locales dentro del menú, sin abrir
el navegador. Algunos enlaces especializados upstream se conservan para acción explícita del usuario.

Las pruebas de transportes existentes pueden usar únicamente IP loopback literal cuando Gradle
activa `tflcore.test.loopback=true`. La excepción no acepta dominios remotos ni aliases DNS,
y no habilita por sí misma login, hosting o inicialización de servicios en el producto.
Los tests de lógica online que se conservan usan transportes falsos inyectados, no servicios reales.

Esta auditoría no certifica un sandbox de seguridad global para cualquier mod del pack. El juego,
Fabric y otros mods conservan su comportamiento; las llamadas vanilla de autenticación observadas
al intentar arrancar Minecraft no proceden de un backend TFL activado.

La revisión visual del primer smoke Forge detectó accesos remotos deshabilitados pero aún visibles
en pausa. Se reemplazaron por TFL Core, capturas locales y Server Info; el segundo recorrido
verificó la pantalla corregida. Las cuentas/wardrobe sin proveedor local usable no se ofrecen.
