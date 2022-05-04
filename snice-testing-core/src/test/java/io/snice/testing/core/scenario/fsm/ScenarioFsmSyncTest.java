package io.snice.testing.core.scenario.fsm;

import io.snice.testing.core.Session;
import io.snice.testing.core.scenario.Scenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.snice.testing.core.scenario.fsm.ScenarioState.EXEC;
import static io.snice.testing.core.scenario.fsm.ScenarioState.SYNC;
import static org.mockito.Mockito.verify;

/**
 * Test all events that SYNC handles.
 */
class ScenarioFsmSyncTest extends ScenarioFsmTestBase {

    private Session session;
    private Scenario scenario;

    @Override
    @BeforeEach
    void setUp() {
        super.setUp();
        session = newSession("Test Sync");
    }

    /**
     * Happy base test where a single synchronous action is to be executed.
     */
    @Test
    public void testActionCompletes() {
        scenario = newScenario("Test Exec Sync");
        final var exec = driveToExec(session, scenario);
        driveToSync(exec);
        driveSyncJobCompletes(exec, session);
    }

    /**
     * For a synchronous action, we will sit and wait in the SYNC state for that
     * action to complete before we transition back to EXEC. However, while in the
     * SYNC state, other asynchronous actions may complete and those needs to be
     * processed correctly and also not to be confused with the synchronous action
     * that we are sitting and waiting for. The below test case tests that...
     */
    @Test
    public void testAsyncJobCompletesWhileWaitingForSyncJob() {
        final var async = mockActionBuilder(true);
        final var sync = mockActionBuilder(false);
        final var scenario = newScenario("Async followed by Sync", async, sync);

        driveToInit(session, scenario);
        final var execAsync = driveAsyncAction(session, scenario, 0);
        final var execSync = driveExec(session, scenario, 1);
        driveToSync(execSync);

        // Now we are assuming that the asynchronous job finishes and its ActionFinished message is
        // consumed by the FSM. We should still be in the SYNC state since this job was NOT the one
        // we are waiting for.
        final var asyncJobComplete = driveJobCompletes(execAsync, session);
        assertState(SYNC);

        // Also, let's assume the underlying actor that drove the async job also terminates while
        // we are in the SYNC state.
        driveActionActorTerminates(execAsync);
        assertState(SYNC);

        // And now finish the sync job, which should transition us over to the EXEC state again, which
        // has no other actions to execute and therefore will "post" a new no action job but NOTE, since
        // we are not driving that message we will still be in the EXEC state (other unit tests will test the
        // EXEC -> WRAP transition)
        driveJobCompletes(execSync, asyncJobComplete.session());
        assertState(EXEC);
        verify(ctx).tell(new ScenarioMessage.NoMoreActions());
    }

}