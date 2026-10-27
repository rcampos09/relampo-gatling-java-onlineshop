# 01 · Conceptos de Gatling usados en el proyecto

Cada concepto apunta a un ejemplo real de `OnlineShopSimulation.java`.

## Estructura de una Simulation

Una clase que extiende `Simulation` tiene cuatro piezas:

| Pieza | Para qué | En el código |
|---|---|---|
| Protocolo HTTP | Configuración común de los requests | `http.baseUrl(...)` |
| Escenario | El journey: la secuencia de pasos | `scenario("Compra invitado").exec(...)` |
| Inyección | Cuántos usuarios entran y cómo | `injectOpen(rampUsers(...))` |
| Assertions | Criterios de éxito de la prueba | `.assertions(...)` |

## Protocolo HTTP (`HttpProtocolBuilder`)

Define lo que comparten todos los requests: URL base, headers por defecto, user agent, etc.

```java
HttpProtocolBuilder httpProtocol = http
    .baseUrl("http://testingyes.com")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Gatling training)");
```

- Con `baseUrl`, cada request usa solo la ruta: `.get("/onlineshop/")`.
- **Las cookies de sesión las maneja Gatling solo** por cada usuario virtual. Equivale al HTTP Cookie Manager de JMeter.
- Se conecta a la simulación con `.protocols(httpProtocol)` dentro de `setUp`.
- Referencia: [Protocolo HTTP](https://docs.gatling.io/reference/script/protocols/http/protocol/).

## Escenario y pasos (`ScenarioBuilder`, `ChainBuilder`)

Un escenario es una cadena de acciones. Para que se pueda leer, cada paso del journey es un `ChainBuilder`:

```java
ChainBuilder home = exec(http("01 Home").get("/onlineshop/").check(status().is(200)));
ScenarioBuilder scn = scenario("Compra invitado").exec(home, search, productPage, addToCart, ...);
```

El nombre que le pones a `http("...")` es el que aparece en el reporte, así que numera los pasos (`01 Home`, `02 Buscar producto`...).

## Pausas (`pause`): el think time

Una pausa simula el tiempo que un usuario real tarda entre una acción y otra (leer, escribir, decidir).

```java
.pause(1, 3)   // espera aleatoria entre 1 y 3 segundos
.pause(2)      // espera fija de 2 segundos
```

- Sin pausas, cada usuario virtual dispara requests sin parar: la carga real es mucho mayor que la de usuarios reales.
- Una pausa **no cuenta** en los tiempos de respuesta del reporte.
- Equivale al Think Time de JMeter. Referencia: [Escenario](https://docs.gatling.io/reference/script/core/scenario/).

## Feeders: datos desde un CSV

Un feeder entrega una fila de datos a cada usuario virtual. Cada columna queda disponible como `#{nombreColumna}`.

`src/test/resources/data/customers.csv`:

```csv
firstname,lastname,emailUser,address1,city,idState,postcode,phone
Ana,Rojas,ana.rojas,123 Pine St,Seattle,50,98001,2065550101
```

```java
FeederBuilder<String> customers = csv("data/customers.csv").circular();
// ...
.feed(customers)                                  // toma la siguiente fila
.formParam("firstname", "#{firstname}")           // usa el dato
```

| Estrategia | Comportamiento |
|---|---|
| `.circular()` | Recorre las filas y vuelve al inicio al terminar (la que usamos). |
| `.random()` | Elige una fila al azar. |
| `.queue()` | Una vez por fila; **falla si se acaban las filas**. |
| `.shuffle()` | Orden aleatorio, una vez por fila. |

- **Regla de oro:** un dato que debe ser único (por ejemplo el email) no se deja solo en el CSV. En el proyecto se arma `emailUser` + un sufijo aleatorio con una función de sesión.
- Datos relacionados (estado y código postal) deben ir en la **misma fila**, para que sean coherentes.
- Referencia: [Feeders](https://docs.gatling.io/reference/script/core/session/feeders/).

## Checks y correlación

Un check valida la respuesta y/o extrae un valor y lo guarda en la sesión del usuario (`saveAs`). Extraer un valor dinámico de una respuesta y reutilizarlo en un request posterior es la **correlación**.

```java
.check(status().is(200))                                              // validar
.check(regex("\"static_token\":\"([a-f0-9]+)\"").saveAs("staticToken")) // extraer con regex
.check(css("input#product_page_product_id", "value").saveAs("productId")) // extraer con CSS
.check(jmesPath("success").is("true"))                                // validar un JSON
// después:
.formParam("token", "#{staticToken}")
```

| Extractor | Úsalo para |
|---|---|
| `regex` | Valores dentro de JavaScript o texto libre |
| `css` | Atributos y texto del HTML |
| `jmesPath` / `jsonPath` | Respuestas JSON |
| `substring` | Verificar que aparece un texto |

**Un 200 no prueba que el flujo funcionó.** Pon siempre un check de contenido en el último paso (aquí `substring("Your order is confirmed")`). Referencia: [Checks](https://docs.gatling.io/reference/script/core/checks/).

## Inyección

Define cuántos usuarios virtuales entran y con qué patrón. Se detalla en [04-carga-y-stress.md](04-carga-y-stress.md).

```java
scn.injectOpen(rampUsers(3).during(10))   // 3 usuarios repartidos en 10 s
```

## Assertions

Convierten el reporte en un resultado de aprobado o fallido (útil en CI). En este proyecto cada paso tiene su propio criterio:

```java
.assertions(
    global().failedRequests().percent().lt(1.0),
    details("10 Payment (transferencia)").responseTime().percentile(95).lt(25000))
```

Referencia: [Assertions](https://docs.gatling.io/reference/script/core/assertions/).

## Parámetros por línea de comandos

```java
Integer.getInteger("users", 2)   // lee -Dusers=5; si no viene, usa 2
```

Referencia: [Pasar parámetros](https://docs.gatling.io/guides/passing-parameters/).
