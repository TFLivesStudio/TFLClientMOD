# TFL Client

Distribución **en desarrollo, 0.4.0-dev.3**, derivada del código completo de TRS Client MOD
(GPL-3.0-only, © Ohev Tamerin). Las modificaciones TFL son de TFLivesStudio.
Builds de ambos targets y smokes nativos verificados; véase [el informe actual](docs/FINAL_REWORK.md).
No es un launcher y no necesita TRS Launcher ni TFL Client para sus funciones offline.

El código autoritativo está en `vendor/trs-client/`: `common/` compartido, `fabric/` moderno y
`legacy/` Forge. El antiguo `src/` de TFL 0.3 queda como referencia para migración y **no se compila**
en el producto adoptado. No se deben instalar ambos clientes juntos.

## Probar la candidata

Entrega local en `distribution/build/`: importa `tflcore-validation.mrpack` en un launcher
compatible con Modrinth y selecciona Java 25. Para instalación manual, usa una instancia nueva
con Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3 y un único JAR TFL.
Abre el menú de módulos con **Right Shift**. No añadas optimizadores durante la primera prueba.
Consulta [guía de uso](docs/USER_GUIDE.md) e [informe de entrega](docs/DELIVERY.md).

## Compilar

Con JDK 25:

```sh
./gradlew build
# Equivalente directo (Gradle 9.5.1 fijado por el proyecto adoptado):
cd vendor/trs-client
./gradlew --no-daemon --max-workers=2 :common:test :fabric:26.3:build -PtrsVersions=26.3
```

JAR: `vendor/trs-client/fabric/versions/26.3/build/libs/tflcore-fabric-26.3-0.4.0-dev.3.jar`.
No instalar el sources JAR. Dependencias de la candidata: Minecraft **26.3**, Java **25**,
Fabric Loader **0.19.5**, Fabric API **0.161.0+26.3**. Los optimizadores son externos y opcionales.

Forge 1.8.9 es un build independiente, con Java 8 como toolchain/bytecode y JDK 25 para Gradle:

```sh
./gradlew legacyBuild
```

## Distribución y estado

```sh
python3 -m unittest discover -s tools -p 'test_*.py'
python3 tools/distribution.py package vendor/trs-client/fabric/versions/26.3/build/libs/tflcore-fabric-26.3-0.4.0-dev.3.jar
python3 tools/distribution.py check-instance /ruta/instancia/mods
```

El empaquetador inspecciona el JAR real, exige informes de pruebas sin fallos, genera SHA-256 y
un manifiesto local; **no publica una release ni certifica una partida**. Consultar
[estado de distribución](docs/RELEASE_READINESS.md), [pruebas](docs/TESTING.md),
[integración de modpacks](docs/MODPACK_INTEGRATION.md), [migración](docs/MIGRATION.md),
[compatibilidad](docs/COMPATIBILITY.md) y [licencias](docs/LICENSES_AND_ATTRIBUTION.md).

El fondo incluido es una imagen estática original. El reproductor local Motion JPEG decodifica en worker y conserva el fallback; no se entrega
un clip cinematográfico. Consulta [vídeo local](docs/LOCAL_VIDEO.md).
