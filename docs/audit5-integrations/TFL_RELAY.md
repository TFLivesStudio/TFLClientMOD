# TFL Relay — diseño de dominio propio para compartir mundos

> **Diseño propuesto, no desplegado.** No se ha configurado DNS, Docker, VPS, firewall, broker ni un servidor de relay. Toda operación de infraestructura requiere autorización separada.
> Relacionado con [PLAN.md](PLAN.md). Dirigido a Minecraft 1.8.9 Ornithe y 26.3 Fabric.

## 1. Meta de producto

En lugar de recibir un dominio de tercero como `*.e4mc.link`, el anfitrión que elija «Compartir con amigos» recibiría una dirección temporal bajo un dominio controlado por TFLives. **Ejemplo ilustrativo, condicionado a propiedad DNS:** `marea-lunar.share.tflives.com`.

No exigir a los invitados instalar TFL Core. Deben entrar usando la dirección desde un cliente Minecraft compatible con el protocolo/version de partida, sujeto a autenticación y reglas vanilla. No prometer que 1.8.9 y 26.3 puedan conectarse entre sí sin un proxy/bridge externo.

## 2. Hallazgos de código comprobados

`xhyrom/e4mc-retro`, rama `1.8.x`, versión 1.0.2:
- `common/src/main/java/link/e4mc/Config.java` declara `useBroker`, `brokerUrl`, `relayHost`, `relayPort`, whitelist, TLS opcional inseguro (default false).
- `QuiclimeSession.java` usa QUIC hacia relay por UDP, solicitando `request_domain_assignment` en canal de control. El **relay asigna el dominio** y devuelve `domain_assignment_complete`.
- La conexión exterior hacia el host se reenvía a `127.0.0.1:localRelayPort` de la instancia que abrió el LAN.
- `useBroker=false` puede señalar el relay propio sin modificar la arquitectura, pero **NO cambia el dominio que el relay anuncia**.
- El código incluye `InsecureTrustManagerFactory` para la sesión QUIC en su versión observada: revisar modelo de confianza antes de una distribución propia. No declarar E2EE/autenticidad del relay sin evidencia.
- La rama por defecto `1.6.x` incluye un broker ejemplo de Cloudflare Workers; no trasladar sus supuestos al port 1.8.x sin pruebas.

e4mc moderno de `vgskye/e4mc-minecraft-architectury` dispone de release Fabric 26.3; valorar interoperabilidad con el mismo relay TFL o gateways específicos. El backend de relay original está documentado como `vgskye/e4mc-quiclime` (Rust; ramas pueden variar).

**Conclusión:** un CNAME hacia un relay de terceros **no** da control de asignaciones; se requiere infraestructura que realmente emita y resuelva nombres `*.share.<dominio>` y enrute las sesiones. La implementación final puede reutilizar código del relay existente si la licencia y los protocolos son compatibles.

## 3. Arquitectura propuesta

```
Jugador anfitrión
TFL Core → botón «Compartir» → LAN local de Minecraft
        │
        └── sesión saliente cifrada/autenticada hacia relay TFL
                                    │
                                    ▼
                            Broker/Control TFL
                          asignación nombre efímero
                              y ruta de sesión
                                    │
                                    ▼
                      Relay TFL (TCP Minecraft público)
                                    ▲
                                    │
                         *.share.<dominio propio>
                                    ▲
                                    │
                         Amigo (Minecraft vanilla)
```

Separar control y datos:
- **Broker HTTPS:** selección de relay y registro/expiración de asignaciones; no tiene que transportar todo el juego.
- **Relay QUIC/TCP:** recibe conexión saliente del host, expone entrada Minecraft e identifica la sesión a partir del hostname de handshake u otro mecanismo soportado.
- **DNS:** wildcard en subdominio propio a IP(s) del relay, plus reglas SRV/puertos si se usan. **No inventar** que TCP 25565 está libre ni que un proxy compartido puede enrutar sin cambios.
- **Host:** no abrir puertos domésticos ni exigir NAT entrante; conexión saliente.
- **Cierre:** revocación de nombre, socket shutdown, TTL corto y limpieza tras crash.

## 4. Opciones de despliegue

