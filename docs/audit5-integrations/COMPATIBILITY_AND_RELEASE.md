# Audit 5 Integrations — Compatibilidad, pruebas y release gates

> **Propuesta, no certificación.** No se han compilado nuevos módulos ni lanzado nuevas matrices de GitHub Actions por crear estos documentos.

## 1. Dos clases de cambio, dos políticas

### Integrados en TFL Core

- Far Terrain / Distant Horizons (26.3 DH, 1.8.9 port funcional).
- Share World / e4mc Retro 1.8.9 y e4mc moderno 26.3.
- World Insight / overlay propio similar a HWYLA.

Deben estar controlados por TFL Core y aparecer bajo la configuración actual con toggles, feedback, persistencia y desmontaje. La integración no autoriza copiar clases sin cadena de derechos comprobada, ni requiere traer nuevos servicios online por defecto.

### Separados en TFL Selection 1.8.9

- `Zirconium-Ornithe`, `0.2.0`, MC 1.8.9, licencia LGPL-3.0-only; declara Java 21+.
- `Legacy-Starlight`, `1.0.0`, MC 1.8.9, licencia GPL-3.0-only.

La lista existente de mods 1.8.9 permanece sin reemplazos, tal como decidió el producto. Son **candidatos adicionales**, no incluidos definitivamente hasta las pruebas. Repositorio del catálogo de selección: `TFLivesStudio/TFLClientSelection` (no modificar ese repo en esta fase de planificación).

## 2. Riesgos específicos de Zirconium

Fuente: `Coccocoahelper/Zirconium-Ornithe`, `src/main/resources/zirconium.mixins.json`:
- `chunksaving.MinecraftServerMixin` y `ServerWorldMixin`: probar guardados, salida, retorno a mundos, rollback de archivos.
- `hidedownloadingterrainscreen.ClientPlayNetworkHandlerMixin`: riesgo para transiciones de mundo, login/reconnect y la fijación de sesiones de mapas de Audit 4. Validación especialmente importante; no desactivar un safety check del Core sin evidencia.
- `languagereloading.LanguageSelectionListWidgetMixin`: comprobar idiomas, texturas y fuentes.
- `lithium.math.sine.MathHelperMixin` y cambios de objetos: comprobar estabilidad de animaciones, cámaras y hitboxes, sin afirmar impacto de FPS a priori.
- `stbitextureloading.TextureAtlas*Mixin`: comprobar texturas, shaders, vídeo y HUD; no confundir con corrupción de OpenGL.
- El README enumera varias optimizaciones, pero en `zirconium.mixins.json` los mixins de glint/fontbatching aparecen como `commented_out_mixins`: **no dar por activos todos los beneficios anunciados**.
- `loader_version=0.19.3`, `osl_version=0.20.3` en Gradle: Core usa loader/OSL más recientes; verificar bytecode, APIs y conflictos de mixin al cargar con versiones definitivas. La metadata sugiere Argentum, pero no prueba la compatibilidad.

## 3. Riesgos específicos de Legacy Starlight

Fuente: `Polyfrost/Legacy-Starlight`:
- Reescribe propagación de luz, almacenamiento y ciclo de chunks.
- `ClientChunkCacheMixin`, `ClientWorldMixin`, `WorldChunkClientMixin`, `WorldChunkS2CPacketMixin` y mixins del servidor integrado pueden modificar el orden en que la luz está lista.
- Incluye `CeleritasGate.java` con integración reflexiva específica de rastreador de estado de chunks cuando sus clases existen; **no** inferir que por ello todos los forks/versions de Argentum funcionarán.
- Dibujado de Diorama y Far Terrain deben tolerar secciones parcialmente iluminadas y actualizar tiles/mesh al cambiar luz.
- Probar mundos nuevos y existentes, iluminación de cielo/subsuelo, bloques luminosos, día/noche, dimensiones, carga-descarga, teleport, cambio de servidor y guardar/reabrir.
- No combinar con otro motor de iluminación que modifique la misma ruta sin prueba explícita.
- `loader_version=0.19.3` frente al loader del cliente: confirmar comportamiento real.

## 4. Matriz de pruebas exigida

| Target | Escenario | Prueba principal |
| --- | --- | --- |
| 1.8.9 Ornithe | TFL Core baseline | Arranque, mundo, HUD, ESC, Diorama y OpenGL, compatibilidad con Audit 4 |
| 1.8.9 Ornithe | Core + Zirconium | Texturas, guardado, idiomas, transiciones, FPS/frametimes |
| 1.8.9 Ornithe | Core + Starlight | Iluminación, chunks, recepción, mundo local, mapa y LOD |
| 1.8.9 Ornithe | Core + ambos | Mixins, crash, iluminación, sesiones, guardados, estabilidad |
| 1.8.9 Ornithe | Core + ambos + Argentum/Extras | Renderer y luz de chunks; cero corrupción visual |
| 1.8.9 Ornithe | Core + ambos + Legacy LWJGL3 | Startup, fullscreen, render y cierre |
| 1.8.9 Ornithe | Selección completa con Lucerna/Amplio y demás mods aprobados | Shaders, frame scaling, FSR1/DLSS según hardware, HUD, LOD y GL |
| 26.3 Fabric | Core + DH integrado, vanilla | LOD, caché, normal chunks, transparencias, dimensión |
| 26.3 Fabric | Core + DH + Sodium | Profundidad, rendering, meshing, frametimes |
| 26.3 Fabric | Core + DH + Iris/shaders compatibles | LOD con shader, agua, niebla, sombras y FBO |
| 26.3 Fabric | Core + DH independiente presente | Detectar y evitar doble motor/incompatibilidad |
| 1.8.9/26.3 | World Insight | IDs/nombres, bloques/entidades, servidor, toggle, F1/ESC, internacionalización |
| 1.8.9/26.3 | World Share | Invitado vanilla externo, relay propio, cierre y conexión fallida |

