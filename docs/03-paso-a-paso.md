# 03 · Paso a paso: de la práctica en vivo a la solución

Dos partes: (A) lo que se aprendió en `TestSimulation`, y (B) cómo se limpia `RecordedSimulation` hasta llegar a `OnlineShopSimulation`.

---

## Parte A · Lo aprendido en `TestSimulation`

Primera simulation, contra la API de demo de Gatling (`https://api-ecomm.gatling.io`).

```bash
mvn gatling:test -Dgatling.simulationClass=onlineshop.TestSimulation -Dvu=1
```

| # | Qué se aprendió | En el código |
|---|---|---|
| 1 | Pasar parámetros por línea de comandos | `Integer.getInteger("vu", 1)` lee `-Dvu` |
| 2 | Configurar el protocolo HTTP | `http.baseUrl(...).acceptHeader(...)` |
| 3 | Armar un escenario con un request | `scenario(...).exec(http(...).get("/session"))` |
| 4 | Validar la respuesta | `status().is(200)` |
| 5 | **Extraer un valor** (primer paso hacia la correlación) | `jsonPath("$.sessionId").saveAs("SESSION_ID")` |
| 6 | Inyectar usuarios y asociar el protocolo | `injectOpen(atOnceUsers(vu))` y `.protocols(...)` |
| 7 | Leer el reporte | `target/gatling/testsimulation-<fecha>/index.html` |

Un error útil: si el check dice `status.find.is(201), found 403`, la ruta o el método están mal. Esa API solo responde a `GET /session`; un `POST` (o un typo en la ruta) devuelve `403 Missing Authentication Token`.

Los conceptos están explicados en [01-conceptos.md](01-conceptos.md).

---

## Parte B · De `RecordedSimulation` a `OnlineShopSimulation`

### Punto de partida: qué tiene el código del Recorder

`RecordedSimulation.java` corre sin errores, **pero no completa la compra**: ninguna de sus respuestas contiene "Your order is confirmed". El reporte dice 0 fallos porque casi no tiene checks, y un 200 no prueba que el flujo funcionó. Problemas que se ven en el archivo:

| Problema | Ejemplo en `RecordedSimulation` |
|---|---|
| Valores dinámicos fijos | `token=827f7ea1…`, `token=7d02c27f…`, `delivery_option[471]` |
| Datos de una persona fijos | nombre, dirección y teléfono escritos en cada `formParam` |
| Requests repetidos por reintentos | `request_8` a `request_11`: cuatro veces el mismo POST de dirección |
| Requests de interfaz, no del journey | `request_3` (refresco del producto) y `request_5` (AJAX del carrito) |
| Pausas de la grabación | `pause(16)`, `pause(22)`... eran los tiempos de quien grababa |
| Headers repetidos | mapas `headers_0 ... headers_8` con `Accept-Encoding`, `Origin`, etc. |
| Nombres poco claros | `request_0`, `request_4`... |
| Faltan pasos | no hay búsqueda ni "Personal Information" (en la grabación ya había sesión iniciada) |
| Ningún check de contenido | solo `status().is(200)` en el primer request |

### Paso 1 · Ejecutar el código tal cual y mirar el trace

```bash
mvn gatling:test -Dgatling.simulationClass=onlineshop.RecordedSimulation -Dhttp.log=TRACE | tee trace.log
```

Busca en `trace.log` si aparece `Your order is confirmed`. No aparece: el "OK" del reporte es un falso positivo. (No compartas `trace.log`: incluye cookies y datos del checkout.)

### Paso 2 · Copiar el archivo y limpiar la estructura

Trabaja sobre una copia (`OnlineShopSimulation.java`) y deja `RecordedSimulation` intacto como referencia.

- Quita los imports que no se usan (`io.gatling.javaapi.jdbc.*`).
- Mueve los headers comunes al protocolo y borra los mapas `headers_N`:

```java
HttpProtocolBuilder httpProtocol = http
    .baseUrl("http://testingyes.com")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Gatling training)");
```

### Paso 3 · Quitar lo que no es del journey

- Borra los POST de interfaz: el refresco del producto (`action=refresh`) y `ps_shoppingcart/ajax`.
- De los POST de dirección repetidos (`request_8` a `request_11`) deja **solo el último, el que fue aceptado**. Los anteriores son intentos fallidos (código postal inválido) de la grabación.

### Paso 4 · Dividir en pasos con nombre

Cada paso pasa a ser un `ChainBuilder` y los requests se numeran:

```java
ChainBuilder home = exec(http("01 Home").get("/onlineshop/"));
ScenarioBuilder scn = scenario("Compra invitado")
    .exec(home, search, productPage, addToCart, checkout, addresses, shipping, payment);
```

