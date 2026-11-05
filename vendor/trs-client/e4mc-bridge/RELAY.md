# TFL local broker and relay

This is standalone infrastructure; it is excluded from the client bridge JAR.
No public TFL service, DNS record, firewall rule or hosting deployment is created.

Build with JDK 25:

```
../gradlew -p . test relayDist
java -cp 'build/relay/lib/*' dev.theredstonee.trsclient.e4mcbridge.relay.TflRelay --key=/secure/private.pem --cert=/secure/fullchain.pem
```

The default bind is **127.0.0.1**. Defaults: HTTPS broker 8443, UDP/QUIC 25575,
Minecraft TCP 25565, broker-advertised relay hostname `localhost`, suffix
`share.test`. Test names require a local resolver/hosts fixture; they are not public
addresses. Keys must be RSA PKCS#8 PEM. Never commit keys. The certificate must be
trusted by the client and cover the advertised relay/broker hostname. Certificate
verification is mandatory, including in tests (private test CA).

An operator-approved deployment would need a public host, a valid DNS name such
as `relay.share.<owned-domain>`, an A/AAAA wildcard for temporary names, a trusted
certificate/renewal, TCP 443 (or a specified HTTPS port), UDP 25575, TCP 25565 and
explicit network/hosting approval. Configure `--bind=`, `--relay-host=`, `--suffix=`,
`--https-port=`, `--quic-port=` and `--guest-port=` for that environment. The broker
path is `/getBestRelay`. An HTTPS reverse proxy must preserve this response without
caching. No administrative credential belongs in Core. Do not expose the test
certificate or assume wildcard DNS alone creates sessions.

QUIClime is selected explicitly for both game adapters. The inspected modern e4mc
implementation also implements Dialtone/Iroh; this relay does **not** implement or
advertise those extensions. Native Minecraft guests use ordinary TCP handshakes,
without installing Core. Authentication and whitelist remain on the LAN server;
Core rejects attempts to log in using the integrated host's name before forwarding
login bytes. Stopping internet sharing leaves LAN open until the world closes.

Bounds: 32 host sessions, 8 guests per host, 2-hour leases, 10-second allocation
deadline, 5-second guest routing deadline, 30-second idle guest read deadline,
1 MiB pending per pipe, 100 broker/guest attempts per IP per minute with at most
1024 rate entries, 32 queued HTTPS requests and HMAC/address-bound QUIC retry
tokens valid for 30 seconds. Assignments contain 128 random bits and disappear on
disconnect/expiry. There are no game-content, player-name or address logs. Restart
revokes all leases. These are basic abuse controls, not a production DDoS service.

Local acceptance tests cover trusted/mismatched/untrusted TLS peers, allocation,
ordinary Minecraft TCP routing, bidirectional bytes, buffer limits and revocation.
A raw TCP fixture is not a completed second Minecraft gameplay test.
