package io.snice.testing.core.scenario.fsm;

import io.hektor.fsm.Data;
import io.snice.testing.core.Session;
import io.snice.testing.core.scenario.InternalActionBuilder;
import io.snice.testing.core.scenario.Scenario;

import java.util.Optional;

import static io.snice.preconditions.PreConditions.assertNotNull;

public class ScenarioData implements Data {

    /**
     * This is always the current "official" {@link Session} and is what will be used in the
     * next ask to execute a new action. I.e. through the {@link ScenarioMessage.Exec} message.
     */
    private Session session;
    private Optional<Scenario> scenario = Optional.empty();
    private int actionIndex = 0;

    public ScenarioData() {
    }

    public Session session() {
        return session;
    }

    public void scenario(final Scenario scenario) {
        assertNotNull(scenario, "If you do not wish to set the Scenario, simply " +
                "don't call the method. This method assumes the Scenario is not null");
        this.scenario = Optional.of(scenario);
    }

    public void session(final Session session) {
        assertNotNull(scenario, "If you do not wish to set the Session, simply " +
                "don't call the method. This method assumes the Session is not null");
        this.session = session;
    }

    public boolean hasMoreActions() {
        return actionIndex < scenario.map(s -> s.actions().size()).orElse(0);
    }

    /**
     * Get the next {@link InternalActionBuilder} or else throw an {@link IllegalStateException}
     *
     * @return the next action
     */
    public InternalActionBuilder nextAction() {
        return scenario.map(s -> s.actions().get(actionIndex++))
                .map(builder -> (InternalActionBuilder) builder)
                .orElseThrow(() -> new IllegalStateException("Internal Error: There should have been more " +
                        "actions or it was not checked first, which is an internal bug"));
    }
}
