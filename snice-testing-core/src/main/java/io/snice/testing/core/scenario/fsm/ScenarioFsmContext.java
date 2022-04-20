package io.snice.testing.core.scenario.fsm;

import io.hektor.fsm.Context;
import io.snice.testing.core.Execution;
import io.snice.testing.core.Session;
import io.snice.testing.core.scenario.InternalActionBuilder;
import io.snice.testing.core.scenario.Scenario;
import io.snice.testing.core.scenario.ScenarioContex;

import java.util.List;

public interface ScenarioFsmContext extends Context {

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
     * Whenever a new action is supposed to be executed, it'll be executed as a separate "job" in a separate
     * FSM and Actor. Ask the {@link ScenarioSupervisorCtx} to create such an {@link ActionJob} and once
     * {@link ActionJob#start()} the execution will begin.
     *
     * @param action  the action builder that contains the action to eventually run.
     * @param session the session to be used along with this job.
     * @return a representation of the "job" that will take place elsewhere.
     */
    ActionJob prepareExecution(InternalActionBuilder action, Session session);


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