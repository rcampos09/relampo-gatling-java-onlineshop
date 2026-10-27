package onlineshop;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

/**
 * Práctica en vivo: primera simulation contra la API de demo de Gatling.
 * Aprendimos las 4 piezas: protocolo, escenario (con check y saveAs), inyección y ejecución.
 * Ejecutar: mvn gatling:test -Dgatling.simulationClass=onlineshop.TestSimulation -Dvu=1
 */
public class TestSimulation extends Simulation {

    // Parámetro por línea de comandos (-Dvu=5); si no se envía, usa 1 usuario.
    // Referencia: https://docs.gatling.io/guides/passing-parameters/
    private static final int vu = Integer.getInteger("vu", 1);

    // Protocolo HTTP: URL base y headers comunes a todos los requests.
    // Las cookies de sesión las maneja Gatling automáticamente.
    // Referencia: https://docs.gatling.io/reference/script/protocols/http
    HttpProtocolBuilder httpProtocol = http.baseUrl("https://api-ecomm.gatling.io")
            .acceptHeader("application/json")
            .userAgentHeader(
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/");

    // Escenario: un request GET /session.
    // check valida que el status sea 200 y jsonPath guarda el sessionId
    // en la sesión del usuario virtual para usarlo luego como #{SESSION_ID}.
    ScenarioBuilder scenario = scenario("Scenario")
        .exec(
            http("GET Session /session")
                .get("/session")
                .check(
                    status().is(200),
                    jsonPath("$.sessionId").saveAs("SESSION_ID")));

    // Inyección: cuántos usuarios entran y cómo (aquí todos a la vez, según -Dvu).
    // Se asocia el protocolo con .protocols(...).
    // Otras opciones: rampUsers, constantUsersPerSec.
    SetUp setup = setUp(
        scenario.injectOpen(atOnceUsers(vu)))
        .protocols(httpProtocol);
}
