package io.snice.testing.core.scenario.fsm;

import io.hektor.actors.LoggingSupport;
import io.hektor.fsm.FSM;
import io.hektor.fsm.TransitionListener;
import io.snice.identity.sri.ActionResourceIdentifier;
import io.snice.testing.core.Session;
import io.snice.testing.core.action.Action;
import io.snice.testing.core.scenario.InternalActionBuilder;
import io.snice.testing.core.scenario.Scenario;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.function.BiConsumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ScenarioFsmTestBase implements LoggingSupport {

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


    protected Session newSession(final String name) {
        return new Session(name);
    }

    protected InternalActionBuilder mockActionBuilder(final boolean isAsync) {
        final var actionBuilder = mock(InternalActionBuilder.class);
        final var action = mock(Action.class);
        when(actionBuilder.isAsync()).thenReturn(isAsync);
        when(actionBuilder.isScenario()).thenReturn(false);
        when(actionBuilder.build(any(), any())).thenReturn(action);
        return actionBuilder;
    }

    protected Scenario newScenario(final String name) {
        final var builder = mockActionBuilder(false);
        return newScenario(name, builder);
    }

    protected Scenario newScenario(final String name, final InternalActionBuilder... actions) {
        var scenario = new Scenario(name);
        for (int i = 0; i < actions.length; ++i) {
            // because Scenario is immutable so every change to it yields a new instance.
            final var action = actions[i];
            if (action.isAsync()) {
                scenario = scenario.executeAsync(actions[i]);
            } else {
                scenario = scenario.execute(actions[i]);
            }
        }
        return scenario;
    }

    private FSM<ScenarioState, ScenarioFsmContext, ScenarioData> newFsm() {
        // Note how we use the onTransition-method below just to get some logging as well.
        final var fsm = ScenarioFsm.definition.newInstance(uuid, ctx, data, unhandledEventHandler, this::onTransition);
        return fsm;
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
        Mockito.verify(listener).onTransition(eq(from), eq(to), any());
    }

    public ActionJob someJob() {
        final var job = mock(ActionJob.class);
        when(job.sri()).thenReturn(ActionResourceIdentifier.of());
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
        assertThat(fsm.getState(), is(expectedState));
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public Object getUUID() {
        return uuid;
    }


}
