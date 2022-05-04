package io.snice.testing.core.scenario.fsm;

import io.snice.testing.core.Session;
import io.snice.testing.core.scenario.Scenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test all events that SYNC handles.
 */
class ScenarioFsmWrapTest extends ScenarioFsmTestBase {

    private Session session;
    private Scenario scenario;

    @Override
    @BeforeEach
    void setUp() {
        super.setUp();
        session = newSession("Test Wrap");
    }

    @ParameterizedTest
    @ValueSource(ints = {1})
    public void testWrapThingsUp(final int count) {
        final var execs = asyncScenario(session, count);

        // Remember that there are no more actions left to execute
        // and we already checked that as part of the scenario drive above.
        // Hence, actually have to have the event processed by the FSM before
        // we continue.
        fsm.onEvent(new ScenarioMessage.NoMoreActions());

        for (int i = 0; i < count; ++i) {
            final var exec = execs.get(i);
            driveJobCompletes(exec, session);
            driveActionActorTerminates(exec);
        }
    }


}