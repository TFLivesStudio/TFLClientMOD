# TFL Client 0.4.0-dev.3 · Native Rework

Candidata de desarrollo para probar en una instancia separada. Hay **dos JAR distintos**:

- `tflcore-fabric-26.3-0.4.0-dev.3.jar`: Minecraft **26.3**, Java **25**, Fabric Loader **0.19.5** y Fabric API **0.161.0+26.3**.
- `tflcore-forge-1.8.9-0.4.0-dev.3.jar`: Minecraft **1.8.9**, Java **8**, Forge **11.15.1.2318**.

Instala solo el JAR de tu versión. Conserva copia de tus mundos y configuración.

## Cambios

- Inicio nativo simplificado: una acción principal, navegación textual y agujero negro visible. Nunito local y bordes con cobertura alfa.
- Un único menú TFL: sin enlace Classic ni interruptores de apariencia Vanilla. Las preferencias antiguas no lo desactivan. Recuperación técnica: argumento JVM `-Dtflcore.safeMode=true`.
- Nueva composición de pausa en ambos targets, conservando acciones de Minecraft; acceso local a TFL y capturas, información de servidor solo cuando está disponible.
- Editor HUD con seis presets explícitos (Minimal/PvP/Survival/Builder/Exploration/Custom), copias sin sobrescribir perfiles, historial de 40 estados, teclado preciso, ajuste entre elementos, guías y avisos de colisión.
- Navegación sin servicios TRS inactivos. Galería local sin grabación/compartir ficticios; edición, copia, carpetas y seis emotes locales conservados.
- Auditoría individual de los 79 módulos y harness nativo con resultados separados de las pruebas funcionales.

## Alcance de la candidata

Es **development**, no beta/RC/stable. Algunas pantallas conservan la composición de Minecraft con controles y superficies TFL para preservar formularios, accesibilidad, inventarios e integración con otros mods. La matriz y las capturas especifican qué se ejecutó realmente.

No hay clip cinematográfico final autorizado; se mantiene fondo estático y reproductor local Motion JPEG opcional, sin audio. No hay certificación funcional exhaustiva de los 79 módulos, servidores autenticados ni rendimiento en GPU física. El editor todavía no incorpora selección múltiple/grupos. Servicios online y sincronización permanecen desactivados.

Consulta `docs/FINAL_REWORK.md`, `docs/MODULE_CERTIFICATION.md` y `distribution/module-audit.json` en las fuentes adjuntas. Se entregan fuentes correspondientes completas, licencias, manifiestos, paquete Fabric de validación y checksums. El PR de adopción permanece sin fusionar a main.
