# TFLClient · TFL Core

[![TFL Client — sitio web oficial](https://launcher.tflives.com/images/social/tfl-client-es.png)](https://launcher.tflives.com)

**TFLClient**, cliente modificado de Minecraft de **TFLives**, distribuido mediante **TFL Client** e impulsado por **TFL Core**.

## Nombres del ecosistema

**TFL Client** es el launcher. **TFL Selection** es la biblioteca donde TFLives
publica sus clientes modificados. **TFLClient** es el cliente que se instala y se
utiliza dentro de Minecraft; **TFL Core** impulsa sus funciones.

Consulta [los nombres y términos](docs/PRODUCT_NAMES.md) para mantener esta
distinción en todo el proyecto.

## Web oficial · [launcher.tflives.com](https://launcher.tflives.com)

Conoce TFLClient y la biblioteca TFL Selection en la web oficial. Allí encontrarás la presentación del cliente, la guía de instalación y las novedades del ecosistema TFL Client; este repositorio conserva el código fuente y la documentación técnica de TFL Core.

**[Conocer TFLClient en TFL Selection](https://launcher.tflives.com/selection)** · [Guía de instalación](https://launcher.tflives.com/ayuda/selection) · [Descargar el launcher](https://launcher.tflives.com/descargas)

[Novedades](https://launcher.tflives.com/novedades) · [Ayuda](https://launcher.tflives.com/ayuda) · [Discord](https://discord.gg/tflives) · [Website in English](https://launcher.tflives.com/en)

> La web presenta el ecosistema y sus distintas versiones. El estado, los requisitos y las limitaciones descritos a continuación corresponden a la rama `main` de este repositorio; otras versiones candidatas pueden diferir.

---

[![Fabric 26.3 Build](https://github.com/TFLivesStudio/TFLClientMOD/actions/workflows/build.yml/badge.svg)](https://github.com/TFLivesStudio/TFLClientMOD/actions/workflows/build.yml)

## Estado

**0.3.0-alpha.1 — PROJECT OVERDRIVE (alpha experimental)**

Objetivo: **Minecraft Java 26.3 / Fabric / Java 25**, estrictamente **client-side**. El port a **1.8.9** se trabajará después de validar la versión moderna.

> **Validación:** esta entrega **compila** (`clean build`), genera el JAR y pasa **51 pruebas JUnit**. **Todavía no se ha ejecutado una partida real de Minecraft con esta versión**; consulta la [matriz de funcionalidades](docs/FEATURE_MATRIX_0.3.md) para ver qué está implementado, qué es parcial y qué falta.

## Incluido en 0.3.0-alpha.1

### Arquitectura y configuración
- Sistema de módulos con id estable, categoría, dependencias, orden de activación, detección de ciclos, desactivación en cascada y aislamiento de fallos.
- Ajustes **schema v2** con secciones tipadas (`ui`, `hud`, `pvp`, `survival`, `perf`, `modules`, `legacy`).
- Migración v1 → v2 con copia de seguridad, conservación de claves desconocidas y protección contra archivos de esquema futuro (solo lectura).
- Guardado **debounced** y atómico; nunca se escribe en disco por frame. Exportación/importación limitada al directorio `config`, con límite de 1 MiB y validación de esquema.
- Sin telemetría, conexiones propias, cuentas, tokens ni descarga de código. `telemetryConsent` se fuerza a `false` al normalizar.

### Interfaz
- Menú principal TFLClient con tarjetas de modo, selector de perfil, estado del Core y fondo animado de bajo coste.
- Temas **BLUE, MIDNIGHT, FROST, EMBER, MONO** y acentos **DEFAULT, CYAN, VIOLET, AMBER, LIME, ROSE**.
- Modo de alto contraste, transparencia reducida, movimiento reducido y duración de transición configurable.
- Pantalla de ajustes con seis pestañas (General, Apariencia, HUD, Juego, Teclas, Avanzado), búsqueda y paginación de widgets, reinicio en dos pasos, importación/exportación y diagnóstico.
- Editor de HUD: arrastrar, imán a cuadrícula, anclas, escala, fondo, reinicio de posición y previsualización de widgets desactivados.
- Botón de ajustes del Core en el menú de pausa y acceso rápido con **Right Shift**.
- Textos en **inglés, español de España y español de México** (168 claves por archivo).

### HUD (19 widgets)
FPS, frametime (media + p95), coordenadas (con conversión Nether), dirección, bioma, reloj local, hora del mundo, CPS izquierdo/derecho, keystrokes (WASD, salto, agacharse, sprint), ping, estado de movimiento, velocidad, armadura con durabilidad, efectos de pociones, objeto en mano con cantidad/duración, bloque apuntado, chunk/dimensión, brújula de waypoint y resumen de rendimiento.

Cada widget tiene intervalo de refresco propio, ancla, desplazamiento, escala, fondo y opacidad persistidos. Todo el HUD se dibuja en **un único elemento Fabric**, con modo streamer y opción de ocultarse mientras hay una pantalla abierta.

### Gameplay
- **Crosshair personalizado** (5 estilos, color, escala, ocultar en tercera persona) mediante el registro público de HUD de Fabric, sin mixin.
- **Toggle sprint** que refleja una pulsación normal de la tecla de sprint; no envía paquetes ni automatiza el juego.
- **Zoom** con FOV configurable, suavizado opcional y restauración exacta del valor original.
- **Waypoints** locales por mundo/servidor y dimensión, con marcador de muerte (máximo 32), waypoints manuales (máximo 256) y guía de brújula/distancia en el HUD.
- Perfiles **PVP, SURVIVAL, BUILDER, EXPLORATION, MINIMAL** y detección automática; solo afectan a módulos y widgets de TFL Core.

### Rendimiento y diagnóstico
- Muestreo de frames con media, mínimo, máximo, **p50/p95/p99** y memoria; el benchmark es una captura temporizada explícita, no una promesa de FPS.
- Detección de mods cargados y matriz de compatibilidad informativa.
- Comandos de cliente `/tflcore diag|hud|profile|waypoint|bench|export`.
- Fallos de módulos aislados y visibles en diagnóstico; el cliente sigue funcionando.

## Requisitos

- Minecraft Java **26.3**
- Fabric Loader **0.19.5** o superior
- **Fabric API** para 26.3
- **Java 25**

## Instalación

1. Prepara una instalación de Minecraft 26.3 con Fabric Loader y Fabric API.
2. Copia el JAR de distribución (`tflcore-0.3.0-alpha.1.jar`) en `mods/`.
3. Inicia el juego con Java 25 y comprueba que TFL Core aparece en la lista de mods.
4. Pulsa **Right Shift** para abrir los ajustes del Core.

No instales el `-sources.jar` ni el `-dev.jar` como mod.

## Compilación

Windows:

```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
.\gradlew.bat clean build
```

Linux/macOS:

```bash
./gradlew clean build
```

Los artefactos quedan en `build/libs/`. GitHub Actions publica el JAR normal en cada compilación satisfactoria.

## Documentación

- [Guía de usuario 0.3](docs/USER_GUIDE_0.3.md)
- [Arquitectura](docs/ARCHITECTURE.md)
- [Matriz de funcionalidades 0.3](docs/FEATURE_MATRIX_0.3.md)
- [Pruebas 0.3](docs/TESTING_0.3.md)
- [Roadmap](docs/ROADMAP.md)
- [Misión 0.3](docs/MISSION_0.3_OVERDRIVE.md)
- [Changelog](CHANGELOG.md)

## Limitaciones conocidas de esta alpha

- **Sin validación en partida**: no se ha confirmado en un cliente real el reemplazo del título, el editor de HUD, el zoom, la restauración del sprint, los waypoints ni el layout a escalas GUI pequeñas.
- No hay escenas 3D, skins, minimapa, combos, indicador de cooldown de ataque, resaltado de bloques, contador de área, herramientas de inventario ni integración con Discord/launcher.
- El HUD no tiene todavía layouts independientes por perfil; los perfiles cambian el conjunto de widgets, no su posición.
- La opacidad de fondo por widget se guarda y renderiza, pero solo se edita a mano en el JSON; el modo compacto solo lo interpreta el widget de coordenadas. El editor expone escala y fondo.
- En alturas GUI muy pequeñas (< 300 px) algunas pestañas de ajustes pueden requerir una resolución mayor; el HUD y el menú tienen modo compacto.
- El CPS se muestrea a partir del estado de los botones al ritmo de frames, por lo que puede subestimar clics en equipos con FPS muy bajos.
- No se publican comparativas de FPS porque todavía no existen mediciones reproducibles en partida.

## Privacidad

TFL Core no incluye telemetría, analítica, publicidad, cuentas, sockets, peticiones HTTP propias, tokens ni ejecución de código remoto. La configuración y los waypoints se guardan únicamente en el directorio local de configuración de Minecraft.
