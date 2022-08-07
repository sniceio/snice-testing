package io.snice.testing.runtime.fsm;

import io.snice.testing.core.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.snice.testing.runtime.fsm.ScenarioState.EXEC;
import static io.snice.testing.runtime.fsm.ScenarioState.WRAP;


/**
 * Test all events that ASYNC handles, which is really nothing since it is
 * a transient state. However, we are testing asynchronous actions here too
 * so even though all go back to EXEC right away, makes sense to test those
 * scenarios here.
 */
class ScenarioFsmAsyncTest extends ScenarioFsmTestBase {

    private Session session;

    @Override
    @BeforeEach
    void setUp() {
        super.setUp();
        session = newSession("Test Async");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testActionCompletes(final boolean asyncJobCompletesFirst) {
        final var execAsync = asyncScenario(session, 1).get(0);

        // Note that there is a race condition here since we created a scenario with async
        // actions, they will be kicked off, eventually finished but if the EXEC state
        // has no more actions to execute, most likely it'll transition over to the WRAP
        // state faster than any actions will complete. Our boolean will control this.
        if (asyncJobCompletesFirst) {
            driveJobCompletes(execAsync, session);
            assertState(EXEC);
        }

        fsm.onEvent(new ScenarioMessage.NoMoreActions());
        assertState(WRAP);

        // Note that the tests for the WRAP state will test that it handles
        // the driveJobCompletes etc etc.
    }


}