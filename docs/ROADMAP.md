# TFL Core — Roadmap

## Principios

- Minecraft 26.3 con Fabric y Java 25 primero; port 1.8.9 posteriormente.
- Client-side; sin paquetes de red propios, seguimiento ni cuentas obligatorias.
- Rendimiento medido, gráficos configurables y accesibilidad.
- Mejor aprovechar mods especializados de optimización que duplicar su código.
- Ninguna función se marca como terminada sin compilar, pasar pruebas y, cuando sea posible, validarse en partida.

## 0.1 — Foundation (completado)

- [x] Entrada client-only, CI/Gradle Wrapper.
- [x] Minecraft 26.3 abre mundo e inicializa el mod (prueba del usuario).
- [x] Configuración local mínima y HUD básico.

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
- [ ] Validación completa en partida.
- [ ] HUD arrastrable, widgets avanzados, escenas 3D, integraciones.

## 0.3 — PROJECT OVERDRIVE

### 0.3.0-alpha.1 — Fundación modular y UI (esta entrega)

**Estado:** código compilado y 51 pruebas JUnit en verde. **Sin validación en partida.**

- [x] Sistema de módulos con dependencias, ciclos, cascada y aislamiento de fallos.
- [x] Configuración schema v2, normalización, migración v1 → v2 y backups.
- [x] Guardado debounced/atómico, import/export seguro, modo solo lectura.
- [x] Gestor de teclas con conflictos y reinicio.
- [x] Temas (5) y acentos (6), alto contraste, transparencia reducida, movimiento reducido.
- [x] Menú principal rediseñado y botón en pausa.
- [x] Pantalla de ajustes con 6 pestañas, búsqueda, paginación y reinicio en dos pasos.
- [x] HUD de 19 widgets, un solo elemento Fabric, intervalos por widget.
- [x] Editor de HUD con arrastre, anclas, cuadrícula, escala y fondo.
- [x] Crosshair personalizado sin mixin.
- [x] Toggle sprint y zoom con restauración de opciones.
- [x] Waypoints locales con marcador de muerte y guía en HUD.
- [x] Diagnóstico con p50/p95/p99 y benchmark temporizado.
- [x] i18n inglés / español de España / español de México.
- [x] Comandos de cliente y exportación.
- [ ] **Validación en Minecraft 26.3 (bloqueante para alpha.2).**

### 0.3.0-alpha.2 — Validación y pulido (siguiente)

- [ ] Probar en partida todo el matrix de `docs/TESTING_0.3.md`.
- [ ] Corregir integración de título/pausa, editor de HUD y layout a GUI pequeña.
- [ ] Layouts de HUD independientes por perfil.
- [ ] Controles de opacidad y modo compacto por widget en el editor.
- [ ] Combo, cooldown de ataque e indicador de impacto (solo información local).
- [ ] Perfil por mundo/servidor.
- [ ] Abrir carpeta de logs y exportar informe de diagnóstico.
- [ ] Medir rendimiento real con `/tflcore bench` en escenas comparables.

### 0.3.0-beta — Exploración y rendimiento

- [ ] Resaltado de bloque, distancia entre puntos y contador de área.
- [ ] Overlay de chunk y nivel de luz del jugador.
- [ ] Matriz de compatibilidad con versiones exactas de mods.
- [ ] Detección de conflictos gráficos y fallback automático.
- [ ] Benchmarks reproducibles publicados (mismo hardware/escena).
- [ ] Revisión de accesibilidad y navegación completa por teclado.

### 0.3.0 — Release candidate

- [ ] Sin regresiones conocidas.
- [ ] Documentación y guía de usuario cerradas.
- [ ] JAR, sources, changelog y matriz de funcionalidades publicados.
- [ ] Validación en una instalación limpia de Minecraft 26.3.

### Fuera de alcance de 0.3

- Escenas 3D, skins, minimapa, reimplementación de optimizadores, herramientas de redstone/inventario, integración con Discord o manifiesto del launcher.
- Cualquier función que requiera modificar el servidor, enviar paquetes propios o automatizar combate.

## 1.0 — Objetivo a largo plazo

- Base estable y documentada para 26.3.
- Port validado a 1.8.9.
- Integración con TFL Client (instalación/actualización) sin comprometer la privacidad.
