package io.snice.testing.runtime.fsm;

import io.snice.testing.core.Execution;
import io.snice.testing.core.Session;
import io.snice.testing.core.protocol.ProtocolRegistry;
import io.snice.testing.core.scenario.Scenario;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ScenarioSupervisorMessages {

    record Init() {
    }

    record Run(Scenario scenario, Session session, ProtocolRegistry registry, CompletableFuture<Void> future) {
    }

    /**
     * Message to signify the completion of a run, which then will contain the collected
     * information about the {@link Execution} and the resulting {@link Session}
     */
    record RunCompleted(Scenario scenario, Session session, List<Execution> executions) {
    }

    record Terminate() {
    }
}
