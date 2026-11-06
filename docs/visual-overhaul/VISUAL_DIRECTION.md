# TFL Core — Dirección visual y especificación de pantallas

> **Etapa:** posterior a Audit 5. **Naturaleza:** objetivos de producto y criterios de diseño, NO implementación existente.
> Documento de referencia de la renovación visual. Leer con [README.md](README.md), [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md) e [IMPLEMENTATION_AND_ACCEPTANCE.md](IMPLEMENTATION_AND_ACCEPTANCE.md).

## 1. Problema que se pretende resolver

Feedback de producto (2026-11-04):
- El **main menu** gusta en conjunto; los **botones aún generan dudas**.
- El **ESC** gusta parcialmente y puede evolucionar sin perder su organización ni su comportamiento.
- **El resto de las interfaces propias o modificadas por TFL Core no satisface en absoluto**: hay que reemplazar su dirección de diseño, no hacer un simple retoque.
- Calidad de referencia esperada: la sensación de producto y pulido de **Lunar Client, Laby Client/LabyMod y CMPack**, con una identidad TFL propia. La referencia CMPack necesita capturas comprobables antes de deducir detalles específicos.

**Hipótesis a validar con capturas y observación:** la UI tiene demasiados tratamientos dispares entre menús, ajustes y pantallas TFL. No declarar esta hipótesis causa demostrada de cada defecto sin inspección visual en el juego.

El éxito es una interfaz cuya **composición, tipografía, estados, animaciones, control por teclado, navegación y consistencia** parezcan diseñados por un único equipo con una intención clara.

## 2. Identidad deseada

**Concepto:** TFL Premium / Quiet Confidence.

- Sobria, oscura, moderna y nítida; no un HUD de ciencia ficción, un panel gamer RGB, una página web genérica ni una imitación pixel-art.
- Superficies casi neutras, un acento azul TFL distintivo y poco ruido.
- Interacciones perceptibles, nunca estridentes; se anima el cambio de estado, no se hace bailar toda la pantalla.
- Capacidad para gran cantidad de funciones sin convertir cada superficie en un inventario de toggles.
- Accesibilidad y legibilidad primero; con herramientas de Minecraft de por medio, la funcionalidad debe mantenerse intacta.
- Priorizar refinamiento del ritmo espacial, estructura y contenido antes que blur, partículas, bloom o sombras.

**Referencias, no plantillas copiables:**
- Lunar: prioridades visuales sencillas, paneles despejados, claridad de foco.
- Laby: organización extensible de módulos/HUD/herramientas y estados ricos.
- CMPack: referencia nombrada por el responsable del producto; investigar con imágenes reales cuando arranque la etapa.
- TFL: menú actual que agrada, tono azul de marca, vídeo de agujero negro, ESC con chat integrado.

## 3. Alcance preciso

**IN:** toda GUI creada o intervenida visualmente por TFL Core, incluidas pantallas compartidas y adaptadores de ambas versiones: título, ESC, Right Shift, Mod Menu, Settings, Graphics, HUD Editor, perfiles, menú de mapas/waypoints, editor de marcadores, búsqueda/teclas, capturas/clips, confirmaciones, overlays TFL y pantallas Vanilla que el mod ya reemplaza o modifica.

**CONDICIONAL:** mundo individual y lista multijugador **solo** en la medida en que el mod modifique su presentación. Se conserva la lógica de guardados y conexiones Vanilla; fondos HD, previews y agrupación son mejoras posibles, no promesas de funcionalidad ya presente.

**OUT:** rediseño del launcher de escritorio TFL Client, del modpack TFL Selection, del backend de TFL Network, de UI de otros mods no intervenidos por TFL y de servidores. No sustituir funciones Vanilla por clones incompletos.

## 4. Jerarquía y navegación común

Una pantalla TFL de configuración debe responder instantáneamente:
1. ¿Dónde estoy? Título y navegación inequívocos.
2. ¿Qué puedo hacer? Una acción dominante, sin competencia de 6 botones iguales.
3. ¿Qué está activo? Estado persistente, disponible, no disponible, pendiente y fallo diferenciados.
4. ¿Cómo vuelvo? Acción atrás/cerrar siempre clara y uniforme.
5. ¿Qué estoy cambiando? Explicación en lenguaje del usuario, consecuencias y valores actuales.

Arquitectura sugerida para superficies amplias:
- **Navigation rail** estrecho a izquierda (categorías).
- **Topbar** con título, contextualización, búsqueda y cierre.
- **Content pane** con subgrupos y secciones, nunca 25 controles planos sin agrupación.
- **Inspector contextual** a derecha solo donde aporte valor (editor HUD, módulos complejos).
- **Footer** solo para acciones persistentes reales (Aplicar, Deshacer, Guardar).

