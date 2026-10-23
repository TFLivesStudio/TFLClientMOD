# Uso de la candidata de desarrollo

## Opción rápida: paquete de prueba

Importa `distribution/build/tflcore-validation.mrpack` desde «Importar» en un launcher que
admita Modrinth (por ejemplo, Prism Launcher o Modrinth App). Selecciona Java 25 para esa
instancia. El paquete fija Minecraft/Fabric y descarga la API oficial; incluye el JAR TFL,
sus fuentes y licencia. Inicia primero sin otros mods. El launcher gestiona tu sesión de
Minecraft; TFL no añade otra cuenta. Es una candidata de desarrollo, no una versión estable.

## Instalación manual

Instala Fabric Loader 0.19.5 para Minecraft 26.3 y usa Java 25. Copia Fabric API
0.161.0+26.3 y el JAR TFL instalable en `mods/`. No copies `-sources.jar`. Retira cualquier
JAR del TFL 0.3 anterior o de TRS Client. Puedes usar el detector de instancias de
`tools/distribution.py check-instance` antes de arrancar.

El título TFL Selection permite abrir mundos, multijugador, opciones, TFL Core y salir.
Tab/flechas recorren los botones y Enter activa la opción; se mantiene el retorno al título vanilla.
Right Shift abre el menú de módulos. Busca módulos por nombre y revisa sus ajustes.
Configuration permite exportar snapshots, importar `config/tflcore/tfl-import.json` y restablecer
módulos/HUD con doble confirmación y backup. HUD Editor y Profiles conservan el sistema adoptado de TRS. Zoom predeterminado: V
(evita el conflicto vanilla de C); las teclas efectivas se pueden revisar en Controles.
No se importan automáticamente las teclas de la versión TFL antigua.

El núcleo conserva HUD, minimapa, mapa mundial, waypoints, zoom, sprint/sneak, ajustes de interfaz,
chat, capturas locales y herramientas de diagnóstico. Que un módulo aparezca no certifica su
compatibilidad en partida: consultar la matriz y checklist de pruebas. Respeta las reglas de cada
servidor, especialmente para freelook y personalizaciones visuales.

Los servicios TRS, friends, hosting, sincronización, cuentas y grabación mediante launcher están
inhabilitados. No necesitas iniciar sesión en TFLives. Las capturas F2 de Minecraft son locales.
El fondo tiene cinco modos expuestos en el módulo de título; los modos de vídeo usan el fallback
estático porque esta candidata no incluye decodificador. Desactivar animación impone estático.

Guarda una copia de `config/`, `options.txt` y mundos antes de actualizar. Si aparece solo lectura,
consulta logs y la guía de migración; no borres información para silenciar el aviso.