**Recomendada:** relay aislado con **IP pública y puertos dedicados**, separado de los proxies de TFL Network y del panel de control. DNS del subdominio propio. Permite una dirección limpia sin `:puerto` si se dispone de TCP 25565 dedicado; túnel QUIC por UDP con puerto elegido y autenticación/seguridad apropiadas.

**Alternativa:** usar IP compartida con un router TCP Minecraft-aware de hostnames y puertos. Requiere inspección/proxy del handshake, protección, pruebas de compatibilidad con Velocity y seguridad. No usarla solo para ahorrar una IP si amenaza TFL Network.

**Plan interino:** e4mc con broker/relay externo solo mediante elección informada; no disfrazar dominio tercero como infraestructura TFL. Si el servicio TFL no está disponible, mantener «Abrir a LAN local» operativo y mostrar error/reintento; fallback a tercero solo con consentimiento explícito, nunca en silencio.

## 5. Seguridad y privacidad — obligatorias

- Compartir mundo es **opt-in por sesión**. No abrir túnel al iniciar Minecraft o desde el launcher.
- UI muestra claramente «cualquiera con el enlace podría intentar conectarse», estado, duración, nombre, copiar y cerrar.
- Whitelist/allowlist operativa por mundo cuando aplique; respetar autenticación de cuentas y reglas Vanilla. Contraseña solo si existe un mecanismo real; no inventar enforcement del lado GUI.
- Rate limit de conexiones entrantes, sesiones concurrentes, conexiones por host, bytes y ancho de banda; mitigación de abusos y registros mínimos con retención definida.
- Verificar seguridad del protocolo de túnel: certificado/identidad del broker y relay, protección anti-MITM, transport encryption autenticado, rotación de claves, expiración de sesión, no habilitar `useInsecureTLS`.
- No registrar IPs, mensajes de chat, sesiones, tokens, handshakes o mapas en logs públicos de Actions. El relay conoce IPs de conexiones; no prometer anonimato extremo a anfitrión/invitados.
- Desconexión/reconexión con limpieza y backpressure; proteger tick thread, firewall y VPS frente a DDoS/agotamiento. No transportar datos privados del mundo a la infraestructura salvo los paquetes necesarios del túnel.
- Evitar integrar credenciales admin/FeatherPanel en el mod o en código público; ninguna URL de token en logs.
- Definir ToS/contacto abuso solo si se ofrece a terceros y se opera públicamente.

## 6. Compatibilidad/QA

1. Host 1.8.9 Java 25 Ornithe, invitado vanilla 1.8.9 externo.
2. Host 26.3 Fabric, invitado vanilla 26.3.
3. Dos sesiones al mismo tiempo y dos hosts con mundos diferentes; ninguna conexión cruzada.
4. LAN local solo, relay cerrado, broker caído, DNS inválido, TLS rechazado, UDP bloqueado y TCP bloqueado.
5. Host cierra mundo, reinicia, cambia dimensión, cierra el juego por error, apaga PC; limpieza comprobada.
6. Whitelist, ban, autenticación cuando corresponda, nombre de mundo legible, código de compartir/copy y sesiones expiradas.
7. Test de stress limitado: tamaño máximo, cantidad de conexiones y memoria/CPU del relay; métricas sin datos personales.
8. Firewall/puertos aislados del panel y TFL NETWORK; ninguna colisión con proxy existente.
9. Comprobar que clientes de la versión correcta puedan conectarse sin instalar mod.
10. Confirmar que el nombre del dominio se anuncia realmente por relay, no como string bonito en GUI.

## 7. Definition of Done

Se acepta integración con dominio propio cuando:
- El anfitrión recibe una URL temporal real del dominio controlado por TFLives.
- Un invitado externo llega al mundo por esa URL sin instalar mod adicional.
- Solo se expone el mundo mediante acción explícita y se corta al cerrar.
- No requiere port forwarding doméstico.
- Relay tiene límites, revocación, controles operativos y documentación de incidentes.
- Se verifica el protocolo real de e4mc Retro y moderno, no únicamente la conexión a broker.
- Hay plan de costes y capacidad: e4mc implica tráfico agregado y soporte; una VPS compartida puede no bastar.
- No se alteran funciones offline ni networking del resto de TFL Core.

**Dato pendiente:** dominio exacto al que el propietario tenga control DNS. `share.tflives.com` es ejemplo, no DNS existente verificado.
