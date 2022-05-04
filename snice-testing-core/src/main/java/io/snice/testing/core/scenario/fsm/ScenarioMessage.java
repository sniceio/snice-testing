package io.snice.testing.core.scenario.fsm;

import io.snice.testing.core.Session;
import io.snice.testing.core.scenario.InternalActionBuilder;
import io.snice.testing.core.scenario.Scenario;

import java.util.List;

import static io.snice.preconditions.PreConditions.assertArrayNotEmpty;
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

    /**
     * Message to signal that there are no more actions left to execute within
     * the {@link Scenario}. There may of course still be asynchronous actions
     * that we need to wait to finish but nothing new to start.
     * <p>
     * Only the {@link ScenarioState#EXEC} state will emit this message.
     */
    record NoMoreActions() implements ScenarioMessage {
    }

    /**
     * Just join on the named actions
     */
    record JoinOn(String... actions) implements ScenarioMessage {

        public JoinOn {
            assertArrayNotEmpty(actions, "You must specify at least one Action to join on");
        }
    }
}
