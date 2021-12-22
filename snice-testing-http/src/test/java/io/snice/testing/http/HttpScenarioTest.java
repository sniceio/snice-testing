package io.snice.testing.http;

import io.snice.testing.core.Snice;
import io.snice.testing.core.SniceConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static io.snice.testing.core.CoreDsl.scenario;
import static io.snice.testing.http.HttpDsl.http;

@ExtendWith(MockitoExtension.class)
public class HttpScenarioTest {

    @Test
    public void buildHttpBasicScenario() {
        final var config = new SniceConfig();

        final var port = 8000;
        final var http = http(config)
                .baseUrl("http://localhost:" + port)
                .build();

        final var simpleHttpGet = http("GET Something").get("/hello").asJson();
        final var scenario = scenario("Simple HTTP GET")
                .execute(simpleHttpGet);


        final var snice = Snice.run(scenario)
                .configuration(config)
                .protocols(http)
                .start();

    }
}
