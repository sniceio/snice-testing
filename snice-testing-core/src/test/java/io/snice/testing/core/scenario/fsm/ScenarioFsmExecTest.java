package io.snice.testing.core.scenario.fsm;

import io.snice.testing.core.Session;
import io.snice.testing.core.scenario.InternalActionBuilder;
import io.snice.testing.core.scenario.Scenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.snice.testing.core.scenario.fsm.ScenarioState.ASYNC;
import static io.snice.testing.core.scenario.fsm.ScenarioState.EXEC;
import static io.snice.testing.core.scenario.fsm.ScenarioState.JOIN;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        driveToInit(session, scenario);

        // As soon as we enter the EXEC state, it'll will (on the enter action) pop next action
        // off the list and send an EXEC message to itself.
        final var expectedAction = (InternalActionBuilder) scenario.actions().get(0);
        final var execMsg = new ScenarioMessage.Exec(expectedAction, session);
        verify(ctx).tell(execMsg);

        // This is the job that will be created and "kicked-off". The job is just a container for
        // an action.
        final var job = someJob();
        when(ctx.prepareExecution(eq(expectedAction), any(Session.class))).thenReturn(job);

        // Now that we know that the FSM did ask the ctx to "tell" with the given message, drive the
        // FSM using that very message. In the real execution environment, this would of course have been
        // handled by the Actor framework but this is a unit test so we are driving manually.
        fsm.onEvent(execMsg);

        // since this was a synchronous event, we should be in the SYNC state since we will sit and wait for
        // the action to finish (or for us to timeout)
        assertState(ScenarioState.SYNC);

        verify(job).start();
    }

    @Test
    public void testExecAsync() {
        final var asyncAction = mockActionBuilder(true);
        scenario = newScenario("Test Exec Sync", asyncAction);
        driveToInit(session, scenario);

        final var expectedAction = (InternalActionBuilder) scenario.actions().get(0);
        final var execMsg = new ScenarioMessage.Exec(expectedAction, session);
        verify(ctx).tell(execMsg);

        final var job = someJob();
        when(ctx.prepareExecution(eq(expectedAction), any(Session.class))).thenReturn(job);

        fsm.onEvent(execMsg);

        // we end up in JOIN because there are no more actions to execute (this unit test
        // only had a single async one) and we will not terminate the scenario until all
        // actions have completed. And of course, since this is a transient state we should
        // find ourselves traversing EXEC -> ASYNC -> JOIN so let's check that too
        assertTransition(EXEC, ASYNC);
        assertTransition(ASYNC, JOIN);
        assertState(JOIN);

        // ensure that the job was actually started since that is what actually
        // kicks-off the actual job (duh - it's called start for a reason)
        verify(job).start();

    }

}