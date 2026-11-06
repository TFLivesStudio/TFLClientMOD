# TFL Core — Visual Overhaul (etapa posterior a Audit 5)

Estado de preparación aislada: [prototipo y límites actuales](STATUS.md). Esto no congela Audit 5 ni declara terminada la renovación.

> **Estado: APROBADO COMO DIRECCIÓN DE PRODUCTO; IMPLEMENTACIÓN NO INICIADA.**
> Se realizará **después de cerrar Audit 5**. Esta documentación es una especificación para desarrollo futuro, no una declaración de funcionalidades ya implementadas.
> Fecha de decisión: 2026-11-04.

## 1. Decisión del producto

La experiencia visual actual de TFL Core **no satisface el nivel de calidad esperado** fuera del menú principal y, parcialmente, del menú ESC. La siguiente gran actualización, una vez finalizado Audit 5, será una **reconstrucción coherente de todas las pantallas GUI creadas o modificadas por TFL Core**.

**Lo que se conserva como punto de partida:**
- **Menú principal:** preservar su composición y personalidad general, incluido el fondo en vídeo. Revisar específicamente calidad, proporciones, jerarquía y estados de los botones; no rediseñar de cero sin evidencia.
- **ESC:** preservar los elementos que funcionan y gustan: disposición izquierda, vista del mundo, acceso al chat inferior derecho, botón de cierre y ocultación temporal del HUD. Darle pulido visual, espaciado, controles y detalles; no perder el comportamiento funcional.
- **Marca:** azul TFLives, diseño moderno y limpio, coherencia entre 1.8.9 Ornithe Gen2 y 26.3 Fabric.
- **Funcionalidad y datos:** mantener ajustes, módulos, perfiles, HUD, mapas, waypoints, carpetas y compatibilidad de configuración.

**Todo lo demás es candidato a rediseño de raíz, sin eliminar funciones:** panel de configuración, Right Shift, navegador de módulos, ajustes gráficos, editores de HUD y perfiles, pantallas TFL de mundos/servidores cuando estén intervenidas por el mod, interfaces de mapas/waypoints, clips/capturas, overlays, diálogos, tooltips, controles y estados.

Referencias de calidad: Lunar Client (claridad, jerarquía, sobriedad), Laby Client/LabyMod (riqueza funcional y organización), CMPack (referencia estética pendiente de análisis de capturas verificables). **No replicar identidades, diseños propietarios, iconos ni assets de terceros.** El resultado debe sentirse inequívocamente TFL.

## 2. Principio rector

No se trata de cambiar colores y radios pantalla por pantalla. Primero se define el **sistema de diseño**, después su capa de componentes compartidos y adaptadores, y finalmente las pantallas. La UI debe sentirse como **un producto**, no como varias pantallas de mods ensambladas.

**Prioridades:** calidad visual y experiencia de usuario; compatibilidad con el comportamiento y los datos existentes; fiabilidad gráfica; rendimiento medido; soporte de ambos targets.

La complejidad visual debe ser deliberada y configurada; no añadir efectos caros de forma global. No reducir silenciosamente la calidad por rendimiento ni forzar blur, vídeo o animaciones si el usuario solicita modo reducido.

## 3. Documentación normativa de esta actualización

1. [VISUAL_DIRECTION.md](VISUAL_DIRECTION.md) — alcance, diseño de cada pantalla, experiencia deseada, límites y decisiones.
2. [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md) — tokens, tipografía, componentes, layout, interacciones, movimiento, accesibilidad y renderizado.
3. [IMPLEMENTATION_AND_ACCEPTANCE.md](IMPLEMENTATION_AND_ACCEPTANCE.md) — plan de trabajo, prioridades, QA, criterios de aceptación y rollback.

En caso de conflicto con implementaciones heredadas, la especificación de esta carpeta describe **la meta visual futura**; los invariantes y obligaciones funcionales de `AGENTS.md`, de Audit 4 y Audit 5 permanecen obligatorios.

## 4. Estrategia de trabajo

- Esta rama documental `docs/post-audit5-visual-overhaul` parte del estado de `feat/audit5-development` y **no debe ser fusionada ni aplicada a código productivo hasta concluir Audit 5**.
- Al iniciar la actualización visual, incorporar la documentación a la rama de desarrollo vigente después de comprobar divergencias; no sustituir los avances posteriores con un reset/force-push.
- Preparar referencias reales y capturas *antes* de diseñar, pero no frenar toda la etapa por una investigación interminable.
- Implementar por tandas funcionales y hacer commits recuperables; validar visualmente y probar interacciones tanto en 1.8.9 como en 26.3.
- No iniciar builds o tests de hardware/GitHub Actions ahora por crear estos documentos.
- Esta etapa **no incluye** el launcher de escritorio ni los modpacks de TFL Selection; son proyectos separados.

## 5. Condición de inicio y de finalización

**Iniciar:** Audit 5 cerrado o explícitamente congelado con sus pendientes conocidos, rama de integración identificada y baseline de capturas reproducible.

**Terminar:** pantallas intervenidas por TFL Core coherentes con el sistema, flujos completos y controles funcionales, aceptación visual en ambos targets, pruebas de accesibilidad y teclado, estados gráficos restaurados, configuración preservada y builds de prueba recuperables. No declarar satisfacción estética de la persona responsable del producto sin su revisión real.

**No confundir:** «documentado», «implementado», «compilado», «probado en CI» y «aprobado visualmente» son estados distintos.
