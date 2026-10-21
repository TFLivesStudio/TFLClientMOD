# Arquitectura — TFL Core (0.2 alpha)

TFL Client es el instalador y actualizador. TFL Core es un mod **estrictamente client-side**. Optimización gráfica compleja pertenece a los mods especializados, distribuidos por el launcher.

## Componentes implementados
```text
TflCoreClient
 ├── SettingsStore → config/tflcore.json (atómico, schema v1)
 ├── KeyMappingHelper → Right Shift
 ├── ClientTickEvents → reemplazo opcional de TitleScreen
 ├── TflMainMenuScreen → singleplayer/multiplayer/options/settings/quit
 ├── TflSettingsScreen → General/HUD/Perfiles
 ├── TflTheme → BLUE/MIDNIGHT/FROST
 └── TflHud → HudElementRegistry/FPS/coordenadas/reloj
```

Usamos `GuiGraphicsExtractor`; NO hacemos llamadas a OpenGL directo, pensando en compatibilidad con backends modernos.

## Límites de seguridad
- Sin telemetría, peticiones HTTP, sockets, cuentas, tokens, descarga de código, ni modificaciones al servidor.
- Las configuraciones se guardan localmente. Una configuración de schema futuro se conserva en modo de solo lectura para evitar downgrade destructivo.
- Los presets modifican únicamente animaciones y widgets de TFL Core, **no** parámetros de Sodium ni del driver GPU.
- Solo se renderizan datos disponibles legítimamente en el cliente.

## Pendiente
- Sistema de módulos extensible con inicialización desacoplada.
- Editor drag/drop de HUD, configuración por mundo/servidor.
- Escenas 3D opcionales limitadas por presupuesto de GPU.
- Integración opcional con TFL Client y Discord.
- Port separado para Minecraft 1.8.9.

## Barreras de calidad
1. Gradle build, JUnit y workflow aprobados.
2. Verificación humana de menús y atajos dentro del juego.
3. Pruebas de compatibilidad con Sodium/otros mods.
4. Medición comparativa antes de anunciar mejoras de FPS.
