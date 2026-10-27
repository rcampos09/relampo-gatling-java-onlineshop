package onlineshop;

import java.time.Duration;
import java.util.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import io.gatling.javaapi.jdbc.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;
import static io.gatling.javaapi.jdbc.JdbcDsl.*;

public class RecordedSimulation extends Simulation {

  private HttpProtocolBuilder httpProtocol = http
    .baseUrl("http://testingyes.com")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/153.0.0.0 Safari/537.36");
  
  private Map<CharSequence, String> headers_0 = Map.ofEntries(
    Map.entry("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"),
    Map.entry("Accept-Encoding", "gzip, deflate"),
    Map.entry("Accept-Language", "es-ES,es;q=0.9,en;q=0.8"),
    Map.entry("Cache-Control", "max-age=0"),
    Map.entry("Upgrade-Insecure-Requests", "1")
  );
  
  private Map<CharSequence, String> headers_2 = Map.ofEntries(
    Map.entry("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"),
    Map.entry("Accept-Encoding", "gzip, deflate"),
    Map.entry("Accept-Language", "es-ES,es;q=0.9,en;q=0.8"),
    Map.entry("Upgrade-Insecure-Requests", "1")
  );
  
  private Map<CharSequence, String> headers_3 = Map.ofEntries(
    Map.entry("Accept", "application/json, text/javascript, */*; q=0.01"),
    Map.entry("Accept-Encoding", "gzip, deflate"),
    Map.entry("Accept-Language", "es-ES,es;q=0.9,en;q=0.8"),
    Map.entry("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8"),
    Map.entry("Origin", "http://testingyes.com"),
    Map.entry("X-Requested-With", "XMLHttpRequest")
  );
  
  private Map<CharSequence, String> headers_5 = Map.ofEntries(
    Map.entry("Accept", "*/*"),
    Map.entry("Accept-Encoding", "gzip, deflate"),
    Map.entry("Accept-Language", "es-ES,es;q=0.9,en;q=0.8"),
    Map.entry("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8"),
    Map.entry("Origin", "http://testingyes.com"),
    Map.entry("X-Requested-With", "XMLHttpRequest")
  );
  
  private Map<CharSequence, String> headers_8 = Map.ofEntries(
    Map.entry("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"),
    Map.entry("Accept-Encoding", "gzip, deflate"),
    Map.entry("Accept-Language", "es-ES,es;q=0.9,en;q=0.8"),
    Map.entry("Cache-Control", "max-age=0"),
    Map.entry("Origin", "http://testingyes.com"),
    Map.entry("Upgrade-Insecure-Requests", "1")
  );


