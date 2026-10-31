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
