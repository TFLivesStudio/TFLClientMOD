# Compatibilidad comprobada y límites

La información de `distribution/optimization-availability.json` procede de Modrinth el 2026-10-23:

| Proyecto | Versión disponible para Fabric 26.3 | Estado conjunto con TFL |
|---|---|---|
| Sodium | mc26.3-0.9.3-beta.1-fabric | No probado; el propio archivo es beta |
| Lithium | mc26.3-0.26.2-fabric | No probado |
| FerriteCore | 9.0.0-fabric | No probado |
| ImmediatelyFast | 1.17.1+26.3-fabric | No probado |
| Iris | 1.11.7+26.3-fabric | No probado; revisar su dependencia de Sodium |
| Entity Culling | 1.11.3 | No probado; evitar culling duplicado |

El JSON conserva dependencias reales declaradas, IDs de versión, hashes y licencias. Esto prueba
**disponibilidad**, no resultados de render, rendimiento o compatibilidad. No están embebidos por
defecto. El opt-in `-PtflBundleAuditedMods` existe para auditores, no para distribuir sin completar
las obligaciones de cada dependencia y su código fuente.

Upstream conserva `PerfCompat` y detectores de mods para ceder funciones a optimizadores. Deben
verificarse con las versiones fijadas; no se garantizan FPS. Revisar shaders/Iris, render de mapas,
capas y culling antes de recomendar un conjunto. Instalar un solo minimapa/HUD cuando existan
solapamientos o deshabilitar explícitamente el módulo de TFL que coincida.

Zoom V y mapa M pueden coincidir con mods añadidos. Upstream incluye comprobación de conflicto
de la tecla del mapa; revisar manualmente el resto. La suite cubre la lógica de conflictos, no
un pack real con todos los keybinds registrados.

Forge 1.8.9 usa Forge 11.15.1.2318-1.8.9, MCP stable 22 y Java 8 para compilar/ejecutar el juego.
Gradle/Stonecutter requieren JDK 21+ y se ejecutan aquí con Java 25. No instalar su JAR en Fabric.
Los demás targets upstream permanecen como fuentes y no reciben certificación TFL en esta misión.