  private ScenarioBuilder scn = scenario("RecordedSimulation")
    .exec(
      http("request_0")
        .get("/onlineshop/")
        .check(status().is(200)),
      pause(3),
      http("request_3")
        .post("/onlineshop/index.php?controller=product&token=827f7ea1ba9ee1e054114e8c99098d4a&id_product=1&id_customization=0&group%5B1%5D=3&group%5B2%5D=8&qty=1")
        .headers(headers_3)
        .formParam("quickview", "0")
        .formParam("ajax", "1")
        .formParam("action", "refresh")
        .formParam("quantity_wanted", "1"),
      pause(1),
      http("request_4")
        .post("/onlineshop/cart")
        .headers(headers_3)
        .formParam("token", "827f7ea1ba9ee1e054114e8c99098d4a")
        .formParam("id_product", "1")
        .formParam("id_customization", "0")
        .formParam("group[1]", "3")
        .formParam("group[2]", "8")
        .formParam("qty", "1")
        .formParam("add", "1")
        .formParam("action", "update"),
      http("request_5")
        .post("/onlineshop/module/ps_shoppingcart/ajax")
        .headers(headers_5)
        .formParam("id_customization", "0")
        .formParam("id_product_attribute", "5")
        .formParam("id_product", "1")
        .formParam("action", "add-to-cart"),
      pause(2),
      http("request_6")
        .get("/onlineshop/cart?action=show")
        .headers(headers_2),
      pause(2),
      http("request_7")
        .get("/onlineshop/order")
        .headers(headers_2),
      pause(16),
      http("request_8")
        .post("/onlineshop/order?id_address=0")
        .headers(headers_8)
        .formParam("back", "")
        .formParam("token", "7d02c27f6633428aff012c2f3844eb54")
        .formParam("alias", "")
        .formParam("firstname", "Test")
        .formParam("lastname", "Gatling")
        .formParam("company", "ACME")
        .formParam("address1", "123 Main St")
        .formParam("address2", "Apt 4")
        .formParam("city", "Seattle")
        .formParam("id_state", "48")
        .formParam("postcode", "1234567")
        .formParam("id_country", "21")
        .formParam("phone", "5550100")
        .formParam("saveAddress", "delivery")
        .formParam("use_same_address", "1")
        .formParam("submitAddress", "1")
        .formParam("confirm-addresses", "1"),
      pause(8),
      http("request_9")
        .post("/onlineshop/order?id_address=0")
        .headers(headers_8)
        .formParam("back", "")
        .formParam("token", "7d02c27f6633428aff012c2f3844eb54")
        .formParam("alias", "")
        .formParam("firstname", "Test")
        .formParam("lastname", "Gatling")
        .formParam("company", "ACME")
        .formParam("address1", "123 Main St")
        .formParam("address2", "Apt 4")
        .formParam("city", "Seattle")
        .formParam("id_state", "48")
        .formParam("postcode", "1234567")
        .formParam("id_country", "21")
        .formParam("phone", "5550100")
        .formParam("saveAddress", "delivery")
        .formParam("use_same_address", "1")
        .formParam("submitAddress", "1")
        .formParam("confirm-addresses", "1"),
      pause(13),
      http("request_10")
        .post("/onlineshop/order?id_address=0")
        .headers(headers_8)
        .formParam("back", "")
        .formParam("token", "7d02c27f6633428aff012c2f3844eb54")
        .formParam("alias", "")
        .formParam("firstname", "Test")
        .formParam("lastname", "Gatling")
        .formParam("company", "ACME")
        .formParam("address1", "123 Main St")
        .formParam("address2", "Apt 4")
        .formParam("city", "Seattle")
        .formParam("id_state", "50")
        .formParam("postcode", "1234567")
        .formParam("id_country", "21")
        .formParam("phone", "5550100")
        .formParam("saveAddress", "delivery")
        .formParam("use_same_address", "1")
        .formParam("submitAddress", "1")
        .formParam("confirm-addresses", "1"),
      pause(22),
      http("request_11")
        .post("/onlineshop/order?id_address=0")
        .headers(headers_8)
        .formParam("back", "")
        .formParam("token", "7d02c27f6633428aff012c2f3844eb54")
        .formParam("alias", "")
        .formParam("firstname", "Test")
        .formParam("lastname", "Gatling")
        .formParam("company", "ACME")
        .formParam("address1", "123 Main St")
        .formParam("address2", "Apt 4")
        .formParam("city", "Seattle")
        .formParam("id_state", "50")
        .formParam("postcode", "98001")
        .formParam("id_country", "21")
        .formParam("phone", "5550100")
        .formParam("saveAddress", "delivery")
        .formParam("use_same_address", "1")
        .formParam("submitAddress", "1")
        .formParam("confirm-addresses", "1"),
      pause(4),
      http("request_12")
        .post("/onlineshop/order")
        .headers(headers_8)
        .formParam("delivery_option[471]", "2,")
        .formParam("delivery_message", "")
        .formParam("confirmDeliveryOption", "1"),
      pause(8),
      http("request_13")
        .post("/onlineshop/module/ps_wirepayment/validation")
        .headers(headers_8)
    );

  {
	  setUp(scn.injectOpen(atOnceUsers(1))).protocols(httpProtocol);
  }
}