### Compatibilidad de LOD, servidor y privacidad

- El LOD client-only solo muestra información del terreno que el cliente ha recibido, o terreno conocido recuperado de caché correctamente asociada al mundo; no simular exploración de nuevos chunks en servidores.
- No asumir que un cliente 1.8.9 puede compartir con invitado 26.3 sin traductor de protocolo.
- Validar que e4mc no tenga efectos mientras no se utilice Compartir mundo.
- Guardados: backups y pruebas de restauración con dos mundos A/B en misma dimensión, y mundos con datos antiguos importables.
- Desactivar/rendimiento: cambios de ajustes no deben truncar bases de LOD, cachés Diorama o archivos de mapa.

## 5. Métricas a registrar

No inventar ganancias ni resultados:
- FPS p50/p95/p99 o frametimes equivalentes en escenas reproducibles, CPU y RAM.
- Tiempo de carga de chunks y guardado; coste de light updates.
- LOD: tiempo de generación, memoria de colas, distancia renderizada, VRAM aproximada, tiempo de carga/consulta de caché.
- E4mc: latencia entre invitado/host, conexiones fallidas, tiempo de asignación de dominio, tráfico relay y recuperación.
- World Insight: tiempo de inspección en tick/render; asignaciones, visibilidad y compatibilidad con HUD.
- Baseline comparativo **sin** Zirconium/Starlight y con cada uno, para evitar atribuir ventajas sin evidencia.
- Probar al menos una GPU real potente y un equipo de recursos modestos, además de smokes CI; render software no sustituye drivers físicos.

## 6. Release gates y Definition of Done

### Core integrado
- [ ] World Insight operativo y completo en 1.8.9 y 26.3, con licencia clara y sin acceso a información oculta.
- [ ] Compartir mundo funcional con invitado real en ambas versiones, apagado limpio, consentimiento, y manejo seguro de errores.
- [ ] TFL Relay con dominio real: solo declarar completo si el DNS/broker/relay propio funciona y fue autorizado y probado; si está sin desplegar, etiquetar «relay TFL pendiente» y no afirmar URL propia disponible.
- [ ] 26.3: DH produce terreno lejano real e integrado, con archivo y fuente/licencia compatible y sin doble instancia.
- [ ] 1.8.9: TFL Far Terrain produce geometría LOD real de chunks conocidos con mundo/caché correctos; no basta un screenshot o mini mapa.
- [ ] Features desactivadas por UI no consumen recursos indebidamente, no dejan threads ni sockets.
- [ ] No pérdida de mapas, waypoints, perfiles, ESC/chat, configuraciones existentes ni fluidos de render.
- [ ] Licencias y fuentes correspondientes en distribución. Atribución no sustituye permiso.
- [ ] No introducir integración no consentida con terceros, endpoints TRS ni modpack externamente descargado.

### Mods independientes para selección
- [ ] Zirconium con versión/hash/origen y licencias fijados, carga y estabilidad probadas.
- [ ] Starlight con versión/hash/origen y licencias fijados, iluminación correcta y estable.
- [ ] Ambos coexistiendo con Core/Argentum, con las demás dependencias definitivas sin cambios.
- [ ] Prueba de rendimiento y resultado honesto: pueden entrar solo si aportan valor sin regresiones inaceptables.
- [ ] Instalación/resolución desde catálogo TFL Selection + launcher independiente del Core, con rollback y desinstalación limpios. No depender de que una página de terceros continúe sirviendo el binario durante ejecución, respetando licencias.

### Calidad, comunicación y rollback
- [ ] Sin modificaciones al branch `main` antes de release aprobada.
- [ ] Commits reversibles separados por integración; checkpoint de Audit 4 y baseline de Audit 5 conservados.
- [ ] Tests Java/Python focalizados, builds Ornithe/Fabric, smokes selectivos y prueba física.
- [ ] Si falla la certificación de 1.8.9 OpenGL, esa plataforma conserva **etiqueta experimental**; el CI no se falsifica ni salta.
- [ ] Una sola release puede incluir las integraciones, pero **no se da por terminada si falta una de las funcionalidades prometidas**. Para reducir alcance se requiere decisión explícita del responsable; un módulo silenciosamente deshabilitado no cuenta.
- [ ] JARs y hashes, fuente exacta, licencias de terceros, changelog, matriz de resultados y recuperación con copia de configs/mapas.

## 7. Plan de pruebas eficiente

- Se trabaja en tandas: implementación → tests locales relevantes → commit focalizado; no una auditoría de la auditoría.
- Usar una prueba corta de compatibilidad tras cada integración y CI nativo **solo al cerrar un bloque mayor o antes de publicar**.
- Ejecutar matrices amplias cuando el código y los mocks de integración ya estén sólidos. Evitar runs de 16 minutos repetidos sin información nueva.
- Para infraestructura relay, probar localmente con broker/relay bajo entorno de test antes de gestionar VPS en producción.

**Estado a 2026-11-04:** documento; no se ha probado ni instalado ninguno de estos nuevos componentes por esta tarea de planificación.
