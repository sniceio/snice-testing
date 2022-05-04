package io.snice.testing.core.scenario.fsm;

import io.snice.testing.core.Session;
import io.snice.testing.core.scenario.Scenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Test all events that EXEC handles.
 */
class ScenarioFsmExecTest extends ScenarioFsmTestBase {

    private Session session;
    private Scenario scenario;

    @Override
    @BeforeEach
    void setUp() {
        super.setUp();

        // rarely any reason to change the default Session since it'll always
        // start from fresh
        session = newSession("Test Exec");
    }

    /**
     * Happy base test where a single synchronous action is to be executed.
     */
    @Test
    public void testExecSync() {
        scenario = newScenario("Test Exec Sync");
        final var exec = driveToExec(session, scenario);

        // just make sure that we are having all "parts" returned by the above and in all honesty,
        // we're really mainly testing the actual test case itself but still...
        assertThat(exec.execMsg(), notNullValue());
        assertThat(exec.sri(), notNullValue());
        assertThat(exec.job(), notNullValue());
    }

    /**
     *
     */
    @Test
    public void testExecAsync() {
        scenario = newAsyncScenario("Test Exec Sync");
        driveToInit(session, scenario);
        driveAsyncAction(session, scenario, 0);
    }

    /**
     * As actions are being kicked off, eventually we'll run out of actions to execute and
     * at that point, we should see a {@link ScenarioMessage.NoMoreActions} message being
     * submitted...
     */
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 10})
    public void testNoMoreActions(final int count) {

        final var actions = mockActionBuilder(count, true);
        final var scenario = newScenario("NoMoreActionsTest-" + count, actions);
        driveToInit(session, scenario);

        for (int i = 0; i < count; ++i) {
            final var exec = driveExec(session, scenario, i);
            fsm.onEvent(exec.execMsg());
        }

        // since there are no more actions left, the EXEC state should state that and publish a NoMoreActions msg
        verify(ctx).tell(eq(new ScenarioMessage.NoMoreActions()));
    }

}