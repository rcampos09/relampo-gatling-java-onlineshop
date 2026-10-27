package onlineshop;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.FeederBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.util.UUID;

/**
 * Journey: Home -> Buscar -> Producto -> Carro -> Checkout
 *   1 Personal Information -> 2 Addresses -> 3 Shipping -> 4 Payment
 */
public class OnlineShopSimulation extends Simulation {

  // ---------- Protocolo (cookies de sesión: automático, equivale al Cookie Manager) ----------
  HttpProtocolBuilder httpProtocol = http
      .baseUrl("http://testingyes.com")
      .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
      .acceptLanguageHeader("en-US,en;q=0.5")
      .userAgentHeader("Mozilla/5.0 (Gatling training)");

  // ---------- Datos: términos de búsqueda desde CSV ----------
  FeederBuilder<String> products = csv("data/products.csv").circular();
  // Datos del cliente (nombre, dirección, estado/código postal coherentes) desde CSV
  FeederBuilder<String> customers = csv("data/customers.csv").circular();

  // ---------- Pasos del journey ----------
  ChainBuilder home =
      exec(http("01 Home").get("/onlineshop/")
          .check(status().is(200))
          // CORRELACIÓN 1: token CSRF embebido en el JS de la página
          .check(regex("\"static_token\":\"([a-f0-9]+)\"").saveAs("staticToken")))
      .pause(1, 3);

  ChainBuilder search =
      feed(products)
      .exec(http("02 Buscar producto").get("/onlineshop/search")
          .queryParam("controller", "search")
          .queryParam("s", "#{searchTerm}")
          .check(status().is(200))
          // CORRELACIÓN 2: URL del primer producto del resultado
          .check(css("article.product-miniature h2.product-title a", "href").saveAs("productUrl")))
      .pause(1, 3);

  ChainBuilder productPage =
      exec(http("03 Detalle producto").get("#{productUrl}")
          .check(status().is(200))
          // CORRELACIÓN 3: id del producto desde la página de detalle
          .check(css("input#product_page_product_id", "value").saveAs("productId"))
          // CORRELACIÓN 3b: color por defecto (atributo del producto)
          .check(css("input[name='group[2]'][checked]", "value").saveAs("colorId")))
      .pause(2, 5);

  ChainBuilder addToCart =
      exec(http("04 Agregar al carro").post("/onlineshop/cart")
          .header("X-Requested-With", "XMLHttpRequest")
          .header("Accept", "application/json")
          .formParam("token", "#{staticToken}")
          .formParam("id_product", "#{productId}")
          .formParam("id_customization", "0")
          // talla 3 (la S, por defecto de la página, está sin stock)
          .formParam("group[1]", "3")
          .formParam("group[2]", "#{colorId}")
          .formParam("qty", "1")
          .formParam("add", "1")
          .formParam("action", "update")
          .check(status().is(200))
          .check(jmesPath("success").is("true")))
      .pause(1, 2);

  ChainBuilder checkout =
      exec(http("05 Carro").get("/onlineshop/cart?action=show").check(status().is(200)))
      .pause(1, 2)
      .exec(http("06 Checkout").get("/onlineshop/order")
          .check(status().is(200))
          .check(css("#checkout-personal-information-step").exists()))
      .pause(2, 4)
      // una fila del CSV por usuario virtual
      .feed(customers)
      // el email debe ser único por iteración (checkout invitado crea un cliente):
      // usuario del CSV + sufijo aleatorio
      .exec(session -> session.set("email",
          session.getString("emailUser") + "." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"))
      .exec(http("07 Personal Information").post("/onlineshop/order")
          .formParam("id_customer", "")
          .formParam("id_gender", "1")
          .formParam("firstname", "#{firstname}")
          .formParam("lastname", "#{lastname}")
          .formParam("email", "#{email}")
          .formParam("psgdpr", "1")
          .formParam("customer_privacy", "1")
          .formParam("submitCreate", "1")
          .formParam("continue", "1")
          .check(status().is(200))
          // CORRELACIÓN 4: token del formulario de direcciones
          .check(css("#delivery-address input[name=token]", "value").saveAs("addressToken")))
      .pause(2, 4);

  ChainBuilder addresses =
      exec(http("08 Addresses").post("/onlineshop/order?id_address=0")
          .formParam("back", "")
          .formParam("token", "#{addressToken}")
          .formParam("alias", "")
          .formParam("firstname", "#{firstname}")
          .formParam("lastname", "#{lastname}")
          .formParam("company", "")
          .formParam("address1", "#{address1}")
          .formParam("address2", "")
          .formParam("city", "#{city}")
          // OJO: estado y código postal deben ser un par válido (ver customers.csv)
          .formParam("id_state", "#{idState}")
          .formParam("postcode", "#{postcode}")
          .formParam("id_country", "21")
          .formParam("phone", "#{phone}")
          .formParam("saveAddress", "delivery")
          .formParam("use_same_address", "1")
          .formParam("submitAddress", "1")
          .formParam("confirm-addresses", "1")
          .check(status().is(200))
          // CORRELACIÓN 5: el nombre del campo de envío incluye el id de la dirección: delivery_option[472]
          .check(css("input[name^='delivery_option']", "name").saveAs("deliveryField"))
          .check(css("input[name^='delivery_option']", "value").saveAs("deliveryValue")))
      .pause(2, 4);

  ChainBuilder shipping =
      exec(http("09 Shipping Method").post("/onlineshop/order")
          .formParam("#{deliveryField}", "#{deliveryValue}")
          .formParam("delivery_message", "")
          .formParam("confirmDeliveryOption", "1")
          .check(status().is(200))
          .check(css("#checkout-payment-step.-current").exists()))
      .pause(2, 4);

  ChainBuilder payment =
      exec(http("10 Payment (transferencia)").post("/onlineshop/module/ps_wirepayment/validation")
          .formParam("conditions_to_approve[terms-and-conditions]", "1")
          // el POST responde 302 -> order-confirmation; Gatling sigue la redirección solo
          .check(status().is(200))
          .check(substring("Your order is confirmed")));

  // ---------- Escenario ----------
  ScenarioBuilder scn = scenario("Compra invitado")
      .exec(home, search, productPage, addToCart, checkout, addresses, shipping, payment);

  // ---------- Carga (suave: sitio de terceros) ----------
  {
    setUp(
        scn.injectOpen(rampUsers(Integer.getInteger("users", 2)).during(Integer.getInteger("ramp", 10)))
    ).protocols(httpProtocol)
     .assertions(
         global().failedRequests().percent().lt(1.0),
         // el pago crea la orden en el servidor (~19 s): se mide aparte para no esconderlo en el global
         details("10 Payment (transferencia)").responseTime().percentile(95).lt(25000),
         // el resto del journey debe ser rápido
         details("01 Home").responseTime().percentile(95).lt(3000),
         details("04 Agregar al carro").responseTime().percentile(95).lt(3000));
  }
}
