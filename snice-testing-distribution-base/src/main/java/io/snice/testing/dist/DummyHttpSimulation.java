package io.snice.testing.dist;

import io.snice.testing.core.scenario.Simulation;

import static io.snice.testing.core.CoreDsl.scenario;
import static io.snice.testing.http.HttpDsl.get;
import static io.snice.testing.http.HttpDsl.http;
import static io.snice.testing.http.check.HttpCheckSupport.status;

/**
 * Only purpose is to have a maven project that includes all dependencies for projects to
 * be able to write against Snice Testing. The dummay examples are just that, dummy examples.
 */
public class DummyHttpSimulation extends Simulation {

    {
        final var http = http().baseUrl("http://honeypot.snice.io:8000");
        final var get = get("/happy").check(status().is(200));
        final var scenario = scenario("Simple GET").execute(get);

        setUp(scenario).protocols(http);
    }

}
