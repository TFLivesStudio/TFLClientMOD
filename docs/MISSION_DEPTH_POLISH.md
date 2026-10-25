# MISIÓN MAESTRA — TFL CLIENT
## OPERATION DEPTH & POLISH
### True Volumetric Clouds · Live Paper Doll · Video Repair · Cross-Platform Stability

**Producto:** TFL Client  
**Repositorio:** https://github.com/TFLivesStudio/TFLClientMOD  
**Base de desarrollo visual:** `feat/glass-ui-parity`  
**PR de referencia:** https://github.com/TFLivesStudio/TFLClientMOD/pull/7  
**Minecraft objetivo:** Java 1.8.9 / Ornithe Gen2 y Java 26.3 / Fabric  
**Optimizadores prioritarios:** Argentum y Sodium, respectivamente  
**Modelo de trabajo recomendado:** GPT-6.1 Sol — Medium.

# 0. OBJETIVO

TFL Client está avanzando muy bien, pero todavía hay diferencias considerables entre nuestras expectativas visuales y la implementación real.

En esta misión quiero corregir tres problemas de producto:

1. Las supuestas nubes volumétricas del mapa son actualmente una textura plana.
2. El vídeo del agujero negro, aun existiendo un reproductor, se percibe estático durante el uso.
3. Falta un widget de HUD que muestre una miniatura 3D animada del propio jugador, inspirado en el Paper Doll de Minecraft Bedrock.

Además, deseo seguir mejorando el rendimiento, la estabilidad y la calidad visual de ambos clientes.

**No quiero otro rework general que sustituya indiscriminadamente sistemas funcionales.**

Quiero soluciones concretas, técnicamente correctas y respaldadas por pruebas reales.

Trabaja sobre el estado más reciente del repositorio, verificando primero la situación de las ramas y los PR.

Preserva todo el desarrollo funcional ya conseguido.

---

# 1. CLOUDS 3D — NUBES VOLUMÉTRICAS REALES

## Problema actual

Actualmente `CloudFog.java` genera una textura estática de ruido a 256×256 píxeles.

`WorldMapUi.java` y `MinimapRenderer.java` dibujan esa textura por debajo del terreno explorado.

Aunque tiene apariencia nubosa, no constituye una implementación volumétrica.

No hay separación vertical real ni movimiento de cámara que permita observar profundidad.

**Esto no es lo solicitado por el product owner.**

## Objetivo visual

Quiero nubes tridimensionales flotando sobre las zonas inexploradas del mapa.

Las nubes deben sentirse como una capa atmosférica situada por encima del terreno.

Cuando el usuario cambie la perspectiva del World Map, deberá percibir:

- Separación en altura respecto al mapa.
- Paralaje.
- Siluetas con volumen.
- Sombras suaves.
- Variaciones de densidad.
- Iluminación coherente.
- Profundidad al inclinar la cámara.
- Movimiento atmosférico muy sutil cuando esté habilitado.

No quiero una textura PNG más grande.

No quiero simplemente dibujar varias texturas planas una sobre otra sin una representación convincente de profundidad.

Quiero un sistema visual realmente tridimensional.

## Implementación

Investiga cuál es la arquitectura más apropiada para ambos targets.

Considera un campo de densidad procedural tridimensional con renderizado volumétrico mediante ray marching acotado, o una representación geométrica tridimensional optimizada que produzca volumen y paralaje verificables.

No se requiere simular física atmosférica.

Sí se requiere una representación con dimensión vertical real y apariencia volumétrica convincente.

Crea interfaces compartidas para configuración y gestión del efecto, mientras los backends gráficos específicos se implementan por loader.

No copies código de renderizadores de terceros sin autorización.

## Cámara de World Map

Actualmente World Map tiene una presentación predominantemente bidimensional.

Para observar la altura de las nubes, incorpora una opción de perspectiva inclinable.

Requisitos:

- Vista superior tradicional.
- Vista inclinada en perspectiva.
- Transición suave.
- Rotación controlada cuando sea apropiado.
- Inclinación configurable.
- Zoom estable.
- Coordenadas correctas.
- Selección de waypoints precisa.
- Interacción de mouse coherente.

