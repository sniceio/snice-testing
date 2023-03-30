package io.snice.testing.examples.http;

import io.snice.testing.core.scenario.Simulation;
import io.snice.testing.runtime.Snice;

import static io.snice.testing.core.CoreDsl.scenario;
import static io.snice.testing.http.HttpDsl.get;
import static io.snice.testing.http.HttpDsl.http;
import static io.snice.testing.http.check.HttpCheckSupport.status;

public class Http201 extends Simulation {

    {
        final var http = http().baseUrl("http://local.honeypot.snice.io:8080");
        final var get = get("/happy").check(status().is(200));
        final var scenario = scenario("Simple GET").execute(get);

        setUp(scenario).protocols(http);
    }

    public static void main(final String... args) {
        Snice.start(args).sync();
    }

}
