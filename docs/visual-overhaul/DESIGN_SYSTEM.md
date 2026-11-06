# TFL Core — Sistema de diseño propuesto (UI Design System)

> **Estado: especificación para etapa post-Audit 5.** Valores de diseño *candidatos* sujetos a validación visual y técnica; ningún número aquí significa que la implementación actual lo cumpla.
> Dirección general: [VISUAL_DIRECTION.md](VISUAL_DIRECTION.md).

## 1. Objetivo técnico

Una única biblioteca de patrones y componentes de TFL, implementada en lógica/UI compartida y renderizada de forma nativa por cada plataforma:
- `vendor/trs-client/common/`: composición, tokens, estado, semántica, navegación, entradas, persistencia y pruebas de UI.
- `vendor/trs-client/ornithe/`: adaptación LWJGL/OpenGL 1.8.9 y componentes gráficos específicos cuando sea necesario.
- `vendor/trs-client/fabric/`: adaptación gráfica de Minecraft 26.3.
- `vendor/diorama/`: motor de mapas; mantener separado del rediseño de sus herramientas de interacción.

**Bases de código existentes para inspeccionar/reutilizar antes de inventar más frameworks:** `core/ui/Canvas.java`, `Theme.java`, `UiAppearance.java`, `UiScreen.java`, `Anim.java`, `core/ui/menu/{ModMenu,ModulePanel,SettingsPanel,HudEditor}.java`, `core/ui/graphics/GraphicsUi.java`, `core/ui/title/TitleUi.java`, `OrnitheCanvas.java` y `GfxCanvas.java`. Los nombres propuestos abajo son **responsabilidades conceptuales**, no clases existentes ni promesas de API.

Nunca crear dos árboles de pantallas que evolucionen por separado. Los adaptadores reciben la misma especificación de widgets/layout y aportan capacidades nativas de renderer/texto/inputs.

## 2. Identidad y tokens: primer borrador

### 2.1 Colores (HEX sRGB; por validar contra la marca y capturas)

| Rol de token | Ejemplo provisional | Uso |
| --- | --- | --- |
| `surface.canvas` | `#0C1017` | Fondo TFL sin juego visible |
| `surface.panel` | `#141B25` | Base de panel |
| `surface.elevated` | `#1C2532` | Superficies elevadas |
| `surface.hover` | `#263246` | Hover discreto |
| `border.subtle` | `#344054` | Delimitación tenue |
| `text.primary` | `#F1F5FA` | Lectura normal |
| `text.secondary` | `#B2BFCE` | Descripciones y metadatos |
| `text.muted` | `#8998AA` | Contenido terciario, nunca información crítica |
| `accent.primary` | `#4D94FF` | Azul TFL provisional (revisar identidad) |
| `accent.hover` | `#6BA7FF` | Hover/pressed según contraste |
| `accent.soft` | `#19355E` | Selecciones y superficies suaves |
| `state.success` | `#4DCAA2` | Confirmación real |
| `state.warning` | `#EAB86C` | Cautela |
| `state.danger` | `#EF7D89` | Errores o acciones peligrosas |

Estos no reemplazan la paleta TFL definitiva; son un punto de partida. El color del acento debe utilizarse con moderación. **Verificar contraste de cada combinación real (al menos 4.5:1 para texto normal y 3:1 para textos grandes/controles identificadores cuando aplique); no declarar cumplimiento sin medirlo.** No comunicar estados únicamente con color.

Para el mundo visible debajo de paneles: scrim uniforme/dinámico limitado, no convertir cualquier fondo de Minecraft en un problema de legibilidad. El vídeo del menú principal necesita su propio tratamiento de contraste, no aplicar un oscurecimiento global que arruine el material.

### 2.2 Escala, espaciado y densidad

Usar **unidades de interfaz lógicas adaptadas a GUI Scale**, no fijar coordenadas en píxeles físicos.

Tokens iniciales para prototipos:
- Espaciado: `4 / 8 / 12 / 16 / 24 / 32 / 40` unidades lógicas.
- Padding de pantalla: `16–32` según espacio disponible.
- Padding de panel: `16–24`.
- Espaciado entre secciones: `24–32`.
- Alto de fila de ajuste: objetivo `44–56` cuando el espacio permita; modo compacto `36–44` sin perder clicabilidad.
- Área interactiva: apuntar a `>= 32 × 32` unidades lógicas donde sea posible; tamaño mayor para acciones principales.
- Radios: micro `4`, control `8`, panel `12`, modal `16` (límites sugeridos; evitar cápsulas gigantes por defecto).
- Bordes: máximo `1` unidad lógica en paneles ordinarios; énfasis únicamente en focus/errores.

Se debe probar que los targets adaptan correctamente su escala. Una fila no puede quedar inaccesible porque el tamaño GUI sea grande.

**Breakpoints de comportamiento (conceptuales, no valores rígidos):**
- Ancho estrecho: una columna; sidebar colapsable; inspector contextual modal.
- Ancho intermedio: rail + contenido.
- Ancho grande: rail + contenido + inspector en pantallas que lo necesitan.
- Alto pequeño: scrolling estable; acciones críticas visibles y accesibles.

