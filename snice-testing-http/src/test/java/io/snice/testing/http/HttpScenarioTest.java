package io.snice.testing.http;

import io.netty.util.NetUtil;
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
        System.out.println("Is IPv4 stack preferred? " + NetUtil.isIpV4StackPreferred());
        final var config = new SniceConfig();

        final var port = 80;
        final var http = http(config)
                .baseUrl("https://example.com:" + port);

        final var listRepos = http("Example.com")
                .get("/")
                .asJson()
                // .latch("hello_latch").countDown() // to use a latch
                .check(status().is(200).saveAs("hello_status"));

        final var scenario = scenario("Simple HTTP GET")
                .execute(listRepos)
                // .latch("hello_latch").await() // to wait for a latch to open.
                .execute(session -> {
                    System.err.println("Session variable: " + session.attributes("hello_status"));
                    return session.attributes("ops", "oh man").markAsFailed();
                })
                .execute(session -> session.attributes("hello", "world").markAsSucceeded());


        final var snice = Snice.run(scenario)
                .configuration(config)
                .protocols(http)
                .start();

    }
}
