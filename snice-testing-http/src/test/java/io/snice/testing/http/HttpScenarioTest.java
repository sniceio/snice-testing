package io.snice.testing.http;

import io.snice.codecs.codec.http.HttpMethod;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static io.snice.testing.core.CoreDsl.scenario;
import static io.snice.testing.http.HttpDsl.http;
import static io.snice.testing.http.check.HttpCheckSupport.header;
import static io.snice.testing.http.check.HttpCheckSupport.status;

@ExtendWith(MockitoExtension.class)
public class HttpScenarioTest {

    // @Test()
    public void buildHttpBasicScenario() throws InterruptedException {

        final var port = 5555;
        final var http = http().baseUrl("http://127.0.0.1:" + port);

        final var webhook = http("Accept Webhook")
                .accept(HttpMethod.POST, "/whatever")
                .saveAs("my webhook")
                .check(header("Connection").is("Close"))
                .respond(200)
                .header("nisse", "apa");

        /**
         http("Accept Webhook")
         .accept(POST, "/whatatever") // You can specify the relative path only. Snice will generate the rest
         .saveAs("my_webhook") // so we can reference it elsewhere. Mandatory parameter. Also must be unique within the scenario.
         .guard(req -> true) //
         .timeout("30s") // applies to each "wait" period. If you expect 3 callbacks
         .times(1) // once being
         .times(2) // exactly two times
         .times(1, 4)
         .atLeast(1)
         .max(3)
         .check(reqUri("asdf").saveAs("nisse"))
         .respond("200 OK")
         .header("Hello", "World")
         .header("Connection", "Close")
         */

        // Issue a POST with form-encoded data where one is the webhook
        final var installWebhook = http("Install My Webhook")
                .post("/")
                .content(Map.of("webhook", "${my webhook}"))
                .check(status().is(200));

        final var listRepos = http("Example.com")
                .get("/")
                .asJson()
                // .latch("hello_latch").countDown() // to use a latch
                .check(status().is(200).saveAs("hello_status"));

        final var scenario = scenario("My First Scenario")
                .executeAsync(webhook)
                /*
                .execute(session -> {
                    System.err.println("Webhook: " + session.attributes("my webhook"));
                    return session.attributes("ops", "oh man").markAsFailed();
                })
                 */
                .execute(installWebhook);
        // .execute(listRepos);
                /*
                .join("Accept Webhook")
                .execute(session -> session.attributes("hello", "world").markAsSucceeded());
                 */


        /*
        final var snice = Snice.run(scenario)
                .configuration(config)
                .protocols(http)
                .start();

        Thread.sleep(100000);

         */

    }
}