### 2.3 Tipografía

**Objetivo:** tipografía moderna y consistente en componentes propios TFL, sin interferir con texto Minecraft, chat o inventarios de terceros.

- Nunito forma parte de la identidad actual, pero **no imponerla mediante mixins globales**: se corrigieron regresiones de fuente en auditorías previas.
- Validar Nunito en títulos y superficies TFL propias; contrastar legibilidad en body/control con la fuente actual y, si corresponde, otra familia licenciada. No adoptar una nueva fuente sin comparación de capturas.
- Jerarquía de ejemplo: heading L `22–26`, heading M `17–20`, sección `14–16`, control `12–14`, ayuda `11–12` unidades lógicas. Ajustar por resolución, GUI Scale y rasterización real; **ningún texto importante microscópico**.
- Pesos coherentes: regular/medium/semibold; negrita no sustituye agrupación.
- Métricas Unicode y fallback para acentos, símbolos, cirílico donde haya soporte; cobertura verificada en ES/EN.
- No usar texto de Unicode decorativo que dificulte lectura o copy/paste.

### 2.4 Iconografía e imágenes

- Una familia de iconos coherente (trazo, peso óptico, grilla, esquinas); geometría adaptada a Minecraft.
- Icono + texto en acciones principales; icon-only solo si tiene tooltip, focus y nombre accesible.
- No mezclar emojis, iconos de mods arbitrarios y glifos pixel-art como lenguaje visual primario.
- Preferir vector rasterizado en atlas o geometría cacheada; no recrear texturas en cada frame.
- Assets, fuentes, vídeos e iconos deben registrar autor/licencia/procedencia.

## 3. Componentes base: contrato, no decoración

Cada componente debe tener:
1. **Identidad y semántica** (acción, navegación, entrada, lectura, estado).
2. **Interacciones** (hover, focus, clic, teclado, drag cuando corresponda).
3. **Estados** (normal, activo, pressed, disabled, pending, error, loading cuando corresponda).
4. **Layout** (medidas, truncado, wrapping, clipping, hitbox).
5. **Estilo** derivado de tokens (sin HEX hardcoded por pantalla).
6. **Persistencia** delegada al modelo/backend original, nunca almacenada directamente en el widget.
7. **QA**: prueba visual, de entrada y de estado.

| Componente propuesto | Contrato principal |
| --- | --- |
| ScreenFrame / OverlayFrame | Inset, back/close, fullscreen safe area, capa modal |
| NavigationRail / Breadcrumbs | Ruta y categoría seleccionada; restore foco/scroll |
| Section / PropertyGroup | Encabezado, descripción, controles y separación |
| SettingRow | Label/ayuda/control, disponibilidad, validación, dirty state |
| Primary / Secondary / Destructive Button | Acciones diferenciadas, targets y foco |
| IconButton | Nombre + tooltip, hitbox y hover/focus |
| Toggle | Estado verdadero, actualización y disabled reason |
| Slider / NumericInput | Rango, step, unidad, input teclado, valor editado |
| Select / SegmentedControl | Opciones validadas; teclado; estados restringidos |
| SearchField | Selección texto, clipboard, IME, focus y empty state |
| ModuleRow / ModuleCard | Nombre, detalle, disponibilidad, estado y navegación |
| TabBar / CategoryList | Una selección inequívoca |
| Modal / Confirm | Escape/Enter con semántica segura, cierre y bloqueo |
| Tooltip / HelpPopover | Contraste, edge avoidance y no tapar control relevante |
| Toast / InlineError | Mensaje verificable y accionable |
| EmptyState / LoadingState | No inventar contenido, no congelar pantalla |
| ScrollContainer | Clipping y scrolling sin afectar controles vecinos |
| InspectorPanel | Propiedades contextualizadas, especial para HUD |
| ColorPicker / KeybindPicker | Acciones que no alteran bindings ajenos inesperadamente |

No crear 20 clases independientes si los componentes existentes pueden extenderse de forma limpia. Priorizar reuso y contratos comprobables.

## 4. Interacción y accesibilidad

**Teclado/foco:**
- Tab / Shift+Tab en orden visual; flechas en listas y sliders según contexto; Enter para aceptar; Escape para cerrar/volver sin destruir cambios.
- Focus visible incluso sin hover; foco restaurado tras volver de un modal.
- Menú ESC conserva el chat nativo y sus atajos, sin activar acciones mientras el texto esté enfocado.
- Los controles de Minecraft y proveedores mantienen sus propios contratos de aplicar/deshacer.
- Inputs: selección, copiar/pegar, borrado, navegación, teclado internacional e IME en targets que lo soporten.

**Texto y lectura:**
- Títulos cortos, descripciones humanas y avisos no crípticos.
- Traducciones ES y EN para todas las cadenas nuevas. No mezclar idiomas.
- Validación en formatos largos y con escalado de sistema.
- Tooltips por contexto, no depender de conocer el nombre interno del mod.

