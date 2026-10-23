# Migración y recuperación

El producto adoptado escribe `config/tflcore/client.json` y `config/tflcore/waypoints.json`.
No interpreta `tflcore.json` como una configuración TRS aunque ambos tengan una versión numérica 2.

Si no existe el destino, detecta primero `config/tflcore.json` (TFL schema v2), después
`config/trsclient.json` (TRS configVersion 1/2). Valida tamaño (1 MiB), estructura y versión;
crea una copia `.pre-adoption-<UUID>.bak`; escribe el destino nuevo al final. Un destino existente
nunca se sobrescribe durante esta importación. El archivo original se mantiene intacto.

Se convierten estados de módulos con equivalencia conocida y widgets compatibles: activación,
ancla, escala, fondo y opacidad. Los waypoints `server:<dirección>` se convierten a `mp:<dirección>`;
`sp:` conserva su mundo. Las coordenadas fraccionales se llevan al bloque inferior, también para
valores negativos. Marcadores de identidad de mundo ambigua no se asignan a otro mundo.

**Límites pendientes:** los offsets antiguos eran píxeles y los nuevos son proporciones de pantalla;
no se inventa la resolución original. Hay que confirmar la posición con el editor HUD. Presets,
keybinds de `options.txt` y campos sin equivalencia demostrada no se convierten automáticamente.
El JSON v2 íntegro queda en `tflLegacyV2`, además de las copias originales; esto conserva información,
pero no equivale a reproducir su comportamiento. Se debe verificar la migración con configuraciones
reales antes de anunciar compatibilidad total de upgrade.

Config futura, datos de origen corruptos o un conflicto con waypoints existentes bloquean la
importación y dejan defaults **en solo lectura**. Consultar la advertencia en logs; conservar ambos
archivos y resolver manualmente el conflicto. No borrar el archivo futuro para forzar un downgrade.
La recuperación de corrupción en el archivo adoptado hace copias sin reemplazar backups anteriores.

`ConfigStore.exportTo/importFrom` ofrece transferencias con nombres JSON simples dentro de su
carpeta, rechazo de symlinks/traversal, backups y restauración del estado en memoria si falla la
importación. Estas operaciones tienen pruebas. El menú Configuration permite exportar snapshots, importar
un `tfl-import.json` de esa carpeta y restablecer módulos/HUD con confirmación de dos pasos.
El reset guarda un backup y no elimina notas, mapas ni datos desconocidos.

Rollback: cerrar Minecraft, archivar toda `config/`, mapas/notas y `options.txt`, retirar el JAR
adoptado e instalar únicamente el JAR anterior. El archivo original TFL no fue modificado.
No mezclar JARs antiguos/nuevos ni abrir mundos nuevos con una versión anterior de Minecraft.