El mapa debe seguir siendo útil como herramienta.

No conviertas el World Map en un escenario 3D que dificulte encontrar coordenadas.

## Terreno desconocido

Las nubes deben cubrir regiones cuya información todavía no ha sido recibida o explorada.

Nunca generes chunks para mostrar información oculta.

No consultes información del servidor que no esté disponible para el cliente.

No reveles terreno desconocido mediante transparencias, sombras o artefactos.

Las nubes deben desaparecer o despejarse gradualmente cuando aparecen datos válidos de la región.

Verifica especialmente los bordes entre zonas conocidas y desconocidas.

El efecto debe ser visual, no un mecanismo que altere los datos guardados del mapa.

## Minimap

El World Map tendrá prioridad para la perspectiva volumétrica completa.

El minimapa deberá conservar su utilidad y rendimiento.

Puede compartir el campo de nubes, pero no necesita ejecutar el mismo renderizador volumétrico de alta calidad a escala completa.

Si se utiliza una representación simplificada, debe mantener cierta profundidad aparente y estar identificada internamente como modo simplificado.

No declares que una textura plana equivale al modo volumétrico completo.

## Rendimiento

Implementa opciones:

- Off.
- Low.
- Balanced.
- High.

Ajusta de forma razonable:

- Resolución interna.
- Número de pasos.
- Actualización temporal.
- Calidad de iluminación.
- Densidad.
- Distancia.
- Coste de composición.

No generar ruido costoso en CPU cada frame.

No volver a construir todas las texturas del mapa por mover la cámara.

Reutilizar buffers y recursos gráficos.

Comprobar compatibilidad con Argentum, Sodium y shaders cuando corresponda.

No interferir en el renderizado principal del mundo.

Si una plataforma no admite el renderer completo, documentar la limitación y utilizar un fallback explícito sin presentarlo como implementación terminada.

## Validación obligatoria

Producir evidencia real del mismo mapa en:

- Vista superior.
- Inclinación moderada.
- Inclinación elevada.

Las capturas deberán mostrar cambios reales de paralaje y profundidad.

Además, grabar una secuencia de movimiento de cámara, cuando el entorno lo permita.

**Criterio de aceptación: las nubes deben percibirse claramente por encima de la superficie del mapa.**

---

# 2. REPARAR DEFINITIVAMENTE EL VÍDEO DEL MAIN MENU

El product owner proporcionó anteriormente un MP4 como recurso visual de referencia.

Codex realizó adaptaciones para utilizarlo dentro del cliente.

Sin embargo, durante las pruebas del usuario, el resultado se percibe como una imagen estática.

Esto debe investigarse y corregirse.

## Auditoría inicial

Examinar:

- Vídeo MP4 original, si está accesible.
- Archivo MJPEG generado.
- Número real de frames.
- Duración.
- Diferencias entre frames.
- Cadencia de reproducción.
- Decodificación.
- Subida de texturas.
- Estado del reproductor.
- Gestión de errores.
- Configuración persistente.
- Fallback estático.
- Reduced Motion.
- Eventos de entrada y salida del menú.

No asumir que el problema reside en el codec.

No sustituir automáticamente el vídeo por otro diferente.

El recurso autorizado por el product owner debe ser la referencia.

## Pruebas de movimiento real

No basta con comprobar que se decodificó un fotograma.

Implementar una prueba donde:

1. Se abre el menú principal con vídeo activado.
2. Se captura el resultado visual inicial.
3. Se mantiene abierta la pantalla durante varios segundos.
4. Se capturan nuevos frames renderizados.
5. Se demuestra que existe movimiento perceptible.
6. Se comprueba que la reproducción continúa.
7. Se verifica el loop.
8. Se comprueba que el vídeo no se congela tras volver al menú.

Comprobar tanto diferencias en los frames decodificados como en los realmente presentados.

Crear instrumentación para distinguir:

- Decoder running.
- Frames decoded.
- Frames uploaded.
- Frames presented.
- Decoder failures.
- Playback mode.
- Effective FPS.
- Static fallback reason.

Evitar que las pruebas den falsos positivos por cambios en hover, mouse o componentes de la interfaz.

## Presets

Mantener:

- Quality.
- Balanced.
- Performance.
- Static.
- Off.

Quality debe reproducir correctamente el recurso original adaptado, con máxima calidad permitida.

Balanced debe equilibrar nitidez y consumo.

Performance debe reducir costes sin congelar la animación.

Static y Off deben funcionar como alternativas explícitas.

No cambiar preferencias existentes sin justificación o consentimiento.

## Optimización

Medir:

- Tiempo de decodificación.
- Consumo CPU.
- Memoria.
- Transferencias a GPU.
- Frametime.
- Cantidad de texturas.
- Liberación de recursos.

El vídeo debe continuar sin impedir que la interfaz responda inmediatamente.

No introducir dependencias nativas grandes sin justificación técnica.

Si MJPEG produce un coste excesivo, investigar alternativas viables, verificando compatibilidad de plataforma y licencias.

## Criterio de aceptación

El usuario debe abrir TFL Client y observar un agujero negro realmente animado.

El movimiento debe ser visible y continuo.

La prueba debe funcionar en un JAR instalable tanto para Ornithe como para Fabric.

---

# 3. NUEVA FUNCIÓN: TFL PAPER DOLL / MINI PLAYER

Quiero una nueva funcionalidad oficial del HUD.

Se llamará:

**Paper Doll**, con nombre descriptivo **Mini Player**.

Inspiración: el Paper Doll de Minecraft Bedrock.

## Concepto

Renderizar una miniatura tridimensional del personaje del jugador dentro del HUD.

La cámara predeterminada debe mostrar al personaje de frente.

No debe ser una imagen estática, un retrato 2D ni un skin head.

Debe ser un modelo 3D vivo.

## Funcionalidades

Mostrar:

- Skin real.
- Modelo Steve/Alex según corresponda.
- Armadura equipada.
- Objetos sostenidos.
- Sombrero y capas de skin.
- Capa del jugador cuando esté disponible y autorizada.
- Movimiento de brazos.
- Movimiento de piernas.
- Animaciones de caminar.
- Correr.
- Saltar.
- Agacharse.
- Ataques.
- Emotes locales.
- Estados adicionales compatibles con la versión de Minecraft.

Las funcionalidades inexistentes en 1.8.9 no deben simularse artificialmente.

## Cámara del widget

Permitir:

- Frontal, predeterminado.
- Lateral.
- Tres cuartos.
- Rotación manual opcional.

La cámara del widget debe ser independiente de la cámara del mundo.

No modificar F5 ni el punto de vista del jugador.

No alterar freelook, zoom ni freecam.

## Configuración

Incluir:

- Enabled.
- Visibility: Always / Movement / Off.
- Position.
- Scale.
- Opacity.
- Camera Angle.
- Background: Transparent / Glass.
- Animation Smoothing.
- Show Armor.
- Show Held Items.
- Show Cape.
- Hide with F1.
- Hide in menus.
- Optional frame-rate limit.

El widget debe estar completamente integrado con HUD Editor, presets, perfiles y persistencia.

## Diseño

Por defecto aparecerá discretamente en la esquina superior izquierda, sin ocupar demasiado espacio.

Debe poder moverse, redimensionarse y recolocarse.

No solaparse de manera innecesaria con otros widgets.

Mantener el diseño Nunito/TFL cuando haya texto o controles.

No añadir marcos grandes por defecto.

## Arquitectura

Implementar un contrato compartido para el módulo y el estado visual del jugador.

Crear adaptadores de renderizado específicos:

- Fabric 26.3.
- Ornithe Gen2 1.8.9.

Reutilizar los modelos, texturas y animaciones legítimas que proporciona el juego.

Evitar una segunda simulación de entidades.

Utilizar interpolación temporal cuando sea apropiado.

Controlar framebuffer, viewport, scissor, depth test, lighting, matrices y GL state.

Restaurar todos los estados modificados.

Evitar conflictos con:

- Argentum.
- Sodium.
- Iris, cuando corresponda.
- Cape Physics.
- Emotes.
- Animaciones PvP.
- Otros módulos HUD.

## Rendimiento

No dibujar el modelo si el módulo está desactivado, oculto o fuera de pantalla.