**Accesibilidad:**
- Reduced motion / reduce transparency / alto contraste cuando corresponda a las preferencias existentes.
- No depender de color ni sonido para comunicar estado.
- No usar animaciones que impidan interacciones en mitad de la transición.
- Si una función requiere mouse, ofrecer alternativa por teclado donde sea razonable.

## 5. Motion design

**Presets conceptuales:** normal, reducido y desactivado, según capacidades y ajustes ya implementados.

Duraciones de partida para prototipos (no objetivos de FPS):
- Hover/focus: `80–140 ms`.
- Abrir/cerrar panel: `140–220 ms`.
- Cambios de categoría: `120–180 ms`.
- Feedback de acción: `90–150 ms`.

Easing consistente y preferiblemente sin rebote agresivo. Evitar morphing de texto e iconos que afecte a hitboxes, y nunca retrasar la ejecución funcional por decoración.

**Animar principalmente transform/opacity o valores baratos**, no reconstruir todo el árbol UI ni recargar recursos por frame. Menú con vídeo: motion de interfaz independiente de la reproducción y del fallback.

## 6. Renderizado y estabilidad por plataforma

### 6.1 Objetivos generales
- Renderer nativo por target, sin webview/WebGL requerido ni dependencia de servicios externos.
- OpenGL correcto; Vulkan no es una dirección del cliente actual.
- Caches de geometría/texto/texturas, invalidación precisa y reutilización de layouts.
- Cortar/ocultar correctamente por scissor, preservar textura, programa, VAO/VBO, framebuffer, matrices, blend, viewport, color mask, depth y el estado activo de textura cuando se alteren.
- Restaurar siempre en `finally` / scopes seguros; no asumir que una snapshot «solo consulta» si usa llamadas que modifican el contexto.
- Cada efecto que exige un pase extra debe medir su coste. No degradar silenciosamente resolución, calidad, escala o vídeo.
- No bloquear tick/render por lectura de disco, decodificación, búsqueda o carga de imágenes.

### 6.2 Ornithe Gen2 1.8.9
- Examinar `OrnitheCanvas`, `MapGlState`, target del minimapa, shader/fog y su interacción con el `Tesselator` compartido antes de cambios globales de pipeline.
- La incidencia de CI nativa Audit 5 en el blit `glDrawArrays` permanece como restricción importante; no aceptar una renovación visual que la empeore.
- Respeto a Argentum / Argentum Extras, Lucerna, Amplio, Legacy LWJGL3, OSL y demás mods aprobados, sin modificar la lista definitiva del modpack.

### 6.3 Fabric 26.3
- Adaptación de controles Vanilla/Sodium/Iris cuando estén disponibles y sean compatibles, sin clonar backends.
- Mantener el renderer y los datos de Diorama, conservar inputs nativos, comprobar fullscreen y resolución.
- Compatibilidad con el conjunto seleccionado de mods de optimización sin incorporarlos al Core.

## 7. Economía visual y presupuestos

No fijar métricas arbitrarias como certificación. Medir escenas representativas antes/después de cada bloque grande:
- Tiempo de frame UI p50/p95/p99 y coste CPU de preparación/dibujo.
- Asignaciones por frame y carga de texturas/atlas.
- Tiempo de abrir Right Shift / Settings / HUD Editor.
- Costo con vídeo, con vídeo desactivado y con reduced motion.
- GPU real en perfil alto y CPU/GPU modestos; Linux llvmpipe no equivale a GPU de Windows.

Definir presupuestos al levantar el baseline. **No aceptar regresiones importantes** sin justificación, revisión y opción de reducir efectos explícita.

## 8. Backwards compatibility

Nunca renombrar IDs de settings/módulos para hacerlos «bonitos». Separar label de identificador. Mantener:
- Archivo de configuración `config/tflcore/client.json` y semántica de guardado actual.
- Perfiles, hotkeys, HUD layouts, mapas, waypoints y caches.
- Estados de opciones de terceros y validaciones nativas.
- Import/export/backups y compatibilidad de generaciones de esquema.

Los cambios de ubicación visual o navegación **no deben modificar la semántica del dato**. Migraciones solo con tests y respaldo. No añadir dependencia remota a funciones offline.

## 9. Decisiones que requieren prototipos, no especulación

- Botones exactos del main menu.
- Densidad de lista vs cards en Mod Menu.
- Rail fijo o colapsable.
- Variante de fuente body de TFL si Nunito no ofrece la legibilidad esperada.
- Cantidad de transparencia sobre mundo y vídeo.
- Controles del HUD Editor y layout de inspector.

Cada una requiere al menos alternativas visuales con capturas en juego, selección informada y revisión en ambas resoluciones. **No convertir hipótesis de diseño en hecho consumado solo porque una IA haya dibujado una GUI convincente.**

## 10. Criterio de calidad del sistema

Se considera listo cuando el 100% de las pantallas intervenidas usan tokens y componentes coherentes o justifican una excepción; cada acción funciona; ESC, gráficos y HUD no pierden funcionalidades; el teclado es utilizable; los datos se preservan; no hay fugas GL; la experiencia final está aprobada con capturas y prueba en juego, no por screenshots de mockups únicamente.
