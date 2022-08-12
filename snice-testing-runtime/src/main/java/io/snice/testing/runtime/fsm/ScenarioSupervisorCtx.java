package io.snice.testing.runtime.fsm;

import io.hektor.fsm.Context;
import io.snice.testing.core.Session;
import io.snice.testing.core.protocol.ProtocolRegistry;
import io.snice.testing.core.scenario.Scenario;

public interface ScenarioSupervisorCtx extends Context {

    /**
     * Called by the FSM when we enter the {@link ScenarioSupervisorState#RUNNING}
     */
    void isRunning();

    void runScenario(final Session session, final Scenario scenario, final ProtocolRegistry registry);

}