Evitar actualizaciones innecesarias de modelos completamente inmóviles.

Permitir limitar FPS del widget sin limitar los FPS del juego.

No cargar repetidamente skins o modelos.

Medir overhead y frametimes.

## Pruebas

Validar:

- Skin Steve.
- Skin Alex.
- Armadura.
- Objeto equipado.
- Caminar.
- Correr.
- Saltar.
- Sneak.
- Ataque.
- Emote.
- Cambio de mundo.
- Muerte.
- Respawn.
- Cambio de dimensión.
- F1.
- Cambio de GUI Scale.
- Movimiento dentro del HUD Editor.
- Persistencia tras reiniciar.

## Criterio de aceptación

El jugador debe observar una miniatura 3D frontal que refleje sus movimientos reales mientras juega en primera persona.

Debe funcionar en ambas versiones.

No se aceptará una captura estática de la skin como sustitución.

---

# 4. CORREGIR EL BOTÓN SINGLEPLAYER

He identificado en `TitleUi.java` un tratamiento especial del primer botón mediante una condición equivalente a:

`if (i == 0)`

Actualmente Singleplayer permanece resaltado en azul incluso cuando no está seleccionado.

Esta decisión visual no corresponde a la dirección del producto.

## Corrección

Todos los botones del menú principal deben compartir el mismo estado visual neutral.

Utilizar el color de acento solamente para interacciones explícitas como hover, focus y selección de teclado.

No aplicar resaltado azul permanente al primer elemento.

No eliminar la navegación accesible mediante teclado.

Comprobar que el cursor, el foco y el estado seleccionado se representen correctamente.

El objetivo es un menú minimalista, elegante y homogéneo.

---

# 5. PERFORMANCE & BUG STABILIZATION

Realizar una revisión de estabilidad de ambos targets.

Priorizar:

- Compatibilidad con Argentum.
- Compatibilidad con Sodium.
- Rendimiento del HUD.
- Renderizado de mapas.
- Rendimiento de menús.
- Alocaciones por frame.
- Texturas dinámicas.
- Liberación de recursos.
- Ciclo de vida de mundos.
- Cambio de dimensiones.
- FOV y cámara.
- Estado de teclas.
- Persistencia.
- Recargas de recursos.
- Pantallas de carga.
- Capturas.
- Emotes.
- Estados de pausa.

## Networking

Los módulos experimentales de red deben someterse a revisión especial.

`fastConnect` y `netOptimize` no deben activarse automáticamente en instalaciones nuevas de Ornithe mientras sus rutas nativas no estén certificadas contra conexiones reales.

Revisar valores por defecto y preservar configuraciones de usuarios existentes con una estrategia de migración segura.

No modificar paquetes críticos ni codecs sin pruebas suficientes.

## HUD

Corregir colisiones y layouts iniciales que no se adapten a diferentes resoluciones.

Mantener las herramientas de Arrange y Undo/Redo.

Evitar reordenar silenciosamente el HUD existente del usuario.

## Ornithe

Conservar el rendimiento del ecosistema Argentum.

No incorporar un segundo chunk renderer.

No realizar optimizaciones redundantes.

No modificar la configuración del optimizador externo sin permiso.

## Fabric 26.3

Verificar interacciones con Sodium, Lithium, Iris y optimizadores compatibles.

Separar pruebas de compatibilidad de benchmarks.

No declarar mejoras de FPS sin datos reales.

---

# 6. CALIDAD GENERAL DE LA INTERFAZ

Mantener la identidad TFL Client.

Modernizar y corregir los controles que todavía presentan problemas visuales o funcionales.

Prestar atención a:

- Options.
- Video Settings.
- ESC.
- World Selection.
- Server List.
- HUD Editor.
- Gestión de módulos.
- Diálogos.
- Controles y sliders.
- Scrollbars.
- Primer inicio.
- Pantallas de carga.

Evitar grandes reescrituras sin evidencia de necesidad.

Eliminar los tutoriales básicos innecesarios que todavía aparecen en Ornithe, incluido el aviso inicial de abrir el inventario, respetando mensajes importantes y accesibilidad.

El menú ESC debe ofrecer acciones contextuales según singleplayer y multiplayer.

