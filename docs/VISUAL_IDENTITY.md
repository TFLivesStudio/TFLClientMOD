# TFL Client 0.4.0-identity.1 — candidata visual y gráficos unificados

Candidata de pruebas para **Minecraft 1.8.9, Ornithe Gen2, Java 25** y **Minecraft 26.3, Fabric, Java 25**. Continúa `0.4.0-unified.1`. No modifica ni fusiona `main`. No se incluyen binarios de optimizadores externos.

## Cambios visibles

- Menú principal sin contador de vídeo, área de clic de diagnóstico ni versión interna bajo el título. Pie exacto `TFL Client 1.8.9` / `TFL Client 26.3`.
- Corrección de la costura del degradado de 1.8.9. Su causa era el descarte de alfa nativo: el umbral 0,1 cortaba la cola del degradado en la columna 828 de una ventana de 1920 píxeles. El canvas desactiva alpha test durante su propio dibujo y restaura el estado anterior. También se limita la textura a sus bordes. No se oculta el fallo con otro panel o desenfoque.
- Paleta azul marino compartida y tipografía Nunito. Las listas nativas de 1.8.9 que dibujan su propio fondo reciben una textura lisa en lugar de tierra. La fuente empaqueta únicamente glifos disponibles, ampliando Latin Extended y cirílico; escrituras no soportadas conservan la fuente nativa.
- Gráficos organizados por finalidad: Pantalla, Calidad, Rendimiento, Shaders y escalado, Avanzado. Búsqueda entre categorías, alias, descripciones, sliders, estados deshabilitados y navegación por teclado. Tab cambia de zona; flechas navegan o cambian valores; Enter activa; Escape respeta cambios pendientes.
- La misma presentación abre los ajustes originales de Minecraft, Argentum o Sodium. Los objetos del backend conservan validación, disponibilidad, guardado y recarga. No hay un segundo archivo de configuración gráfica.
- Selector real de shaderpacks de Lucerna y sus controles conservados. Amplio informa por separado la elección solicitada y el escalador activo, incluido fallback DLSS → FSR1. No se anuncian Frame Generation ni FSR3.
- Título real de ventana e iconos oficiales; no solamente una textura dentro del juego.

El vídeo, su resolución, sus tres perfiles y sus bytes no cambian. Se mantienen decodificación asíncrona, bucle, fallback, preferencias y métricas internas. No se fuerzan fotogramas interpolados ni se cambia la tasa del recurso. Los contadores quedan en diagnósticos, fuera de la navegación pública.

## Auditoría y fuentes de estado

| Superficie | Implementación / tratamiento |
|---|---|
| Inicio, menú de módulos, ajustes, HUD editor | `TitleUi`, `ModMenu`, `SettingsPanel`, `HudEditor`; componentes comunes y Nunito. Se preservan IDs y valores de Setting. |
| ESC, capturas y clips | Navegación y editores comunes de Unified; se conserva el mundo visible tras ESC y las exportaciones no destructivas. |
| Mapas | Diorama y controles compartidos ya integrados; se conservan cachés, capas y waypoints. |
| Idiomas, multijugador y opciones nativas | Se conserva lógica/listas del juego. Mixins de screen/widget/font y textura de fondo unifican su presentación; no se reemplazan conexiones ni selección de idioma. |
| Gráficos 1.8.9 sin Argentum | `VanillaGraphics`: opciones originales de `VideoOptionsScreen`, setters y normalización de `GameOptions`; cambios inmediatos y guardado nativo. |
| Argentum y Extras | `OrnitheGraphics`: páginas originales de `CeleritasVideoOptionsController`, ID único por opción, `getValue/setValue/isAvailable/hasChanged`; aplicar llama `applyChanges()`, deshacer `undoChanges()`. Se conservan flags y callbacks de almacenamiento/renderer. |
| Lucerna 0.1.1 | `ShaderPackScreen` real, controles nativos y toggle/reload original. No se inventa un formato de shaderpack. |
| Amplio 0.1.1 | Opciones nativas, `CONFIG.upscaler` solicitado y campo `active` efectivo. DLSS rechazado se identifica y no se presenta como activo. |
| Gráficos 26.3 sin Sodium | `OptionInstance`, validadores, formatter y callbacks reales; se inspeccionan controles dinámicos de pantalla. Sliders diferidos aplican al soltar cuando lo exige Minecraft. Se conserva `removed/onClose` nativo. |
| Sodium 0.9.2 | `ConfigManager.CONFIG`, páginas/grupos/opciones originales; `modifyValue/getValidatedValue/isEnabled`, `applyAllOptions`, `resetAllOptionsFromBindings`; botones externos conservan su consumer original. |
| Ajustes visuales TFL | `TflGraphicsOptions` enlaza Settings existentes: vídeo, límites de FPS, Dynamic FPS, nubes, Mini Player, Fullbright y color cuando es soportado. Usa el mismo `ConfigStore.saveLater`, `config/tflcore/client.json`. |
| Identidades técnicas | Se conservan paquetes `trsclient`, dependencias, versiones en metadata, logs, manifiestos y avisos legales. No se exponen como títulos de producto. |

