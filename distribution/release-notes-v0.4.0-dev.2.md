TFL Client **0.4.0-dev.2** es una versión preliminar para pruebas internas, derivada de TRS Client MOD (GPL-3.0-only). Conserva el ID `tflcore`; no requiere TRS Launcher ni cuenta TFL.

### Elige un único target

| Archivo | Minecraft | Loader | Java | Dependencia adicional |
| --- | --- | --- | --- | --- |
| `tflcore-fabric-26.3-0.4.0-dev.2.jar` | 26.3 | Fabric 0.19.5 | 25 | Fabric API 0.161.0+26.3 |
| `tflcore-forge-1.8.9-0.4.0-dev.2.jar` | 1.8.9 | Forge 11.15.1.2318 | 8 | Ninguna API Fabric |

Crea una instancia nueva y coloca el JAR del target en `mods/`. No instales ambos JAR en la misma instancia, ni TRS Client ni el TFL anterior. Para Fabric también puedes importar `tflcore-validation.mrpack` en un launcher compatible con Modrinth y seleccionar Java 25. **Right Shift** abre los módulos; **G** abre la rueda de emotes locales. Guarda copia de tus mundos/configuración antes de actualizar.

### Cambios

- Nombre público TFL Client y tipografía Nunito local. Fabric usa proveedores nativos; Forge incluye un atlas suavizado para sus menús propios.
- Navegación principal a la izquierda, controles con geometría más fina y filtrado del fondo estático.
- Sliders Fabric adaptados y reorganización del menú de pausa conservando los callbacks del juego.
- Recarga de recursos con identidad TFL; tutorial básico y presentación inicial de accesibilidad omitidos en Fabric, sin eliminar narrador ni sus ajustes.
- Seis emotes locales GPL con rueda, animación y cámara; no requieren cuentas, permisos remotos ni servicios TRS. Solo son visibles en tu cliente.
- Reproductor local Motion JPEG de 24 fps: lectura/decodificación en worker, un único frame pendiente, upload en render y cierre al salir del título. No reproduce MP4/H.264 ni incluye audio. Falta un clip cinematográfico final autorizado; el fondo predeterminado sigue siendo estático.

### Límites conocidos

Es una candidata **development**, no RC/stable. La certificación exhaustiva de los 79 módulos, servidores autenticados, GPU real, resoluciones extremas, audio y combinaciones de optimizadores sigue pendiente. Los cambios del launcher externo se entregan mediante un PR separado; esta release contiene solo el mod.

Las fuentes correspondientes completas y las licencias están adjuntas. Los archivos `.sha256` permiten verificar cada descarga. Consulta `docs/FINAL_EXPERIENCE.md`, `docs/TESTING.md` y `docs/RELEASE_READINESS.md` en las fuentes para distinguir pruebas nuevas de las de dev.1.