Adaptaciones:
- Ventana estrecha / GUI Scale alto: navegación comprimida, controles en una columna, scroll independiente.
- Datos densos: filas y separators ligeros; las cards solo cuando una entidad merece identidad propia.
- Modal: una decisión, texto breve, acciones sin ambigüedad.
- Vista en partida: proteger la visibilidad del mundo y entradas del jugador según contexto; no bloquear clics innecesariamente.

### Wireframe conceptual de ajustes — no copiar medidas literalmente

~~~
┌────────────────────────────────────────────────────────────────────┐
│ TFL Client / Configuración                  Buscar ajustes     [X] │
├───────────────┬────────────────────────────────────────────────────┤
│ General       │ Interfaz                               [Tema ...] │
│ Gráficos      │ Ajusta apariencia, movimiento y contraste.       │
│ HUD           │ ─────────────────────────────────────────────────  │
│ Mundo         │ Apariencia                                         │
│ PvP           │   Tema                            [Midnight  v]   │
│ Accesibilidad │   Animaciones                      [●----------]    │
│ Avanzado      │   Reducir movimiento               [○]             │
│               │                                                    │
│               │ Tipografía                                         │
│               │   Escala de interfaz               [100%   v]      │
├───────────────┴────────────────────────────────────────────────────┤
│ Estado de cambios: guardados                  [Restablecer vista] │
└────────────────────────────────────────────────────────────────────┘
~~~

Las etiquetas anteriores son **ilustrativas**, no una orden para crear nuevas opciones que no existan.

## 5. Pantallas y decisiones de rediseño

### 5.1 Main menu — PRESERVAR COMPOSICIÓN

**Conservar:** personalidad general, jerarquía del menú y recurso de vídeo cuando el usuario lo tiene habilitado, ubicación reconocible de navegación y pie de versión limpio.

**Trabajar:** botones (proporción, tipografía, hit-area, separación, hover/pressed/focus, secundario vs principal); contraste dinámico sobre vídeo; degradado estable; responsive en 854×480, 1280×720, 1920×1080 y escalas GUI; reconexión visual al volver de partida; soporte de controles.

**No hacer:** cambiar sin pruebas a un nuevo layout completo, reintroducir textos de debug de vídeo/FPS ni sustituir MP4 por PNG estático como si fuera reproducción real.

**Aceptación:** comparación visual A/B con Audit 4 y aprobación expresa de la composición y botones por el responsable.

### 5.2 ESC — PULIDO, NO REEMPLAZO

**Conservar del Audit 3/4:** navegación izquierda, mundo visible, chat nativo abajo-derecha, X anclada, HUD oculto temporalmente y opciones/comportamientos existentes.

**Mejorar:** tipografía y alineación de acciones, target de clic, espaciamiento, contraste del velo, iconos auxiliares, feedback hover/focus, clipping del chat, regiones scroll, lógica al minimizar y volver, diseño de botones coherente.

**No hacer:** sustituir el chat nativo por un imitador que pierda comandos/historial/autocompletado; mover chat al centro; reactivar scoreboard durante ESC; cambiar preferencias guardadas al abrirlo.

### 5.3 Right Shift / Mod Menu — RECONSTRUCCIÓN PRINCIPAL

**Objetivo:** un control center coherente, espacioso, funcional y ágil para decenas de módulos.

**Estructura objetivo:**
- Navegación por categorías (HUD, PvP, Mundo, Chat, Rendimiento, etc.) reflejando el registro real.
- Búsqueda global con resultados visibles, teclado y estado vacío correcto.
- Lista o grid de módulos con icono consistente, nombre legible, explicación breve, disponibilidad y switch.
- Abrir detalle sin perder posición/categoría/búsqueda.
- Detalle con settings agrupados, tooltips útiles, dependencias y aviso de por qué una opción está bloqueada.
- Acciones de restablecer solo en scope definido, y confirmación si hay pérdida de datos.

**No hacer:** una gigantesca cuadrícula de cajas idénticas, scroll dentro de scroll arbitrario, ocultar un módulo no disponible sin explicación, reemplazar IDs de módulos.

### 5.4 Settings Hub — RECONSTRUCCIÓN PRINCIPAL

**Objetivo:** configuración similar a una aplicación cuidada, no a una página de depuración.

Categorías de **presentación** propuestas (no inventario definitivo de settings): General, Interfaz, Gráficos, HUD, Mundo, PvP, Rendimiento, Accesibilidad, Avanzado. Fusionar/ocultar categorías cuando no tengan contenido disponible.

Patrón: categoría → subgrupo → fila de opción → ayuda/contexto; agrupación por intención, no por proveedor. Mostrar valor actual, estado de guardado y rangos; diferenciar acciones inmediatas de cambios pendientes.

La búsqueda debe indexar título, etiquetas traducidas, descripción, alias y ruta de categoría; resultado al clic enfoca el control exacto.

### 5.5 Graphics UI — RECONSTRUCCIÓN SIN CAMBIAR BACKENDS

Separar conceptual y visualmente: Pantalla, Calidad, Iluminación, Geometría/Entidades, Rendimiento, Shaders/Escalado, Avanzado.

