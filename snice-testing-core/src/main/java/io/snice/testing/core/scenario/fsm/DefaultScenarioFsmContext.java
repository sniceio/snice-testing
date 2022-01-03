package io.snice.testing.core.scenario.fsm;

import io.hektor.core.ActorRef;
import io.snice.testing.core.Session;
import io.snice.testing.core.scenario.Scenario;
import io.snice.testing.core.scenario.ScenarioContex;

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
    public void processActionResult(final Session session) {
        self.tell(new ScenarioMessage.Exec(session));
    }

    @Override
    public void processFinalResult(final Session session) {
        parent.tell("And this is the final result from the Swedish Jury");
    }

}
