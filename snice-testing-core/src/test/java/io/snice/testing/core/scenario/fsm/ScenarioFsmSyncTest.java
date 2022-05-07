package io.snice.testing.core.scenario.fsm;

import io.snice.identity.sri.ActionResourceIdentifier;
import io.snice.testing.core.Session;
import io.snice.testing.core.scenario.Scenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static io.snice.testing.core.scenario.fsm.ScenarioState.ERROR;
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
     * Ensure that we are able to execute many sync jobs after each other. The main thing
     * here is that if we don't correctly clear out the outstanding synchronous action/job, we
     * should not be able to start another synchronous job since only one can be active at any given
     * point in time.
     */
    @ParameterizedTest
    @ValueSource(ints = {2, 3, 5})
    public void testScenarioWithManySyncActions(final int count) {
        final var actions = mockActionBuilder(count, false);
        final var scenario = newScenario("Many Sync Actions", actions);

        driveToInit(session, scenario);

        for (int i = 0; i < count; ++i) {
            final var exec = driveExec(session, scenario, i);
            driveToSync(exec);
            driveJobCompletes(exec, exec.job().session());
            assertState(EXEC);
        }

        verify(ctx).tell(new ScenarioMessage.NoMoreActions());
    }

    /**
     * If there is a bug and we somehow manage to process, or emit, the wrong {@link ActionMessage.ActionFinished} message
     * then the FSM should detect this, complain but continue.
     */
    @Test
    public void testOutstandingSyncAndThenWrongSyncJobCompletes() {
        final var sync = mockActionBuilder(false);
        final var scenario = newScenario("Many Sync Actions", sync);
        driveToInit(session, scenario);

        final var exec = driveExec(session, scenario, 0);
        driveToSync(exec);

        // Now, instead of driving the given job to completion, let's fake it and post
        // the wrong (a made up one) ActionFinished message. If this ever were to happen, there
        // is a bug in the execution environment, and we'll need to fix it. But, we shouldn't kill
        // the current running scenario, which is why this error handling exists.
        final var wrongSri = ActionResourceIdentifier.of();
        final var wrongSession = new Session("Random Wrong Session");
        final var wrongActionFinished = new ActionMessage.ActionFinished(wrongSri, wrongSession, List.of());
        fsm.onEvent(wrongActionFinished);

        // ensure we transitioned through the ERROR state and also reported that error on the context
        assertTransition(SYNC, ERROR);
        assertTransition(ERROR, SYNC);
        verify(ctx).reportError(new ScenarioMessage.ErrorAction(SYNC, wrongActionFinished));

        // and now finish off the real job.
        driveJobCompletes(exec, exec.job().session());
        assertState(EXEC);

        verify(ctx).tell(new ScenarioMessage.NoMoreActions());
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