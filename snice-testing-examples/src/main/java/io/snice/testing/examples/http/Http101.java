package io.snice.testing.examples.http;

import io.snice.testing.runtime.Snice;

import static io.snice.testing.http.HttpDsl.get;
import static io.snice.testing.http.check.HttpCheckSupport.status;

/**
 * Demonstrates the most basic example of how to issue an HTTPS GET request
 * towards example.com and ensure that the response is a 200 OK.
 */
public class Http101 {

    public static void main(final String... args) throws Exception {

        // 1. Create the HTTPS GET request and add any checks to it.
        final var get = get("https://example.com").check(status().is(200));
        final var post = get("https://example.com").check(status().is(200));

        // 2. Ask Snice to just run that GET request for you. It'll return
        //    a CompletionStage that you can use to wait for the test to finish
        final var future = Snice.run(get, post);

        // 3. We convert that CompletionStage to a Future so we can hang on it.
        future.toCompletableFuture().get();
    }
}
