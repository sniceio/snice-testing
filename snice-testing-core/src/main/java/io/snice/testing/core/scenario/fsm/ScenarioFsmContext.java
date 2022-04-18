package io.snice.testing.core.scenario.fsm;

import io.hektor.actors.fsm.FsmActorContextSupport;
import io.hektor.fsm.Context;
import io.snice.testing.core.Execution;
import io.snice.testing.core.Session;
import io.snice.testing.core.scenario.Scenario;
import io.snice.testing.core.scenario.ScenarioContex;

import java.util.List;

public interface ScenarioFsmContext extends Context, FsmActorContextSupport {

    /**
     * The {@link Scenario} that we are supposed to be running.
     *
     * @return
     */
    Scenario scenario();

    ScenarioContex scenarioContext();

    /**
     * The FSM doesn't necessarily need to know that we are executing in an actor environment
     * but an FSM may very well issue messages that needs to be "published" somewhere. Since
     * we do not wish to be dependent on a particular actor framework, this {@link #tell(ScenarioMessage)}
     * method abstracts that away.
     *
     * @param msg
     */
    void tell(ScenarioMessage msg);

    // ActionResourceIdentifier executeSynchronously(Session session, InternalActionBuilder action);

    // ActionResourceIdentifier executeAsynchronously(Session session, InternalActionBuilder action);


    /**
     * Signal that the {@link Scenario} has been validated and is ok to actually execute.
     */
    // void scenarioOk();

    /**
     * Signal that the {@link Scenario} has been validated and is NOT ok to actually execute.
     */
    // void scenarioNotOk();

    void processActionResult(List<Execution> executions, Session session);

    void processFinalResult(List<Execution> executions, Session session);

}
