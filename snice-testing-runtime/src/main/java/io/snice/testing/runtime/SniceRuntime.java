package io.snice.testing.runtime;

import io.snice.testing.core.MessageBuilder;
import io.snice.testing.core.action.ActionBuilder;
import io.snice.testing.core.protocol.Protocol;
import io.snice.testing.core.scenario.Scenario;
import io.snice.testing.core.scenario.Simulation;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public interface SniceRuntime {

    /**
     * Start the snice eco-system.
     *
     * @return a start future that you can hang on to wait gracefully for the runtime to fully start.
     */
    Future<SniceRuntime> start();

    /**
     * Obtain a shutdown future that, when completed, guarantees that the {@link SniceRuntime} has been
     * fully shutdown and terminated.
     *
     * @return
     */
    CompletionStage<Void> sync();

    /**
     * Run the given {@link Scenario}.
     *
     * @param scenario  the {@link Scenario} to run.
     * @param protocols the {@link Protocol}s that the given {@link Scenario} needs.
     * @return
     */
    CompletionStage<Void> run(Scenario scenario, List<Protocol> protocols);

    /**
     * Run the given {@link Simulation}
     *
     * @param simulation the {@link Simulation} to run.
     * @return a {@link CompletionStage} that will complete once the {@link Simulation}
     * has finished (success or failure)
     */
    <T extends Simulation> CompletionStage<Void> run(T simulation);

    default CompletionStage<Void> run(final Scenario scenario, final Protocol.Builder... protocols) {
        return run(scenario, Arrays.stream(protocols)
                .map(Protocol.Builder::build)
                .collect(Collectors.toUnmodifiableList()));
    }

    default CompletionStage<Void> run(final Scenario scenario, final Protocol... protocols) {
        return run(scenario, List.of(protocols));
    }

    /**
     * Convenience method for kicking off a single action. Mainly used for simple test cases
     * and demos.
     */
    CompletionStage<Void> run(final ActionBuilder builder);

    /**
     * Convenience method for kicking off a test with a single message to send. Mainly used for simple test cases
     * and demos.
     */
    CompletionStage<Void> run(final MessageBuilder... message);
}
