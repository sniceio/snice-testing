package io.snice.testing.examples.http;

import io.snice.testing.core.scenario.ExecutionPlan;
import io.snice.testing.runtime.Snice;

import static io.snice.testing.core.CoreDsl.scenario;
import static io.snice.testing.http.HttpDsl.get;
import static io.snice.testing.http.HttpDsl.http;
import static io.snice.testing.http.check.HttpCheckSupport.status;

public class Http201 extends ExecutionPlan {

    {
        final var http = http().baseUrl("http://honeypot.snice.io:8000");
        final var get = get("/happy").check(status().is(200));
        final var scenario = scenario("Simple GET").execute(get);

        setUp(scenario).protocols(http);
    }

    public static void main(final String... args) throws Exception {

        // 4. Ask Snice to just run the scenario using the given HTTP protocol. It'll return
        //    a CompletionStage that you can use to wait for the test to finish
        final var future = Snice.run(new Http201());

        // 5. We convert that CompletionStage to a Future so we can hang on it.
        future.toCompletableFuture().get();
    }

}
