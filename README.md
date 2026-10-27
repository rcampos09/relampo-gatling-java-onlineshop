# Gatling (Java + Maven): del HAR a una prueba de carga

Material de la capacitación de Gatling para el equipo de Relampo. Se construye una prueba de carga del
journey de compra de una tienda PrestaShop de práctica: <http://testingyes.com/onlineshop/>

```
Home → Buscar producto → Agregar al carro → Checkout
     → 1 Personal Information → 2 Addresses → 3 Shipping Method → 4 Payment
```

## Qué contiene el repo

| Archivo | Qué es |
|---|---|
| `src/test/java/onlineshop/TestSimulation.java` | Práctica en vivo: la primera simulation (protocolo, escenario, check, inyección) contra la API de demo de Gatling. |
| `src/test/java/onlineshop/RecordedSimulation.java` | Código **tal como lo generó el Recorder** desde un HAR. Sirve de punto de partida: no completa la compra. |
| `src/test/java/onlineshop/OnlineShopSimulation.java` | **Solución**: el mismo journey limpio, correlacionado, con CSV, pausas y assertions. |
| `src/test/resources/data/*.csv` | Datos de prueba (feeders): productos y clientes. |
| `src/test/resources/logback-test.xml` | Configuración del trace HTTP. |
| `docs/` | Guías detalladas (ver más abajo). |

## Requisitos

| Herramienta | Versión | Verificar con |
|---|---|---|
| JDK | 17 o superior (probado con 21) | `java -version` |
| Maven | 3.9+ | `mvn -v` |

Gatling no se instala aparte: Maven descarga Gatling 3.16.0 y el plugin 4.21.12 la primera vez (unos minutos).

## Inicio rápido

```bash
cd gatling-onlineshop
mvn gatling:test -Dgatling.simulationClass=onlineshop.OnlineShopSimulation -Dusers=1 -Dramp=1
```

Al terminar, la consola indica la ruta del reporte HTML (`target/gatling/<simulacion>-<fecha>/index.html`).

## Comandos

En **Windows PowerShell** pon cada `-D...` entre comillas: `mvn gatling:test "-Dgatling.simulationClass=onlineshop.TestSimulation"`.

| Qué quieres hacer | Comando |
|---|---|
| Práctica en vivo | `mvn gatling:test -Dgatling.simulationClass=onlineshop.TestSimulation -Dvu=1` |
| Código del Recorder (ejemplo) | `mvn gatling:test -Dgatling.simulationClass=onlineshop.RecordedSimulation` |
| Solución, prueba de humo (1 usuario) | `mvn gatling:test -Dgatling.simulationClass=onlineshop.OnlineShopSimulation -Dusers=1 -Dramp=1` |
| Solución, carga suave (3 usuarios, rampa 10 s) | `mvn gatling:test -Dgatling.simulationClass=onlineshop.OnlineShopSimulation -Dusers=3 -Dramp=10` |
| Trace HTTP completo (1 usuario) | `mvn gatling:test -Dgatling.simulationClass=onlineshop.OnlineShopSimulation -Dusers=1 -Dramp=1 -Dhttp.log=TRACE \| tee trace.log` |
| Solo requests fallidos | igual que arriba con `-Dhttp.log=DEBUG` |
| Abrir el Recorder | `mvn gatling:recorder` |
| Compilar sin ejecutar | `mvn test-compile` |

Parámetros que entiende la solución: `-Dusers` (usuarios, por defecto 2), `-Dramp` (segundos de rampa, por defecto 10) y `-Dhttp.log` (`TRACE`, `DEBUG` o apagado).

> **Cuidado con la carga.** `testingyes.com` es un sitio compartido y **cada iteración crea un cliente y una orden reales** (y descuenta stock). Mantén la carga en unos pocos usuarios, y confirma con el responsable del sitio antes de subir.

## Documentación del repo

Dejé las guías en la carpeta `docs/` y no en este README: el README se queda corto y sirve de portada, y cada guía se puede leer y actualizar por separado.

| Guía | Contenido |
|---|---|
| [docs/01-conceptos.md](docs/01-conceptos.md) | Protocolo HTTP, escenario, feeders, pausas, checks, inyección y assertions. |
| [docs/02-recorder.md](docs/02-recorder.md) | Cómo grabar un HAR y abrir el Recorder en Mac y Windows, con la configuración recomendada. |
| [docs/03-paso-a-paso.md](docs/03-paso-a-paso.md) | Lo aprendido en `TestSimulation` y cómo pasar de `RecordedSimulation` a `OnlineShopSimulation`. |
| [docs/04-carga-y-stress.md](docs/04-carga-y-stress.md) | Cómo armar una prueba de carga y una de stress, y cómo leer el reporte. |

## Documentación oficial de Gatling

- [Inicio](https://docs.gatling.io/) · [Conceptos](https://docs.gatling.io/concepts/)
- [Protocolo HTTP](https://docs.gatling.io/reference/script/protocols/http/protocol/) · [Checks](https://docs.gatling.io/reference/script/core/checks/)
- [Feeders](https://docs.gatling.io/reference/script/core/session/feeders/) · [Escenario y pausas](https://docs.gatling.io/reference/script/core/scenario/)
- [Perfiles de inyección](https://docs.gatling.io/reference/script/core/injection/) · [Assertions](https://docs.gatling.io/reference/script/core/assertions/)
- [Recorder](https://docs.gatling.io/reference/script/http/recorder/) · [Plugin de Maven](https://docs.gatling.io/reference/integrations/build-tools/maven-plugin/) · [Pasar parámetros](https://docs.gatling.io/guides/passing-parameters/)

## Problemas frecuentes

| Síntoma | Causa probable |
|---|---|
| `release version 17 not supported` | Usar JDK 17 o superior (`JAVA_HOME`). |
| `No attribute named 'x' is defined` | Un check anterior falló y no guardó el valor; revisa el paso previo. |
| `Jackson failed to parse` en `jmesPath` | Falta el header `Accept: application/json`. |
| `found nothing` en un `css` | La página no es la esperada (sesión, carro vacío o stock agotado). |
| La dirección no avanza a Shipping | Estado y código postal no forman un par válido (ver `customers.csv`). |