**Invariante técnico obligatorio:** cada control usa el backend real correspondiente: Vanilla, Argentum + Extras, Lucerna, Amplio en 1.8.9; Vanilla/Sodium/Iris cuando esté disponible en 26.3. No crear un segundo almacenamiento ni simular aplicar/deshacer de opciones no reversibles.

Estados distintos: activo, no disponible, no instalado, requerido reinicio, cambio pendiente, error de aplicación, fallback de DLSS→FSR. Una etiqueta estética nunca justifica falsear el backend.

La compatibilidad entre postprocesamiento Colores y escalador debe preservar el contrato y las confirmaciones existentes.

### 5.6 HUD Editor — RECONSTRUCCIÓN COMO HERRAMIENTA DE EDICIÓN

**Composición sugerida:** vista de HUD de juego al centro, biblioteca de widgets izquierda, propiedades del seleccionado derecha, barra compacta de edición.

Capacidades que no deben perderse: arrastre, snapping, anclas, escala, opacidad, visibilidad, perfiles y persistencia. Mostrar guías al mover, dimensiones y estados visuales sin tapar más del mundo de lo necesario.

Aceptar mouse/teclado; deshacer/restablecer solo cuando sus datos existan y el comportamiento sea correcto. Confirmación al salir con cambios pendientes según las reglas reales de guardado. No reconstruir cada widget desde cero para mejorar el editor.

### 5.7 Mundos y multijugador — SOLO SUPERFICIES INTERVENIDAS

Cuando TFL modifica listas Vanilla, mejorar legibilidad, selección, foco y acciones sin cambiar semántica de conexión/guardado.

Posible presentación por filas ricas con icono, título, versión, estado e información secundaria, favoritado/agrupación solo si existen datos y persistencia reales. Previews o banners solo cuando estén disponibles y sean legales/seguros, con fallback correcto.

Preservar accesibilidad y entradas Vanilla. El rediseño no debe prometer Quick Join, carpetas o features de Laby como disponibles si no existen.

### 5.8 Diorama / mapas / waypoints

**No rediseñar el motor de mapas para rediseñar su UI.** Mantener proyección, LOD, navegación, caché, identidad de mundos, rutas de rotación, marcadores y sus datos del Audit 4.

Mejorar capas visibles: toolbar, selector de mundo/dimensión, búsqueda, leyenda, widgets de información, editores de waypoints, carpetas y modales. La superposición no debe impedir arrastre, zoom, picking, brújula y controles de cámara.

No alterar OpenGL ni rutas de render fuera de alcance. La estabilidad 1.8.9 es especialmente sensible al estado compartido.

### 5.9 Capturas, clips, perfiles, diálogos y overlays

Unificar encabezados, vistas previas, campos, menús contextuales y barras de acción. Estados vacíos útiles (sin capturas, sin clips, sin datos, opción no instalada), error y carga honesta.

Evitar decoraciones innecesarias en overlays de batalla. Para cualquier HUD, el contraste debe funcionar sobre escenas de juego claras y oscuras.

### 5.10 Filtros, búsqueda y listas de opciones

- Respetar foco de teclado, IME, portapapeles, selección de texto, Ctrl+A y shortcuts de plataforma.
- Resultados y scroll reproducibles. Sin salto de foco al escribir.
- Preservar posición al volver desde detalle, si la pantalla sigue activa.
- Error con instrucción útil; no esconder información técnica cuando sí es necesaria para diagnosticar.

## 6. Criterios de identidad obligatorios

- No imitar la marca de Lunar, Laby ni CMPack, ni copiar iconos/recursos sin permiso/licencia.
- Azul TFL como acento, no color de cada superficie.
- Sin fuentes pixelizadas arcade en superficies propias TFL por defecto.
- Ninguna pantalla con jerarquía disonante respecto al resto.
- No imponer Nunito globalmente sobre chat, inventario o texto renderizado por Vanilla: las incidencias anteriores demostraron riesgo de regresión. La fuente unificada de TFL debe integrarse en componentes propios y respetar fallback.
- Ni glassmorphism omnipresente ni paneles opacos gigantes que oculten el mundo.
- No mostrar diagnóstico interno al usuario final, salvo en una sección diagnóstica explícita.
- Localización profesional: español e inglés completos para textos propios; no cadenas mixtas o truncadas.

## 7. Límites de decisión

Esta documentación fija la **dirección**, no cada pixel final. Antes de cristalizar aspectos con gusto subjetivo (botones de inicio, densidad de settings, tipos de tarjetas), crear 2–3 variantes de mockup o prototipo y escoger la mejor con capturas reales.

**No asumir que toda pantalla de Audit 4 está mal escrita:** el juicio del usuario es visual; conservar su lógica y los comportamientos comprobados mientras se cambia presentación.

**No marcar tareas como completas por haber diseñado screenshots.** Una GUI realmente nueva exige implementación, navegación, datos, acciones y aceptación visual.
