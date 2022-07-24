package io.snice.testing.runtime;

import io.snice.testing.core.protocol.Protocol;
import io.snice.testing.core.scenario.Scenario;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public interface SniceRuntime {

    /**
     * Start the snice eco-system.
     *
     * @return a shutdown future that you can hang on to wait gracefully for the runtime to complete.
     */
    Future<Void> start();

    /**
     * Run the given {@link Scenario}.
     *
     * @param scenario  the {@link Scenario} to run.
     * @param protocols the {@link Protocol}s that the given {@link Scenario} needs.
     * @return
     */
    CompletionStage<Void> runScenario(Scenario scenario, List<Protocol> protocols);

    default CompletionStage<Void> runScenario(final Scenario scenario, final Protocol.Builder... protocols) {
        return runScenario(scenario, Arrays.stream(protocols)
                .map(Protocol.Builder::build)
                .collect(Collectors.toUnmodifiableList()));
    }

    default CompletionStage<Void> runScenario(final Scenario scenario, final Protocol... protocols) {
        return runScenario(scenario, List.of(protocols));
    }
}
