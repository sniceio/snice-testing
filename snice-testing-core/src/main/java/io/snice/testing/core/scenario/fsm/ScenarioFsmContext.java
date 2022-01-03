package io.snice.testing.core.scenario.fsm;

import io.hektor.actors.fsm.FsmActorContextSupport;
import io.hektor.fsm.Context;
import io.snice.testing.core.Session;
import io.snice.testing.core.scenario.Scenario;
import io.snice.testing.core.scenario.ScenarioContex;

public interface ScenarioFsmContext extends Context, FsmActorContextSupport {

    /**
     * The {@link Scenario} that we are supposed to be running.
     *
     * @return
     */
    Scenario scenario();

    ScenarioContex scenarioContext();

    void processActionResult(Session session);

    void processFinalResult(Session session);

}
