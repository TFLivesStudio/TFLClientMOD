# TFL Core — Roadmap oficial

## Principios
- Minecraft 26.3 con Fabric y Java 25 primero; port 1.8.9 posteriormente.
- Client-side; sin paquetes de red propios, seguimiento o cuentas obligatorias.
- Rendimiento medido, gráficos configurables y accesibilidad.
- Mejor aprovechar mods especializados de optimización que duplicar su código.

## 0.1 — Foundation (aprobado)
- [x] Entrada client-only, CI/Gradle Wrapper.
- [x] Minecraft 26.3 abre mundo e inicializa el mod (test del usuario).

## 0.2 — First interactive alpha (código + CI; falta prueba en Minecraft)
- [x] Menú principal propio TFL Selection.
- [x] Acceso a mundos, multijugador, opciones y configuración del Core.
- [x] Temas BLUE / MIDNIGHT / FROST.
- [x] Fondo animado 2D de bajo coste; opción para reducir movimiento.
- [x] Right Shift, tecla editable desde opciones de Minecraft.
- [x] Configuración JSON local con guardado atómico y validación.
- [x] HUD FPS/coordenadas/reloj con interruptores individuales.
- [x] Perfiles BALANCED/FPS/QUALITY que afectan **solo** a TFL Core.
- [x] Pruebas automáticas de configuración.
- [ ] Pruebas manuales A01–A19: **pendientes**.
- [ ] Escenas 3D **todavía no** incluidas.

## 0.3 — Polishing
- [ ] Renderizado visual profesional, temas consistentes y traducción completa.
- [ ] Editor HUD con drag, resize, guías y perfiles.
- [ ] Registro de módulos aislados con configuración independiente.
- [ ] Pruebas de accesibilidad, escalado GUI y diferentes GPU.

## 0.4 — Gameplay multimodo
- [ ] HUD PvP, CPS, teclas, crosshair y efectos.
- [ ] Exploración, waypoints, brújula, armadura, durabilidad.
- [ ] Herramientas técnicas de construcción y redstone.

## 0.5 — Performance
- [ ] Paquete curado y probado de Sodium/Lithium/etc desde TFL Client.
- [ ] Benchmark con FPS, frametime p95/p99, RAM y carga.
- [ ] Escenas 3D reales opcionales con límite de GPU y respaldo estático.
- [ ] No afirmar ganancias de rendimiento sin mediciones.

## 0.6+ — Online opcional
- [ ] Discord Rich Presence, interoperabilidad segura con el launcher.
- [ ] Exportar/importar y eventualmente sincronizar perfiles con consentimiento.

## Port clásico 1.8.9
Se evaluará tras estabilidad de 26.3, con base distinta y paridad funcional razonable.
