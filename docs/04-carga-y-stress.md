# 04 · Pruebas de carga y de stress

La simulation (el journey) es la misma. Lo que cambia entre un tipo de prueba y otro es **el perfil de inyección** y lo que buscas en el reporte.

| Prueba | Pregunta que responde | Perfil |
|---|---|---|
| **Humo** (smoke) | ¿El script funciona de punta a punta? | 1 usuario |
| **Carga** (load) | ¿El sistema cumple los objetivos con la demanda esperada? | Rampa y meseta estable |
| **Stress** | ¿Hasta dónde aguanta y cómo falla? | Carga creciente hasta que se degrada |
| **Pico** (spike) | ¿Qué pasa con un aumento brusco? | Salto repentino y vuelta a la normalidad |

> **Sobre este sitio:** `testingyes.com` es compartido y cada iteración crea una orden real. Los números de los ejemplos son **ilustrativos** y están pensados para tasas bajas. No los ejecutes contra `testingyes.com` sin autorización del responsable del sitio, y menos el stress.

## Modelo abierto vs cerrado

| Modelo | Controla | Úsalo para |
|---|---|---|
| **Abierto** (`injectOpen`) | La **tasa de llegada** de usuarios nuevos | Sitios públicos (tienda online), donde llegan usuarios sin importar si el sistema está lento |
| **Cerrado** (`injectClosed`) | La **cantidad de usuarios concurrentes** | Sistemas con aforo limitado (una intranet, un call center) |

Este proyecto usa el modelo abierto. Referencia: [Perfiles de inyección](https://docs.gatling.io/reference/script/core/injection/).

## Prueba de humo

```java
scn.injectOpen(atOnceUsers(1))
```

Siempre se corre primero. Si falla, hay que arreglar el script antes de generar carga.

## Prueba de carga

Sube de forma gradual, se mantiene en la demanda objetivo y baja:

```java
scn.injectOpen(
    rampUsersPerSec(0.05).to(0.2).during(60),     // calentamiento: de 0,05 a 0,2 usuarios/s en 60 s
    constantUsersPerSec(0.2).during(300),         // meseta: 0,2 usuarios/s durante 5 min
    rampUsersPerSec(0.2).to(0.05).during(30))     // bajada
```

- **Cómo definir los números:** parte de la demanda real (usuarios por hora en el día pico) y pásala a usuarios por segundo. Los usuarios concurrentes aproximados son `usuarios por segundo × duración del journey`. En esta tienda el journey dura ~1 a 2 min por las pausas.
- **Duración:** la meseta debe ser lo bastante larga para que las métricas se estabilicen (minutos, no segundos).
- **Qué mirar:** percentiles (p95 y p99, no solo el promedio), tasa de errores y throughput estable durante la meseta.

## Prueba de stress

Escalones crecientes hasta encontrar el punto de quiebre:

```java
scn.injectOpen(
    incrementUsersPerSec(0.1)          // cada escalón suma 0,1 usuarios/s
        .times(5)                      // 5 escalones
        .eachLevelLasting(60)          // 60 s en cada nivel
        .separatedByRampsLasting(10)   // 10 s de rampa entre niveles
        .startingFrom(0.1))            // nivel inicial
```

**Qué buscar**
- El nivel en que el p95 empieza a subir de forma notoria (el codo de la curva).
- El nivel en que aparecen errores (`failedRequests`) o se estanca el throughput.
- Cómo falla: timeouts, errores 5xx, respuestas lentas y cómo se recupera al bajar la carga.

## Prueba de pico

```java
scn.injectOpen(
    constantUsersPerSec(0.1).during(60),     // carga normal
    stressPeakUsers(20).during(10),          // 20 usuarios repartidos de golpe en 10 s
    constantUsersPerSec(0.1).during(60))     // vuelve a lo normal: ¿se recupera?
```

## Elegir el perfil por línea de comandos

Un patrón práctico es leer `-Dprofile` y devolver el perfil correspondiente:

```java
String profile = System.getProperty("profile", "smoke");
PopulationBuilder population = switch (profile) {
  case "load"   -> scn.injectOpen(/* perfil de carga */);
  case "stress" -> scn.injectOpen(/* perfil de stress */);
  case "spike"  -> scn.injectOpen(/* perfil de pico */);
  default       -> scn.injectOpen(atOnceUsers(1));
};
setUp(population).protocols(httpProtocol).assertions(/* ... */);
```

```bash
mvn gatling:test -Dgatling.simulationClass=onlineshop.OnlineShopSimulation -Dprofile=load
```

> Estado actual: `OnlineShopSimulation` ya usa `-Dusers` y `-Dramp` (una rampa simple). La selección con `-Dprofile` de arriba **no está incorporada**. Los snippets de esta guía compilan con Gatling 3.16.0, pero no se ejecutaron contra el sitio.

## Assertions: definir cuándo la prueba "pasa"

```java
.assertions(
    global().failedRequests().percent().lt(1.0),                                  // errores bajo 1 %
    details("01 Home").responseTime().percentile(95).lt(3000),                    // p95 por request
    details("10 Payment (transferencia)").responseTime().percentile(95).lt(25000))
```

Define un umbral **por request** cuando un paso es mucho más lento que el resto: un percentil global puede esconder que un solo paso concentra el problema. Si una assertion falla, Maven termina con error, útil para integrar la prueba en CI. Referencia: [Assertions](https://docs.gatling.io/reference/script/core/assertions/).

## Leer el reporte

Se abre `target/gatling/<simulacion>-<fecha>/index.html`.

| Dónde | Qué mirar |
|---|---|
| **Global Information** | Total de requests, errores, percentiles y throughput generales |
| **Statistics** (tabla) | Una fila por request: aquí se ve cuál paso es el lento |
| **Active users over time** | Que la forma coincida con el perfil de inyección que definiste |
| **Response time percentiles over time** | Si el tiempo de respuesta crece mientras sube la carga |
| **Requests / Responses per second** | Dónde se estanca el throughput |
| **Errors** | Qué check o status falló y cuántas veces |

Cómo juzgar si el comportamiento es aceptable:
1. ¿Errores bajo el umbral acordado (por ejemplo 1 %)?
2. ¿p95 y p99 dentro del objetivo, por request?
3. ¿Los tiempos se mantienen estables durante la meseta, o crecen sin parar?
4. ¿Qué paso concentra la lentitud?

En este proyecto, el pago (`10 Payment`) tarda ~19 s con 1 a 3 usuarios y ~40 s con 6 usuarios simultáneos, mientras los otros pasos están alrededor de 1 s o menos. Es un ejemplo de cómo un solo paso domina el reporte y se degrada al subir la concurrencia.