No presentar Server Information como una sesión multijugador externa cuando el jugador está en un mundo local.

Mantener la fuente Nunito y el sistema visual compartido.

---

# 7. VALIDACIÓN REAL Y CI

Esta misión NO se considera completada únicamente porque el código compile.

Ejecutar pruebas para ambos targets.

## Ornithe 1.8.9

- Ornithe Gen2.
- OSL.
- Legacy LWJGL3.
- Argentum.
- Mundo local.
- HUD.
- Paper Doll.
- World Map.
- Nubes volumétricas.
- Vídeo animado.
- Cambio de perspectiva.
- Cambio de mundo.
- Renderizado de entidades.
- Recarga de recursos.

## Fabric 26.3

- Fabric y Fabric API.
- Mundo local.
- Interfaces.
- HUD.
- Paper Doll.
- World Map.
- Nubes volumétricas.
- Vídeo animado.
- Compatibilidad con Sodium en un escenario específico.
- Shaders/Iris cuando sea viable.

No considerar una prueba con renderizado por software equivalente a una medición en GPU física.

## Evidencia visual obligatoria

Guardar capturas auténticas y secuencias cuando corresponda.

Especialmente:

1. Nubes desde arriba.
2. Nubes desde una perspectiva inclinada.
3. Nubes mostrando separación en altura.
4. Menú con vídeo en distintos instantes.
5. Paper Doll frontal sin armadura.
6. Paper Doll con armadura.
7. Paper Doll caminando.
8. Paper Doll durante un emote.
9. Singleplayer sin resaltado azul permanente.
10. HUD completo sin solapamientos.

Cuando la captura esté automatizada, asegurar que refleje el renderizado real.

No aceptar mockups como sustitutos de pruebas nativas.

---

# 8. PUBLICACIÓN

Trabajar sobre una rama derivada del estado actual más reciente, preservando el PR #7 y su historial.

No fusionar automáticamente las ramas de desarrollo a `main`.

No eliminar Forge todavía.

Si ambos targets superan sus gates, publicar nuevas prereleases descargables, separadas por loader y versión.

Los entregables deben incluir:

- JAR Ornithe 1.8.9.
- JAR Fabric 26.3.
- Código fuente correspondiente.
- Dependencias exactas.
- SHA-256.
- Changelog.
- Capturas.
- Evidencia de vídeo.
- Pruebas.
- Problemas conocidos.
- Instrucciones de instalación.

Si una plataforma queda bloqueada, no publicar un binario roto ni declarar que el requisito funciona.

Publicar únicamente los artefactos realmente verificados.

---

# 9. INFORME FINAL

Al terminar, presentar:

- Resumen de cambios.
- Enlaces a commits y PR.
- Estado de las nubes volumétricas.
- Evidencia de profundidad y perspectiva.
- Estado real del vídeo animado.
- Pruebas de movimiento entre frames.
- Estado del Paper Doll.
- Capturas reales de ambos targets.
- Corrección del botón Singleplayer.
- Rendimiento y métricas disponibles.
- Bugs corregidos.
- Compatibilidad Argentum/Sodium.
- Resultados del CI.
- Estado de publicación.
- Links de descarga.
- Limitaciones pendientes.

No confundas implementación, prueba automatizada y certificación de calidad.

# 10. INSTRUCCIÓN FINAL

Quiero que TFL Client destaque no solamente por disponer de muchas funciones, sino por ejecutarlas extraordinariamente bien.

Las nubes deben tener verdadero volumen.

El vídeo debe estar realmente animado.

El nuevo Mini Player debe ser una pequeña representación 3D viva del personaje.

La interfaz debe ser coherente.

El rendimiento debe conservarse.

Los bugs deben corregirse desde la causa y no esconderse mediante parches visuales.

Quiero soluciones mantenibles, compatibles con las dos plataformas y verificables dentro de Minecraft.

Tienes libertad técnica para elegir las mejores implementaciones, pero no para reemplazar estos requisitos por aproximaciones superficiales sin informarlo.

**Audita, implementa, mide, prueba, corrige y prepara una nueva candidata verificable.**

# BEGIN — TFL CLIENT: OPERATION DEPTH & POLISH.