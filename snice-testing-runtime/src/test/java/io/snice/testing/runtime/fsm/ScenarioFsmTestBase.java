package io.snice.testing.runtime.fsm;

import io.hektor.core.ActorPath;
import io.hektor.core.LifecycleEvent;
import io.hektor.fsm.FSM;
import io.hektor.fsm.TransitionListener;
import io.snice.identity.sri.ActionResourceIdentifier;
import io.snice.logging.Alert;
import io.snice.logging.Logging;
import io.snice.testing.core.Execution;
import io.snice.testing.core.Session;
import io.snice.testing.core.action.Action;
import io.snice.testing.core.scenario.InternalActionBuilder;
import io.snice.testing.core.scenario.Scenario;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.verification.VerificationMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.snice.testing.runtime.fsm.ScenarioState.ASYNC;
import static io.snice.testing.runtime.fsm.ScenarioState.EXEC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class ScenarioFsmTestBase implements Logging {

    protected static final Logger logger = LoggerFactory.getLogger(ScenarioFsmTestBase.class);

    protected UUID uuid;

    protected FSM<ScenarioState, ScenarioFsmContext, ScenarioData> fsm;
    protected ScenarioData data;
    protected ScenarioFsmContext ctx;

    protected BiConsumer<ScenarioState, Object> unhandledEventHandler;

    protected TransitionListener<ScenarioState> transitionListener;

    @BeforeEach
    void setUp() {
        initializeFsm();
    }

    @AfterEach
    public void tearDown() {
        // All the unit tests should NOT have any events that are unhandled. If you do wish
        // to test that, it seems more a Hektor.io test as opposed to testing the
        // ScenarioFsm itself. As such, this should be true for ALL our unit tests.
        verifyNoInteractions(unhandledEventHandler);
    }

    void initializeFsm() {
        uuid = UUID.randomUUID();
        data = new ScenarioData();
        ctx = mock(ScenarioFsmContext.class);
        unhandledEventHandler = mock(BiConsumer.class);
        transitionListener = mock(TransitionListener.class);

        fsm = newFsm();
        fsm.start();
    }

    /**
     * Simple convenience method to get you through the INIT state. This
     * one assumes the {@link Scenario} is a valid scenario.
     */
    protected void driveToInit(final Session session, final Scenario scenario) {
        fsm.onEvent(new ScenarioMessage.Init(session, scenario));

        // When the INIT state evaluates the scenario for correctness,
        // it'll call the context stating everything is ok at which point
        // the ctx will publish an OK message.
        verify(ctx).tell(new ScenarioMessage.OkScenario());
        fsm.onEvent(new ScenarioMessage.OkScenario());
    }

    /**
     * Simple convenience method to get your through to the EXEC state (via INIT of course).
     * This default flow assumes the {@link Scenario} is valid and there is a single
     * synchronous action to execute.
     *
     * @return a holder of various things that you need to drive the next part of your test.
     */
    protected ExecHolder driveToExec(final Session session, final Scenario scenario) {
        driveToInit(session, scenario);
        return driveExec(session, scenario, 0);
    }

    /**
     * Drive the EXEC state. It assumes we have just entered the EXEC state so you have to drive
     * the scenario to that point. Typically, by calling {@link #driveToInit(Session, Scenario)}, which
     * will (by default) run a happy scenario that'll end up entering the EXEC state.
     *
     * @param session
     * @param scenario
     * @param actionNo the action that is to be executed next.
     * @return
     */
    protected ExecHolder driveExec(final Session session, final Scenario scenario, final int actionNo) {
        assertState(EXEC);
        // As soon as we enter the EXEC state, it'll will (on the enter action) pop next action
        // off the list and send an EXEC message to itself.
        final var expectedAction = (InternalActionBuilder) scenario.actions().get(actionNo);
        final var execMsg = new ScenarioMessage.Exec(expectedAction, session);
        verify(ctx).tell(execMsg);

        // This is the job that will be created and "kicked-off". The job is just a container for
        // an action.
        final var job = someJob(expectedAction.sri(), expectedAction.isAsync(), session);
        when(ctx.prepareExecution(eq(expectedAction), any(Session.class))).thenReturn(job);

        return new ExecHolder(execMsg, expectedAction.sri(), job);
    }

    protected ExecHolder driveAsyncAction(final Session session, final Scenario scenario, final int actionNo) {
        return driveAsyncAction(session, scenario, actionNo, times(1));
    }

    /**
     * This helper method will drive the FSM for a asynchronous job. It assumes you're already in the {@link ScenarioState#EXEC}
     * state and that the next job to execute, as indicated by the parameter <code>actionNo</code>, is a asynchronous
     * action (no check will ensure this is true but the transitions will not be correcft through the FSM
     * so if the test fails, could be that the action was actually a synchronous one so you ended up
     * in the {@link ScenarioState#SYNC} instead)
     *
     * @param session
     * @param scenario
     * @param actionNo
     * @param mode     if you have a scenario that calls this many times, then the {@link TransitionListener} will be called
     *                 over and over and since that is a mocked class, when we assert the transition, we must actually
     *                 know how many times it has been called or mockito verification will fail (since we said it should
     *                 have been invoked once but it was invoked more).
     * @return
     */
    protected ExecHolder driveAsyncAction(final Session session, final Scenario scenario, final int actionNo, final VerificationMode mode) {

        // the "driveExec" ensured that the EXEC onEnter action did the right thing
        // and that there is a message to be sent to self to drive us to the next state.
        // Hence, just grab that message and send it to self.
        final var exec = driveExec(session, scenario, actionNo);
        fsm.onEvent(exec.execMsg());

        // For an asynchronous job, we will end up in the ASYNC state and then right back at the EXEC state
        assertTransition(transitionListener, EXEC, ASYNC, mode);
        assertTransition(transitionListener, ASYNC, EXEC, mode);

        // and then of course, we're back to EXEC
        assertState(EXEC);

        // ensure that the job was actually started since that is what actually
        // kicks-off the actual job (duh - it's called start for a reason)
        verify(exec.job()).start();

        return exec;

    }

    protected List<ExecHolder> driveAllAsyncActions(final Session session, final Scenario scenario) {
        final var execs = IntStream.range(0, scenario.actions().size())
                .mapToObj(i -> driveAsyncAction(session, scenario, i, times(i + 1)))
                .collect(Collectors.toUnmodifiableList());

        verify(ctx).tell(new ScenarioMessage.NoMoreActions());

        return execs;
    }


    /**
     * Helper method to kick-off many async actions and ensure that all of those jobs are indeed
     * started. We will end up on the EXEC state still but now with {@link ScenarioMessage.NoMoreActions}
     * BUT that message is yet to be processed by the FSM. Hence, you choose in your unit test when
     * the {@link ScenarioMessage.NoMoreActions} event should be processed and at that point, you'll move the
     * FSM to the {@link ScenarioState#WRAP} state.
     *
     * @param session
     * @param count
     * @return
     */
    protected List<ExecHolder> asyncScenario(final Session session, final int count) {
        final var async = mockActionBuilder(count, true);
        final var scenario = newScenario("Action Completes Test", async);
        driveToInit(session, scenario);
        final var execs = driveAllAsyncActions(session, scenario);

        verify(ctx).tell(new ScenarioMessage.NoMoreActions());

        return execs;
    }

    /**
     * Drive the FSM to the SYNC state. This assumes we are currently in the EXEC state
     *
     * @return
     */
    protected ExecHolder driveToSync(final ExecHolder execInfo) {
        assertState(EXEC);
        // Now that we know that the FSM did ask the ctx to "tell" with the given message, drive the
        // FSM using that very message. In the real execution environment, this would of course have been
        // handled by the Actor framework but this is a unit test so we are driving manually.
        fsm.onEvent(execInfo.execMsg());

        // since this was a synchronous event, we should be in the SYNC state since we will sit and wait for
        // the action to finish (or for us to timeout)
        assertState(ScenarioState.SYNC);

        verify(execInfo.job()).start();

        return execInfo;
    }

    /**
     * Drive the FSM from the SYNC state back to the EXEC state by completing the outstanding
     * SYNC job.
     * <p>
     * This is the happy case.
     *
     * @param execInfo
     * @param session
     * @return
     */
    protected JobCompletesHolder driveSyncJobCompletes(final ExecHolder execInfo, final Session session) {
        assertState(ScenarioState.SYNC);
        final var jobCompletesHolder = driveJobCompletes(execInfo, session);
        assertState(EXEC);

        return jobCompletesHolder;
    }

    /**
     * Helper function to "complete" a job, which means to create an {@link ActionMessage.ActionFinished} message
     * and "feed" that to the FSM.
     * <p>
     * Since a job can complete in many states (while in SYNC, ASYNC, EXEC, etc), the below
     * helper flow makes no assumptions about the FSM state etc. Other helper methods/unit tests should
     * verify that the FSM starts/ends in the correct state depending on the test.
     *
     * @param execInfo contains the information about the action that was kicked off.
     * @param session
     * @return info object about the completion of the action.
     */
    protected JobCompletesHolder driveJobCompletes(final ExecHolder execInfo, final Session session) {

        // Eventually the job will finish. Pretend our action changed the session...
        final var newSession = session.attributes("fake", "from unit test");
        final var execution = someExecution(true);
        final var actionFinished = new ActionMessage.ActionFinished(execInfo.sri(), newSession, List.of(execution));
        fsm.onEvent(actionFinished);

        return new JobCompletesHolder(newSession, actionFinished, execution);
    }

    protected void driveActionActorTerminates(final ExecHolder execInfo) {
        final var actorPath = ActorPath.of(execInfo.sri().toString());
        final var terminated = LifecycleEvent.terminated(actorPath);
        fsm.onEvent(terminated);
    }

    /**
     * Helper method to just ensure that the given event was "posted" on the context
     * and then we feed it to the FSM
     *
     * @param event
     */
    protected void ensureAndFireEvent(final ScenarioMessage event) {
        verify(ctx).tell(event);
        fsm.onEvent(event);
    }

    /**
     * Ensure that the FSM is in the {@link ScenarioState#TERMINATED} state and that the actual
     * FSM is also marked as terminated.
     */
    protected void ensureFsmTerminated() {
        assertState(ScenarioState.TERMINATED);
        assertThat(fsm.isTerminated(), CoreMatchers.is(true));
    }

    /**
     * Oh I wish Java allowed for multiple returns but since it doesn't, a stupid holder it is. At least records
     * are making it easier these days.
     */
    protected record ExecHolder(ScenarioMessage.Exec execMsg, ActionResourceIdentifier sri, ActionJob job) {

    }

    protected record JobCompletesHolder(Session session, ActionMessage.ActionFinished msg, Execution execution) {

    }


    protected Session newSession(final String name) {
        return new Session(name);
    }

    protected InternalActionBuilder mockActionBuilder(final boolean isAsync) {
        return mockActionBuilder(1, isAsync).get(0);
    }

    /**
     * Helper method for mocking a bunch of action builders.
     *
     * @param count
     * @param isAsync
     * @return
     */
    protected List<InternalActionBuilder> mockActionBuilder(final int count, final boolean isAsync) {
        final List<InternalActionBuilder> actions = new ArrayList<>(count);

        for (int i = 0; i < count; ++i) {
            final var actionBuilder = mock(InternalActionBuilder.class);
            final var action = mock(Action.class);
            when(actionBuilder.isAsync()).thenReturn(isAsync);
            when(actionBuilder.isScenario()).thenReturn(false);
            when(actionBuilder.build(any(), any())).thenReturn(action);
            actions.add(actionBuilder);
        }

        return actions;
    }

    /**
     * Convenience method for creating a simple scenario with a single
     * async action.
     */
    protected Scenario newAsyncScenario(final String name) {
        final var asyncAction = mockActionBuilder(true);
        return newScenario(name, asyncAction);
    }

    protected Scenario newScenario(final String name) {
        final var builder = mockActionBuilder(false);
        return newScenario(name, builder);
    }

    protected Scenario newScenario(final String name, final List<InternalActionBuilder> actions) {
        var scenario = new Scenario(name);
        for (int i = 0; i < actions.size(); ++i) {
            // because Scenario is immutable so every change to it yields a new instance.
            final var action = actions.get(i);
            if (action.isAsync()) {
                scenario = scenario.executeAsync(action);
            } else {
                scenario = scenario.execute(action);
            }
        }
        return scenario;

    }

    protected Scenario newScenario(final String name, final InternalActionBuilder... actions) {
        return newScenario(name, List.of(actions));
    }

    private FSM<ScenarioState, ScenarioFsmContext, ScenarioData> newFsm() {
        // Note how we use the onTransition-method below just to get some logging as well.
        final var fsm = ScenarioFsm.definition.newInstance(uuid, ctx, data, this::onUnhandledEvent, this::onTransition);
        return fsm;
    }

    /**
     * Just a simple wrapper to get some logging. The "real deal" is really the mocked unhandled event handler, which
     * later in the tear down is checked for zero interactions.
     */
    public void onUnhandledEvent(final ScenarioState state, final Object event) {
        logWarn(new UnhandledEvent(), state, event);
        unhandledEventHandler.accept(state, event);
    }

    public void onTransition(final ScenarioState currentState, final ScenarioState toState, final Object event) {
        logInfo("{} -> {} Event: {}", currentState, toState, event);
        transitionListener.onTransition(currentState, toState, event);
    }

    /**
     * Convenience method that assumes that the mocked default {@link TransitionListener} is what is supposed
     * to be verified against.
     */
    protected void assertTransition(final ScenarioState from, final ScenarioState to) {
        assertTransition(transitionListener, from, to);
    }

    protected static void assertTransition(final TransitionListener<ScenarioState> listener, final ScenarioState from, final ScenarioState to) {
        assertTransition(listener, from, to, times(1));
    }

    protected static void assertTransition(final TransitionListener<ScenarioState> listener, final ScenarioState from
            , final ScenarioState to, final VerificationMode mode) {
        verify(listener, mode).onTransition(eq(from), eq(to), any());
    }

    public Execution someExecution(final boolean success) {
        return new Execution("Unit Test", success, List.of());
    }

    public ActionJob someJob(final ActionResourceIdentifier sri,
                             final boolean isAsync,
                             final Session session) {
        final var job = mock(ActionJob.class);
        when(job.sri()).thenReturn(sri);
        when(job.isAsync()).thenReturn(isAsync);
        when(job.session()).thenReturn(session);
        return job;
    }

    /**
     * Convenience method that assumes the FSM to check is the default FSM, which is a member
     * variable of the test base.
     */
    protected void assertState(final ScenarioState expectedState) {
        assertState(fsm, expectedState);
    }

    protected static void assertState(final FSM<ScenarioState, ScenarioFsmContext, ScenarioData> fsm, final ScenarioState expectedState) {
        assertThat(fsm.getState(), Matchers.is(expectedState));
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    private record UnhandledEvent() implements Alert {

        @Override
        public String getMessage() {
            return "{} Unhandled event {}";
        }

        @Override
        public int getCode() {
            return 0;
        }
    }


}
