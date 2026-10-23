Estado actual: [Native Rework 0.4.0-dev.3](FINAL_REWORK.md). La evidencia siguiente conserva el historial de las candidatas previas.

# Entrega de desarrollo · 0.4.0-dev.1

1. **Estado general:** candidata instalable para pruebas internas. No stable ni RC; la misión completa conserva los bloqueos de [RELEASE_READINESS.md](RELEASE_READINESS.md).
2. **Rama y commits:** `adopt/trs-client-foundation`, PR [#5](https://github.com/TFLivesStudio/TFLClientMOD/pull/5). Implementación `a440e26`, corrección de fuente `e2cac1b`, separación de producción/desarrollo `41aaf4e`; el commit de entrega añade herramientas, CI y evidencia. Sin merge a main ni force push.
3. **Arquitectura:** `vendor/trs-client/common` compartido Java 8, adaptadores Fabric y Forge separados. La raíz delega al código adoptado; `src/` antiguo no se compila. Mod ID público `tflcore`, namespace interno `trsclient` conservado deliberadamente.
4. **Funciones conservadas:** registro de 79 módulos, HUD/editor, perfiles, zoom, herramientas PvP/survival, mapas/waypoints, chat, capturas y diagnóstico. La matriz distingue disponibilidad y validación; no se certifican todos por existir sus clases.
5. **Funciones fuera de servicio:** cuentas, social, hosting, sincronización y enlaces/grabación del launcher TRS. Su código se conserva bajo guardas; no existe infraestructura TFL aprobada para reemplazarlas.
6. **Visual:** componentes nativos Selection, acento azul, Inter en Fabric, menús y configuración adaptados. Hay capturas reales en `evidence/`; Forge conserva fallback tipográfico y elementos legacy pendientes de revisión.
7. **Agujero negro:** imagen estática original incluida, modos y contrato de reproductor con fallback. No hay vídeo/decoder distribuido; falta medio con derechos y medición de reproducción real.
8. **Tests:** common 1104 detectados, 1102 aprobados, 2 opt-in omitidos; Forge 50 aprobados; herramientas Python 6 aprobadas. Sin fallos. Las omisiones son servicio público E4mc y benchmark de red, no funcionalidad local desactivada para ocultar errores.
9. **Builds:** ambos targets adaptados compilan. Fabric Java 25/major 69 y Forge Java 8/major 52. Reconstrucción forzada Fabric con SHA-256 idéntico en esta máquina; restauración en máquina nueva no certificada.
10. **Fabric 26.3:** smokes nativos de desarrollo para menús/mapas y JAR de producción en instancia separada, con export/import/reset comprobados y cierre normal. Backend cloud Vulkan/Lavapipe software; falta GPU real y multiplayer autenticado.
11. **Forge 1.8.9:** build, tests y recorrido nativo de desarrollo aprobados. JAR de producción no probado fuera del entorno de desarrollo; es el target secundario.
12. **JAR:** `distribution/build/tflcore-fabric-26.3-0.4.0-dev.1.jar`; Forge separado en el mismo directorio. Cada archivo tiene SHA-256. No instalar los sources/dev JAR.
13. **Dependencias Fabric exactas de prueba:** Minecraft 26.3, Java 25, Fabric Loader 0.19.5 y Fabric API 0.161.0+26.3. El JAR exige loader >=0.19.5 y Java >=25; se recomienda repetir las versiones verificadas.
14. **Primer modpack:** importar `distribution/build/tflcore-validation.mrpack` en un launcher que admita Modrinth, elegir Java 25 e iniciar sin mods adicionales. Alternativa manual en [USER_GUIDE.md](USER_GUIDE.md). El pack incluye TFL/fuentes/licencia y descarga la API oficial verificada por hashes; no configura cuenta ni Java por sí mismo.
15. **Compatibilidad:** no instalar TRS ni TFL 0.3 junto a esta candidata. Sodium/Lithium/FerriteCore/ImmediatelyFast/Iris/Entity Culling tienen versiones consultadas para 26.3, pero el conjunto no está certificado ni se incluye. Agregarlos uno a uno tras validar el pack mínimo.
16. **Licencias y fuentes:** GPL-3.0-only, avisos originales y de modificaciones, Chat Heads MPL-2.0 e Inter OFL conservados. Entregar `tflcore-corresponding-source.zip` junto al binario; el pack también lo incluye. No se cambia la visibilidad del repositorio.
17. **Pendientes:** certificación exhaustiva HUD/editor/módulos/lifecycle, servidores reales, configuraciones históricas reales, offsets/keybinds/presets sin conversión segura, compatibilidad de optimizadores, rendimiento/GPU/audio y vídeo real. Detalle en TESTING, MIGRATION y RELEASE_READINESS.
18. **Recomendación:** comenzar pruebas internas con una instancia nueva y copia de seguridad de datos. Conservar rama/PR de migración; no publicar beta/RC/stable ni fusionar a main hasta cerrar los bloqueos y revisar el CI remoto.

Los archivos de `distribution/build/` son salidas locales ignoradas por Git; el workflow genera artefactos descargables en GitHub Actions. El manifiesto `tflcore-build.json` registra el commit de fuentes, dependencias y hash del JAR; no es un feed de actualizaciones publicado.
