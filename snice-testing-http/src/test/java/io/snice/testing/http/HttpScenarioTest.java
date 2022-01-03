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
                // .latch("hello_latch").countDown() // to use a latch
                .check(status().is(404).saveAs("hello_status"));

        final var scenario = scenario("Simple HTTP GET")
                .execute(simpleHttpGet)
                // .latch("hello_latch").await() // to wait for a latch to open.
                .execute(session -> {
                    System.err.println("Session variable: " + session.attributes("hello_status"));
                    return session.attributes("ops", "oh man").markAsFailed();
                });


        final var snice = Snice.run(scenario)
                .configuration(config)
                .protocols(http)
                .start();

    }
}
