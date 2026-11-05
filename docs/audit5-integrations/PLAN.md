# TFL Core — Audit 5: Horizons, LAN Sharing & World Insight

> **ESTADO: propuesta técnica detallada para definir con el responsable del producto. NO IMPLEMENTADA.**
> Dirección solicitada el 2026-11-04. Propuesta de una única actualización funcional **antes** del Visual Overhaul [#14](https://github.com/TFLivesStudio/TFLClientMOD/issues/14).
> Branch de documentación `docs/audit5-integrations-plan` creada desde `feat/audit5-development` (HEAD observado `48cad899`). La rama de trabajo Audit 5 permanece intacta.
> Los targets siguen siendo **Minecraft 1.8.9 Ornithe Gen2** y **Minecraft 26.3 Fabric**, Java 25.

## 0. Contrato de producto: qué entra y qué no

**TFL Core (dentro del mod):**
1. **Distant Horizons / Far Terrain:** terreno lejano LOD controlable desde TFL. En 26.3 aprovechar DH upstream con integración real; en 1.8.9 requiere un renderer LOD propio o port de ingeniería mayor, porque no hay DH upstream para esa versión.
2. **Share World / e4mc:** abrir una sesión LAN a internet con relay, idealmente de TFLives y URL propia. 1.8.9 sobre `e4mc-retro`, 26.3 sobre `e4mc` actual.
3. **World Insight / WAILA/HWYLA-like:** overlay de bloque/entidad apuntados, funcional en ambas versiones. Reimplementar la experiencia de manera propia hasta resolver el problema de procedencia/licencias del repositorio enviado.

**TFL Selection — modpack 1.8.9, como mods independientes:**
4. **Zirconium-Ornithe 0.2.0** — optimizaciones puntuales, mantenido como paquete/archivo propio.
5. **Legacy-Starlight 1.0.0** — nuevo motor de luz; independiente de Core.

**No** integrar Zirconium ni Starlight en el JAR de TFL Core. Su inclusión es **aditiva** a la selección previamente aprobada: no sustituir Ornithe Gen2, Argentum, Argentum Extras, Legacy LWJGL3, Cera, Lucerna, Amplio, Pulse, Sarcio, SoundFix, OSL ni el Core. Ambos nuevos mods se habilitan en la selección **solo si** aprueban la matriz de compatibilidad, estabilidad y rendimiento; nunca anunciar inclusión final sobre simples declaraciones de metadata.

Esta etapa NO es la renovación visual integral. Solo se crearán pantallas/controles mínimos, coherentes con la UI existente y con la futura actualización visual; no rediseñar el resto de menús.

## 1. Fuentes verificadas y límites de compatibilidad

| Fuente | Revisión/version observada | License/advertencia | Observación |
| --- | --- | --- | --- |
| [Distant Horizons](https://gitlab.com/distant-horizons-team/distant-horizons) | 3.3.4, 26.3 Fabric | LGPL-3.0-only | Existe versión 26.3; **no** existe port oficial 1.8.9 |
| [e4mc Retro](https://github.com/xhyrom/e4mc-retro/tree/1.8.x) | 1.0.2, rama `1.8.x` | Apache-2.0 en repo + cabeceras MIT en código derivado | 1.8.9 y Ornithe explícitos, broker y relay configurables |
| [e4mc modern](https://github.com/vgskye/e4mc-minecraft-architectury) | 26.3 soportado en versiones modernas de e4mc | MIT | Examinar versión exacta antes de integrar |
| [OrnithedHwyla](https://github.com/RoyanAB/OrnithedHwyla) | 1.8.26, configurado para MC 1.12.2 | Metadata `CC0` proviene de template; **NO acredita derechos de todo el contenido** | Requiere port real y auditoría de licencias. HWYLA original indica CC BY-NC-SA 4.0 |
| [Zirconium-Ornithe](https://github.com/Coccocoahelper/Zirconium-Ornithe) | 0.2.0, MC 1.8.9 | LGPL-3.0-only; Java 21+ declarado, Core usa Java 25 | Mixins de chunksave, idioma, texturas, trigonometría, etc. |
| [Legacy-Starlight](https://github.com/Polyfrost/Legacy-Starlight) | 1.0.0, MC 1.8.9 | GPL-3.0-only | Sustituye iluminación; examinar interacción con chunks, cliente y Argentum |

**Licencias:** confirmar copyright/cabeceras de cada archivo redistribuido y dependencias transitivas antes de copiar código o empaquetar. En particular, no asumir que la metadata CC0 de una plantilla permite incluir derivados HWYLA con licencia no comercial. Exigir atribuciones y fuente correspondiente GPL/LGPL/Apache/MIT según el caso; los avisos no se eliminan por renombrar la función TFL.

## 2. Arquitectura propuesta dentro de TFL Core

Separar **servicio de capacidad** (lógica, configuración, estado y UI mínima en `vendor/trs-client/common`) de **backend por versión** (`ornithe` y `fabric`).

Contratos conceptuales — no son clases existentes ni API aprobada:
- `FarTerrainService` + `FarTerrainBackend`: habilitar, calidad, distancia LOD, estado, compatibilidad y cierre.
- `WorldShareService` + `RelayClientBackend`: abrir/cerrar, consentimiento, URL temporal, conexiones, errores y estado de relay.
- `WorldInsightService` + `TargetInfoProvider`: objeto apuntado, texto, icono/identidad, filtros de datos visibles, render overlay y toggle.
- `CompatibilityCaps`: detecta mods, renderers y conflictos reales; no altera componentes de terceros si no hay necesidad.

No es obligatorio introducir un framework genérico gigante. Reutilizar `ConfigStore`, `ModuleRegistry`, `HudManager`, `OrnitheClient`, `TrsClient` y la capa existente de inputs/UI. Preservar IDs y settings; introducir prefijos nuevos/versionados para no tocar datos anteriores.

### 2.1 Distant Horizons — 26.3

**Objetivo de aceptación:** experiencia DH integrada en el Core, **sin que el usuario tenga que buscar un segundo mod en Modrinth**, y con soporte OpenGL/Iris/Sodium cuando cada combinación sea compatible.

1. Estudiar integración legal y real de distribución: portar/encapsular DH con licencia LGPL y fuente disponible, o empaquetar un mod anidado con loader capaz de reconocerlo. No asumir que shadow/relocation de DH es seguro ni que Jar-in-Jar de Fabric funciona sin prueba.
2. Preferir API upstream para interacción TFL; no reescribir su motor o su almacenamiento. Mantener config/caché y compatibilidad de actualización de DH.
3. Detectar si el usuario instala DH independiente: nunca iniciar **dos** motores LOD; resolver duplicado con error claro o integración con la instalación existente.
4. Gestión de shaders, depth buffers y estados GL; probar Sodium/Iris y diferencias en shaderpacks. No prometer soporte universal.
5. Funcionalidad inicial: enable/disable, distancia LOD, calidad/densidad razonable, presupuesto de CPU/RAM, estado de generación, ubicación de datos y fallback. No degradar visuales silenciosamente.
6. Si la ingeniería de un único JAR no fuera viable, no sustituir a escondidas por descarga remota ni presentarla como «incluida». Registrar bloqueo y solicitar decisión de producto antes de cambiar alcance.

### 2.2 Far Terrain — 1.8.9

**Decisión técnica propuesta:** no fingir que el DH oficial puede cargarse. Crear renderer LOD cliente-solo, en la versión Ornithe, que cumpla la función «ver terreno lejano» con límites explícitos.

- Datos: SOLO chunks legítimamente recibidos por el cliente, cacheados de sesiones previas cuando la identidad del mundo esté confirmada; sin generación de chunks desconocidos desde un servidor remoto ni peticiones personalizadas.
- LOD: capturar geometría simplificada fuera de la distancia Vanilla, con niveles por distancia, colas CPU acotadas y buffers GPU reutilizados.
- Persistencia por dirección+identificador verificable de mundo+dimensión; mantener protocolo/sesiones del Audit 4. No mezclar mundos, especialmente redes proxy y múltiples Overworld.
- Compartir datos ya normalizados de chunks con Diorama cuando sea seguro, **no** reutilizar ciegamente sus tiles oblicuos como geometría 3D.
- No invadir FBOs, cachés de texturas, shader pipeline o matrices ajenas; es prioritario el problema de GL nativo 1.8.9 aún no resuelto.
- Compatibilidad con Argentum, Legacy LWJGL3, Lucerna, Amplio; shader integration solo si está verificada. Modo fallback explícito cuando un pipeline no sea compatible.
- La paridad de controles/UX con 26.3 es objetivo; **paridad de renderer/calidad/fps** no se promete.

### 2.3 World Share — 1.8.9 y 26.3

- Integración al flujo **Open to LAN/Compartir mundo**, no túneles abiertos automáticamente al iniciar el juego.
- Iniciar solo con consentimiento explícito; copiar dirección, estado, puerto, cerrar sesión y eventos de conexiones/errores.
- Relay externo opcional TFL con broker/relay propio; consultar [TFL_RELAY.md](TFL_RELAY.md).
- Sin cuentas TFL obligatorias, sin backend TRS, sin acceso remoto a servidores dedicados, sin spyware, sin endpoints ocultos.
- Ocultar la dirección al cerrar el mundo; invalidar asignaciones, liberar hilos/puertos/sockets en desconexión, fin de mundo y salida.
- Seguridad: whitelist y restricciones de jugadores cuando el host las configure; avisar que compartir un mundo local lo expone a internet. Revisar autenticación de Minecraft, IPs, logs y protección de datos. No prometer que relay garantiza anti-DDoS o encriptación end-to-end sin evidencia.
- Hosting en servidor/VPS se define y valida **aparte**; no gestionar el panel, DNS ni abrir puertos como parte de esta planificación.

### 2.4 World Insight / HYWLA-like — ambos targets

- Bloque apuntado: nombre localizado, icono/bloque o item, ID técnico opcional, estado relevante visible para el cliente, nombre del mod de origen cuando sea verificable.
- Entidad apuntada: tipo, nombre y datos de vida SOLO cuando el cliente recibe el dato; ocultar datos desconocidos, no inventar.
- Secciones opcionales y disponibles: propiedades, estado de bloque, herramienta apropiada si es calculable sin engaño, contenedores solo con información legítimamente disponible; NO leer NBT oculto de servidores ni mandar paquetes propios.
- HUD propio adaptable a lugar, escala, opacidad, tecla, escondido por F1/ESC; compatibilidad con Mini Player, minimapa, crosshair, scoreboard y perfiles.
- Evitar integrar hoy las GUIs antiguas de HWYLA; todo el overlay debe utilizar componentes TFL y ser retematizable en Visual Overhaul. En 26.3 no instalar ni depender de Jade externo para la funcionalidad básica.
- Conservar atribución cuando se reusen ideas/APIs permisivas, pero no copiar código/recursos cuyo derecho de redistribución no esté resuelto.

## 3. Reglas de no regresión

- Audit 4: ESC/chat; Diorama, mundo y cache; rotaciones 90°, carpetas/waypoints; fuente; vídeo; shaders; perfiles.
- Audit 5: registros y diagnósticos de stall `glDrawArrays`; no deshabilitar el smoke para obtener green artificial.
- OpenGL: no dejar estados cambiados entre renderers; snapshots de diagnóstico también deben restaurar sus consultas con efectos secundarios.
- Inputs: el overlay de World Insight no debe interceptar teclas/raytrace/acciones de mundo o HUD Editor.
- Rendimiento: ninguna tarea de disco, LOD, túnel de red ni inspección de bloques bloquea el hilo principal.
- Persistencia: backups, formato versionado; en rollback, nunca eliminar mapas/config ni reemplazar caches históricas.
- Sin despliegues ni releases automáticos al registrar este plan.

## 4. Bloques de desarrollo en una sola actualización

**I0 — contratos y pruebas base:** inventario real, licencias, backend boundaries, versiones fijadas, checkpoint, pruebas de regressión de Audit 4.

**I1 — World Insight:** complejidad moderada; aporta ganancia visible pronto.

**I2 — World Share:** fork e4mc-retro 1.8.x + e4mc moderno 26.3; integración de UI/consentimiento, mock relay de tests, red en ensayo local; dominio propio requiere relay funcional.

**I3 — Far Terrain moderno 26.3:** pin DH 3.3.4; resolver incorporación/distribución, shader/render lifecycle y caché; no duplicados.

**I4 — Far Terrain Ornithe 1.8.9:** renderer client-only y pruebas por mundo/LOD; trabajo de ingeniería mayor, no prometer duración.

**I5 — Compatibilidad del modpack 1.8.9:** Zirconium y Starlight *independientes*, cada uno solo y combinados con modpack actual.

**I6 — QA integral y una candidata:** pruebas y checks de [COMPATIBILITY_AND_RELEASE.md](COMPATIBILITY_AND_RELEASE.md), changelog, source/licencias, hashes, rollback.

Se permite construir en paralelo bloques independientes y commits reversibles. Si se exige la misma fecha de entrega para todo, cualquier funcionalidad no lista detiene una release que prometa esas funcionalidades. No ocultar partes incompletas tras la etiqueta «Audit 5 completo».

## 5. Qué decisiones siguen condicionadas a evidencia

- **Licencia HWYLA:** política propuesta «implementación propia», no importación del árbol RoyanAB, hasta corroborar cadena de derechos.
- **Un solo JAR real con DH 26.3:** requiere prototipo loader/build legal y técnico; API sin runtime no cuenta.
- **1.8.9 LOD:** alcance mínimo de rendimiento y fidelidad pendiente del prototipo con render 3D real y tests.
- **Dominio:** el subdominio exacto y el control DNS pertenecen al propietario; elegirlo únicamente cuando se conozca qué zona posee TFLives.
- **Infra relay:** elegir VPS/IP/puertos y autorización separada de producción.
- **Compatibilidad Zirconium/Starlight:** no establecer `required` ni preinstalar como definitivos antes de pruebas.
- **Nombre comercial definitivo y etiquetas de release:** mantener Audit 5 como paraguas de trabajo, sin afirmar que estas integraciones estén finalizadas.

**Seguimiento:** GitHub issue específica de Audit 5. Visual Overhaul posterior sigue separado como #14.