Aplicar/deshacer se habilita para valores pendientes del backend. Minecraft y TFL mantienen sus cambios inmediatos; la descripción lo indica y no se promete deshacerlos. **Deshacer restaura cambios pendientes, no valores de fábrica.** No se ofrece un reset global a valores de fábrica porque los proveedores no comparten un contrato seguro para ello. Los errores de aplicación permanecen visibles y no cierran la pantalla como si se hubiera guardado.

## Logo y ventana

Fuente original proporcionada por el usuario: https://panel.tflives.com/attachments/6ae379b12464c_TFLivesLOGOGO.png

SHA-256: `72db052e5daea5113e33abd38e31f16cadfe5a75a3c5418ebc501d59e4a68941`.

Se conserva el archivo original completo y su fondo negro en `assets/trsclient/branding/logo-original.png`. `BrandIcons` genera 16, 24, 32, 48, 64, 128 y 256 píxeles mediante escalado bicúbico con proporciones conservadas, sin redibujar ni recortar los rombos. Se conserva el negro para evitar halos al extraer transparencia. Las variantes de 16 y 32 píxeles siguen siendo una reducción del mismo símbolo; el detalle de bordes inevitablemente disminuye.

1.8.9 usa `Display.setTitle/setIcon`, también al iniciar y después de fullscreen. Legacy LWJGL3 1.4.10 traduce esto a SDL3. Su implementación copia todos los iconos en un único MemoryStack de 64 KiB: se suministran 64/48/32/24/16, por debajo de ese límite; los tamaños mayores siguen disponibles como recursos. 26.3 usa la ventana SDL real, `SDL_SetWindowTitle` y `SDL_SetWindowIcon`, liberando la superficie y el búfer temporal.

Las pruebas X11 verifican título, icono y cambios de fullscreen. **No se ha probado Windows físicamente.** El mod puede establecer el icono de la ventana SDL; no modifica el ejecutable Java, accesos directos fijados, iconos del launcher ni su AppUserModelID. Windows puede agrupar la barra de tareas por esas identidades externas. No se afirma cambiar el icono del proceso.

## Validación y límites

La evidencia nativa y el inventario de opciones están en [evidence/visual-identity](evidence/visual-identity). La CI ejecuta pruebas comunes/Python y clientes instalables con sondas separadas: 1.8.9 baseline, Legacy LWJGL3 y Argentum; 26.3 baseline y Sodium. Los JAR distribuibles excluyen sondas y clases de pruebas.

En local también se ejecuta la combinación disponible de OSL 0.22.2, Legacy LWJGL3 1.4.10, Argentum 1.0.2, Argentum Extras 1.0.2, Cera 1.0.2, Lucerna 0.1.1, Amplio 0.1.1 y SoundFix 1.1.0. Procedencias y hashes: [optional-dependencies.json](evidence/visual-identity/optional-dependencies.json). **Sarcio 1.1.1 y Pulse 1.0.0 no se verificaron: no se obtuvo su artefacto exacto.**

Se comprueban valores y IDs únicos de gráficos, aplicar/restaurar una opción real, apertura del selector de shaders, toggle de Lucerna y fallback real de Amplio en el backend disponible. En 1.8.9 se ejercitan 1920×1080, 1280×720, 854×480 y fullscreen. También se mantienen pruebas de vídeo, mapa, recarga y editores de Unified.

El entorno usa renderizado por software; sus cifras de FPS no certifican rendimiento en una GPU física. DLSS efectivo necesita hardware/controlador compatible y no está certificado aquí. Se valida su rechazo y fallback FSR1 real. El selector y toggle de Lucerna se prueban sin un shaderpack instalado: no se certifica el renderizado de todos los packs. Iris tampoco está certificado en este backend. No se modifica silenciosamente la configuración externa de Amplio para elevar su preset.

## Instalación

Descarga el JAR de la release correspondiente; instala **un solo JAR TFL** en una instancia de pruebas de la plataforma correcta. 1.8.9 necesita Ornithe Gen2/Fabric Loader 0.19.5, OSL 0.22.2 y Java 25; 26.3 necesita Fabric Loader 0.19.5, Fabric API 0.161.0+26.3 y Java 25. Los optimizadores son opcionales y separados. Conserva una copia de tus ajustes. No mezcles los JAR de ambas versiones ni uses Forge.

La release incluye hashes, evidencia y fuente correspondiente al commit publicado. Se mantienen GPL-3.0-only, atribución TRS y licencia MIT de Diorama.

### Resultados locales de esta candidata

- 1.8.9 con la pila opcional disponible: **154 comprobaciones aprobadas**, 131 entradas gráficas inventariadas, selector y toggle de Lucerna y fallback Amplio comprobados.
- 26.3 sin Sodium: **93 comprobaciones aprobadas**, 42 entradas gráficas inventariadas.
- 26.3 con Sodium: **109 comprobaciones aprobadas**, 50 entradas gráficas inventariadas.
- Python: 6 pruebas aprobadas. Las pruebas comunes y cada combinación mínima adicional se publican también en los artefactos de CI.

Las entradas inventariadas incluyen información de solo lectura y controles TFL; no equivalen a un número de efectos o módulos certificados. Las mediciones simultáneas locales del vídeo fueron 27,32 / 29,96 / 23,20 FPS respectivamente; el recurso sigue siendo 30 FPS nominales. No se presenta un objetivo de FPS como si fuera una medición real.
