# Far Terrain / Lucerna API v1 — candidata 0.4.0-audit.5-rc.2

Adaptador de **Minecraft 1.8.9 Ornithe Gen2** para el contrato de [Lucerna](https://github.com/jonathanthxd/lucerna/blob/work/tfl-far-terrain-shader-stability/docs/TFL_FAR_TERRAIN.md), revisión `c9a4b8e2a5bafb6846e9bdea3ffe97538b291b39`, JAR `lucerna-0.1.5-tfl.1.jar`, SHA-256 `dffa9657cddce76c18bff9221ebe88c0ce79c7b19d36ad25e66b9dc9b38182c1`.

## Instalar para probar

1. Cierra Minecraft, duplica la instancia y conserva el JAR anterior fuera de `mods`.
2. Sustituye TFL por `tflcore-1.8.9-0.4.0-audit.5-rc.2.jar`. Usa Java 25, Ornithe Gen2/OSL y las dependencias habituales de Lucerna; deja una sola copia de cada mod. Lucerna se instala por separado.
3. Usa el Lucerna **0.1.5-tfl.1 remapeado** de esa rama y añade a los argumentos de **Java**, antes de iniciar el juego:

   ```text
   -Dlucerna.experimentalFarTerrain=true
   ```

4. Activa Far Terrain y un shaderpack que proporcione pares completos `dh_terrain` y `dh_water` para la dimensión actual. Ante un fallo de programa, recarga el shaderpack para reintentar. `latest.log` y la exportación de diagnósticos de TFL incluyen el diagnóstico del puente.

La propiedad pertenece a Lucerna y requiere reiniciar al cambiarla. Sin pipeline o con el interruptor apagado se mantiene el renderer original, conforme al contrato; esa ruta no integra la iluminación propia del shaderpack. Con el puente activo y programas incompletos, TFL omite los LOD y registra el motivo. Ante `FAILED`, no dibuja un fallback encima ni reintenta en ese pipeline hasta su recarga.

## Cambios

- Geometría generada por los workers en triángulos de 13 floats / 52 bytes: posición, RGBA sin sombreado precalculado, normal de cara, luz bloque/cielo centrada en texel y mini-ID DH. Los estados Minecraft se clasifican explícitamente, no se pasan como IDs DH.
- VBOs de opacos y agua separados. Terreno enviado antes de la primera geometría vanilla; agua enviada al terminar `Lucerna.beginTranslucents`, después de deferred y antes del agua nativa. Se reutiliza el mismo `FarTerrainFrame`, con matrices/frustum coherentes y distancia en bloques.
- Lucerna conserva la propiedad de programas, FBOs, uniforms y depth. La ruta API no usa el shader interno de TFL ni borra el depth vanilla. Los VBOs permanecen bajo propiedad de TFL.
- Agua ordenada back-to-front por regiones y por triángulos dentro de cada VBO. La reordenación al mover la cámara se ejecuta en un worker acotado; el cliente sube un resultado por vez al VBO persistente, sin convertir toda la geometría cada fotograma. La mezcla entre regiones y los bordes/TAA requieren tu comprobación visual.
- Se elimina el agujero interior cuadrado y se conserva solapamiento con la geometría nativa. El renderer interno usa distancia radial 3D y descarte conservador de regiones; no presume que el borde vanilla coincida con un cuadrado de chunks. No puede rellenar terreno que nunca se recibió ni existe en la caché del mundo.
- Al cambiar de mundo, dimensión o formato, se descartan frames/VBOs y resultados de ordenación antiguos. Se mantienen límites, identidades y **formato de caché existente**. La caché de superficies no añade terreno submarino desconocido: el puente no inventa un fondo donde esos datos no existen.
- La API se compila con `modCompileOnly` y no se copia/sombrea en TFL. Se conserva compatibilidad del campo de título del menú Lucerna (`irisText` antiguo / `lucernaText` actual).

26.3 conserva su integración independiente con DH/Iris; esta adaptación específica de Lucerna no sustituye ese motor.

## Compilar y alcance de comprobación

```sh
python3 tools/ornithe/fetch_lucerna_api.py
# Alternativa sin descargar el artifact: --jar /ruta/lucerna-0.1.5-tfl.1.jar
vendor/trs-client/ornithe/gradlew -p vendor/trs-client/ornithe :remapJar --no-daemon --max-workers=2
```

El preparador verifica el hash del JAR oficial del workflow Lucerna `42134523929`; sólo se usa durante compilación. También se puede pasar `-PlucernaApiJar=...`. Se comprobaron compilación, estructura del JAR y tres casos CPU focalizados: layout/material/luz/normales, ordenación de agua y conservación de la geometría legacy. **No se ejecutó una matriz gráfica ni se certifica compatibilidad universal con shaderpacks.** La prueba dentro del juego queda a cargo del usuario, según lo solicitado.

Para rollback: cierra el juego, restaura el JAR anterior y quita la propiedad experimental si también restauras Lucerna. Conserva mundos, preferencias, waypoints y cachés.
