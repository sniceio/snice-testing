package io.snice.testing.core.scenario.fsm;

import io.snice.testing.core.Session;
import io.snice.testing.core.scenario.InternalActionBuilder;
import io.snice.testing.core.scenario.Scenario;

import java.util.List;

import static io.snice.preconditions.PreConditions.assertNotNull;

public interface ScenarioMessage {

    record Init(Session session, Scenario scenario) implements ScenarioMessage {

        public Init {
            assertNotNull(session);
            assertNotNull(scenario);
        }
    }

    record OkScenario() implements ScenarioMessage {
    }

    record BadScenario(List<String> errors) implements ScenarioMessage {
    }

    record Exec(InternalActionBuilder action, Session session) implements ScenarioMessage {
    }

    record Terminate() implements ScenarioMessage {
    }
}
