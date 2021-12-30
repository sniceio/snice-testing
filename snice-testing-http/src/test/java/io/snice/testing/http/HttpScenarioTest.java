package io.snice.testing.http;

import io.snice.testing.core.Snice;
import io.snice.testing.core.SniceConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static io.snice.testing.core.CoreDsl.scenario;
import static io.snice.testing.http.HttpDsl.http;
import static io.snice.testing.http.check.HttpCheckSupport.status;

@ExtendWith(MockitoExtension.class)
public class HttpScenarioTest {

    @Test
    public void buildHttpBasicScenario() {
        final var config = new SniceConfig();

        final var port = 80;
        final var http = http(config)
                .baseUrl("http://example.com:" + port);

        final var simpleHttpGet = http("GET Something")
                .get("/hello")
                .asJson()
                .check(status().is(200).saveAs("hello_status"))
                .check(status().is(300).saveAs("hello_300"));

        final var scenario = scenario("Simple HTTP GET")
                .execute(simpleHttpGet);


        final var snice = Snice.run(scenario)
                .configuration(config)
                .protocols(http)
                .start();

    }
}