Los pasos quedan: `01 Home`, `02 Buscar producto`, `03 Detalle producto`, `04 Agregar al carro`, `05 Carro`, `06 Checkout`, `07 Personal Information`, `08 Addresses`, `09 Shipping Method`, `10 Payment`.

### Paso 5 · Reemplazar las pausas de la grabación

Las pausas del Recorder son los tiempos reales de quien grabó (hasta 22 s). Se cambian por rangos cortos y aleatorios:

```java
.pause(1, 3)    // en vez de pause(16)
```

### Paso 6 · Completar los pasos que faltan

- **Búsqueda** (`02`): el HAR iba de la home directo al producto. Se agrega `GET /onlineshop/search?controller=search&s=...`.
- **Personal Information** (`07`): en la grabación ya había sesión, así que no hay POST. La solución lo hace como **invitado** (`submitCreate=1`, `continue=1`), lo que crea un cliente por iteración.

### Paso 7 · Correlación: reemplazar cada valor fijo por uno extraído

Para cada valor fijo se hace lo mismo: (1) buscar en qué respuesta aparece por primera vez, (2) extraerlo con un check y `saveAs`, (3) usarlo con `#{...}`.

| Valor fijo en el Recorder | Dónde aparece primero | Extractor | Variable |
|---|---|---|---|
| `token=827f7ea1…` (add to cart) | JS de la Home: `"static_token":"…"` | `regex` | `staticToken` |
| URL del producto | Resultados de búsqueda | `css("article.product-miniature h2.product-title a","href")` | `productUrl` |
| `id_product=1` | Página del producto | `css("input#product_page_product_id","value")` | `productId` |
| `group[2]=8` (color) | Página del producto | `css("input[name='group[2]'][checked]","value")` | `colorId` |
| `token=7d02c27f…` (dirección) | Respuesta de Personal Information | `css("#delivery-address input[name=token]","value")` | `addressToken` |
| `delivery_option[471]=2,` | Respuesta de Addresses | `css("input[name^='delivery_option']","name" / "value")` | `deliveryField`, `deliveryValue` |

El nombre del campo de envío incluye el **id de la dirección** (`delivery_option[471]`), que cambia en cada compra, por eso se extrae el nombre completo del input y no solo el valor.

Cómo encontrarlos: busca el valor fijo (por ejemplo `827f7ea1`) en el HAR o en el `trace.log` y mira qué respuesta lo trae.

### Paso 8 · Parametrizar con CSV

Los datos de la persona grabada se reemplazan con feeders (ver [01-conceptos.md](01-conceptos.md)):

```java
FeederBuilder<String> products  = csv("data/products.csv").circular();
FeederBuilder<String> customers = csv("data/customers.csv").circular();
// ...
.formParam("firstname", "#{firstname}")
.formParam("id_state",  "#{idState}")
.formParam("postcode",  "#{postcode}")
```

- El **email** debe ser único por iteración; se arma con `emailUser` del CSV y un sufijo aleatorio.
- **Estado y código postal van en la misma fila**: el sitio rechaza combinaciones inválidas.
- La talla se deja fija (`group[1]=3`) porque la talla S no tiene stock.

### Paso 9 · Agregar checks de contenido

Cada paso verifica que está donde debe, y el último verifica el resultado de negocio:

```java
.check(css("#checkout-personal-information-step").exists())     // paso 06
.check(css("#checkout-payment-step.-current").exists())         // paso 09
.check(substring("Your order is confirmed"))                    // paso 10
```

### Paso 10 · Carga y assertions

Se reemplaza `atOnceUsers(1)` por una rampa parametrizable y se agregan criterios de éxito (ver [04-carga-y-stress.md](04-carga-y-stress.md)):

```java
scn.injectOpen(rampUsers(Integer.getInteger("users", 2)).during(Integer.getInteger("ramp", 10)))
```

### Paso 11 · Validar con un usuario y revisar el reporte

```bash
mvn gatling:test -Dgatling.simulationClass=onlineshop.OnlineShopSimulation -Dusers=1 -Dramp=1
```

Un solo usuario debe dar 0 errores y 14 requests (los 10 pasos más las redirecciones). Solo entonces se sube la carga.

### Resultado

| | `RecordedSimulation` | `OnlineShopSimulation` |
|---|---|---|
| Completa la compra | No | Sí |
| Valores dinámicos | Fijos | 6 correlaciones |
| Datos de usuario | Fijos | CSV + email único |
| Pausas | Las de la grabación | Rangos aleatorios cortos |
| Checks | Casi ninguno | En cada paso y en la confirmación |
| Reutilizable con varios usuarios | No | Sí |
