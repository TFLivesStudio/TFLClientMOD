# Verificación de nomenclatura

Se corrigieron los nombres visibles del cliente a TFLClient y se distinguió la
biblioteca TFL Selection del launcher TFL Client. La regla está en
[PRODUCT_NAMES.md](PRODUCT_NAMES.md) y AGENTS.md. IDs y namespaces técnicos se
conservan para no romper configuraciones.

En main se validaron los JSON modificados. Las cuatro ramas candidatas usan
TFLClient en Product y sus traducciones; la clase Product real se compiló y
ejecutó para verificar el nombre en Minecraft 1.8.9 y 26.3 sin alterar ID ni
carpeta de configuración. No se ejecutó un build completo del juego: no hay
JDK 25 local y Gradle no pudo descargar su distribución mediante Java.
No se generaron JAR ni nuevas releases binarias.
