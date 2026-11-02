# Evidencia del bloqueo nativo 1.8.9 (runs #42 y #46)

Estado al cierre de la Misión 04. Resumen breve; no sustituye los logs.

## Hechos medidos

- Runs `41148081840` (#42) y `41176799826` (#46): las **tres** stacks nativas de 1.8.9
  (baseline, lwjgl3, argentum) fallan con el mismo patrón: la sonda llega a la fase 4
  y el bucle del juego **no vuelve a hacer tick**; las de 26.3 (baseline y sodium)
  pasan con la misma Mesa/llvmpipe del runner.
- Run #46 (`4a9ec01`): pulso cada 30 s con contadores instrumentados.
  Último estado repetido durante más de 12 min:
  `phase=4 last=phase3:world-map-opened ticks=324 baked=9 disk=0 uploads=8`.
  El worker de Diorama tenía 9 tiles horneados y 8 subidos **antes** de la
  congelación: el bake y el upload de tiles no están bloqueados.
- Thread dumps (`jcmd`, capturados a los ~120 s y de nuevo al deadline, ~10 min
  después, ambos idénticos):

  ```
  "Client thread" RUNNABLE  (cpu 17.0s → 32.5s en 10 min)
    GL11.nglDrawArrays (Native Method)          ← no regresa
    GL11.glDrawArrays
    BufferUploader.end(BufferBuilder)
    Tesselator.end()
    RenderTarget.draw(I,I,Z):244                 ← blit del framebuffer a pantalla
    RenderTarget.draw(I):200
    Minecraft.runGame(Minecraft.java:937)
    Minecraft.run(Minecraft.java:325)
  ```

## Lectura técnica

- El punto congelado es el **blit vanilla** del framebuffer principal al ventana
  (quad fijo de 4 vértices de `RenderTarget.draw`); no es código TFL ni Diorama.
  El hilo cliente no hace ticks: el juego está muerto para lógica aunque el
  proceso siga vivo (RUNNABLE dentro del draw nativo, CPU casi plana).
- El blit vanilla corre en **todas** las fases anteriores sin problema (título,
  vídeo, mundo, HUD y captura de pantalla pasan). Lo único que introduce la
  trama congelada es la primera renderización a pantalla completa del
  `OrnitheDioramaUi` tras `openWorldMap`.
- Un rasterizado infinito sobre un quad de 4 vértices solo sugiere estado fijo
  corrupto (p. ej. programa GL activo distinto de 0, coordenadas fetch
  monstruosas) o un bug de Mesa disparado por ese estado. Todo `glUseProgram`
  del port Ornithe (`MapGlState`, `DioramaFog`, `DioramaMinimapTarget`) restaura
  su programa en `finally`, así que el elemento exacto filtrado está **sin
  demostrar**.

## Lo que no está demostrado

- Qué llamada filtró qué estado. No hay forma viable de leer el estado GL del
  contexto desde el pulso (estado no compartible entre hilos).

## Siguiente diagnóstico propuesto (requiere nueva autorización)

1. Anotar el estado GL (programa activo, bindings de textura, atributos
   habilitados, fog/stencil, matrices en uso) justo antes del blit mediante un
   mixin HEAD en `RenderTarget.draw(I,I,Z)` con muestreo **off por defecto**
   (solo activo con la propiedad de la sonda). Es código shippable: un mixin si
   entra en el JAR productivo; decidirlo con el sponsor.
2. Alternativa sin tocar el JAR productivo: ejecutar 1.8.9 con la meta de
   capturar el renderizado comprobando con estados de textura/tooling
   (`Mesa GALLIUM_HUD`/`MESA_DEBUG`) en la matriz CI antes del freeze.
3. Regla decidida: no corregir código productivo hasta que el dump de estado
   nombre el elemento concreto; mientras tanto, las ofertas de seguimiento de
   1.8.9 deben declarar "experimental" como ya está documentado.

## Run 41390130672 (Misión 06): variante `nofog` — avanza

- Commit `b3a5e4d`, dispatch único `native_target=1.8.9 native_stack=baseline
  native_variant=nofog publish=false preview_189=false`. Job
  `native (1.8.9, baseline, ubuntu-22.04)` → **success** (04:45:19–04:49:28Z);
  `publish` y `preview_189` correctamente omitidos; artefacto de 87 MB subido
  (la cuota se recuperó).
- Cadena de la variante verificada de punta a punta en el log:
  `phase3:map-fog-disabled` → `phase3:world-map-opened` (26695 ms) →
  **`phase 4` (26704 ms) atravesado** — el punto exacto donde los runs #44/#45
  se congelaban — → `phase 80` con `native Diorama map owns screen` y
  `Verified baseline smoke: 34 checks`.
- Precisión del experimento: `CloudQuality.OFF` **salta `fog.draw(...)`**
  (`OrnitheDiorama.java:612`) y, por el cambio de calidad, cierra y destruye el
  shader (`:245-250`). Es decir: desactiva el **pase de dibujo** de niebla, con
  la consecuencia de que `DioramaFog` no se reconstruye en esa ejecución.
- Snapshots `hud-tail` (eco incondicional al log, fases 41 y 99, idénticos):
  `activeTexture=33984` (GL_TEXTURE0, restaurado vía `try/finally`),
  `program=0`, `vao=0`, buffers 0, `unit0.binding=32` habilitado,
  `unit1.binding=9` deshabilitado, `fog=0`, `blend=770,771,1,0`,
  `depth=1/515/1`, `alphaTest=1`, `scissor=0`, arrays cliente 0,
  `stacks=2/1/1`. Este es el **baseline sano** de una trama posterior al mapa.
- Conclusión: **el pase de niebla es necesario para reproducir el bloqueo**
  (caso B). Sigue sin demostrarse **qué campo concreto** deja corrupto
  `DioramaFog.draw` respecto de ese baseline: ningún run estándar con
  snapshots ha capturado la trama congelada (los runs #44/#45 fueron
  anteriores a `GlSnapshot`), ni existe muestreo dentro de `fog.draw`.
  Además, un snapshot en `GameGuiMixin` TAIL no es garantía del estado exacto
  previo a `glDrawArrays`: entre ambos pueden ejecutarse operaciones vanilla.

## Siguiente corrección productiva recomendada (requiere nueva autorización)

1. Ejecutar la variante `standard` con la instrumentación ya incorporada (snapshots
   `hud-tail` activos) para capturar la trama congelada y **diferenciarla** del
   baseline sano anterior; o bien añadir captura dev-gated a la salida de
   `DioramaFog.draw` y diff fog-on vs fog-off en la misma corrida.
2. Solo con el campo nombrado, aplicar la restauración mínima en el pase de
   niebla (p. ej. reafirmar las precondiciones del blit: programa 0, unidad
   activa GL_TEXTURE0, VAO/buffers 0, arrays cliente) con tests y sin tocar la
   calidad visual por defecto.
