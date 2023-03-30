package io.snice.testing.examples.http;

import io.snice.testing.runtime.Snice;

import static io.snice.testing.core.CoreDsl.scenario;
import static io.snice.testing.http.HttpDsl.get;
import static io.snice.testing.http.HttpDsl.http;
import static io.snice.testing.http.check.HttpCheckSupport.status;

public class Http102 {

    public static void main(final String... args) throws Exception {

        // 1. You can optionally specify the base URL to use for all future
        //    requests, so you do not have to type it all out.
        final var http = http().baseUrl("https://honeypot.snice.io");

        // 2. Create the HTTP GET request and add any checks to it.
        //    Note that since we now have a base url, the final FQDN will be
        //    http://honeypot.snice.io/happy
        final var get = get("/happy").check(status().is(200));

        // 3. Although creating a {@link Scenario} may be overkill when we only
        //    have a single GET request, we must do so if we want to specify/configure
        //    the HTTP protocol, as we did in Step 1.
        final var scenario = scenario("Simple GET").execute(get);

        // 4. Ask Snice to just run the scenario using the given HTTP protocol. It'll return
        //    a CompletionStage that you can use to wait for the test to finish
        final var future = Snice.run(scenario, http);

        // 5. We convert that CompletionStage to a Future so we can hang on it.
        future.toCompletableFuture().get();
    }

}
