# Contrato para TFL Client y primer modpack

TFL Core es un mod y no instala launchers, runtimes o versiones de Minecraft. La candidata exige:
Minecraft 26.3, Java 25, Fabric Loader 0.19.5 y Fabric API 0.161.0+26.3.
El JAR declara esos requisitos y los módulos API usados. `distribution/fabric-api.json` fija el
archivo real de Modrinth, versión, tamaño, SHA-1/SHA-512 y URL comprobados.

1. Crea una instancia nueva de Minecraft 26.3 con ese loader y Java.
2. Ejecuta `python3 tools/distribution.py check-instance <instancia>/mods`.
3. Instala un único TFL Core, más la API fijada. No añadas optimizadores en esta primera prueba.
4. Conserva el código fuente correspondiente junto al binario distribuido.
5. Completa el checklist de TESTING.md; añade después un optimizador cada vez, con reinicio y logs.
6. Solo tras certificar el conjunto fija sus hashes en un manifiesto de modpack. Una coincidencia
   de versión de Minecraft no sustituye esa prueba.

## Manifiesto local v1

`python3 tools/distribution.py package <JAR>` produce `distribution/build/tflcore-build.json`,
JAR y SHA-256. `kind=local-build`, `channel=development` y `publication=null` distinguen el
resultado de una release. `artifact.file` es una ruta relativa local, **no una URL de descarga**.
`source.commit` identifica HEAD y `workingTreeModified` revela cambios locales pendientes.
El consumidor debe rechazar stableApproved=false para actualizaciones automáticas estables.

El launcher debe verificar tamaño/hash, instalar en una carpeta temporal, comprobar metadatos y
reemplazar el JAR con Minecraft cerrado. Retirar la versión anterior antes de activar la nueva;
conservar backup y ruta de rollback. No reemplazar `config/`, mundos, mapas, notas ni `options.txt`
con defaults del pack. Ejecutar la migración mediante el mod al primer arranque.

Los canales definidos son development, beta, release-candidate y stable. Cambiar de canal requiere
un proceso de validación y revisión; los builds ordinarios no publican releases. El job de publicación dev.2 exige un push con
el marcador explícito `release: publish v0.4.0-dev.2 prerelease`, ambos builds aprobados y un
borrador aún sin publicar. Usa el token de Actions, adjunta fuentes/hashes y solo publica prerelease;
rechaza sobrescribir una release publicada.
Para una publicación futura, crear metadatos de release separados con URL real, hash, fuente
correspondiente y evidencia de certificación. No reutilizar el manifiesto de build como feed firmado.

La herramienta `mrpack` genera un paquete local de validación siguiendo el índice Modrinth v1:
TFL y su source ZIP en overrides, Fabric API con descarga y hashes oficiales, y dependencias del
juego/loader exactas. No se redistribuyen binarios de optimizadores externos.

Ejemplo después de comprometer todos los cambios:

```sh
mkdir -p distribution/build
git archive --format=zip --output=distribution/build/tflcore-corresponding-source.zip HEAD
python3 tools/distribution.py mrpack distribution/build/tflcore-fabric-26.3-0.4.0-dev.1.jar distribution/build/tflcore-corresponding-source.zip
```
