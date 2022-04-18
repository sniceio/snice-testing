package io.snice.testing.core.scenario.fsm;

import io.hektor.core.ActorRef;
import io.snice.testing.core.Execution;
import io.snice.testing.core.Session;
import io.snice.testing.core.scenario.Scenario;
import io.snice.testing.core.scenario.ScenarioContex;

import java.util.List;

import static io.snice.preconditions.PreConditions.assertNotNull;

public record DefaultScenarioFsmContext(ActorRef parent,
                                        ActorRef self,
                                        Scenario scenario,
                                        ScenarioContex scenarioContext) implements ScenarioFsmContext {

    public DefaultScenarioFsmContext {
        assertNotNull(parent);
        assertNotNull(self);
        assertNotNull(scenario);
        assertNotNull(scenarioContext);
    }

    @Override
    public void tell(final ScenarioMessage msg) {
        assertNotNull(msg);
        ctx().self().tell(msg);
    }

    @Override
    public void processActionResult(final List<Execution> executions, final Session session) {
        System.err.println("Apparently processing the action result");
        // self.tell(new ScenarioMessage.Exec(executions, session));
    }

    @Override
    public void processFinalResult(final List<Execution> executions, final Session session) {
        parent.tell(new ScenarioSupervisorMessages.RunCompleted(scenario, session, executions));
    }

}
