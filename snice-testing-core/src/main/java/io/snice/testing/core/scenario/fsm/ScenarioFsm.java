package io.snice.testing.core.scenario.fsm;

import io.hektor.fsm.Definition;
import io.hektor.fsm.FSM;
import io.snice.testing.core.Execution;
import io.snice.testing.core.Session;
import io.snice.testing.core.action.Action;

import java.util.List;

import static io.snice.testing.core.scenario.fsm.ScenarioState.EXEC;
import static io.snice.testing.core.scenario.fsm.ScenarioState.INIT;
import static io.snice.testing.core.scenario.fsm.ScenarioState.READY;
import static io.snice.testing.core.scenario.fsm.ScenarioState.TERMINATED;

public class ScenarioFsm {

    public static final Definition<ScenarioState, ScenarioFsmContext, ScenarioData> definition;

    static {
        final var builder = FSM.of(ScenarioState.class).ofContextType(ScenarioFsmContext.class).withDataType(ScenarioData.class);

        final var init = builder.withInitialState(INIT);
        final var ready = builder.withTransientState(READY);
        final var exec = builder.withState(EXEC);
        final var terminated = builder.withFinalState(TERMINATED);

        init.transitionTo(READY).onEvent(ScenarioMessage.Exec.class);

        ready.transitionTo(EXEC)
                .onEvent(ScenarioMessage.Exec.class)
                .withGuard((msg, ctx, data) -> data.hasMoreActions())
                .withAction(ScenarioFsm::onExecuteNextAction);

        ready.transitionTo(TERMINATED)
                .onEvent(ScenarioMessage.Exec.class)
                .withAction(ScenarioFsm::onNoMoreActions);

        ready.transitionTo(TERMINATED).asDefaultTransition().withAction(ScenarioFsm::onDefaultTransition);

        exec.transitionTo(READY).onEvent(ScenarioMessage.Exec.class);

        exec.transitionTo(TERMINATED)
                .onEvent(ScenarioMessage.Terminate.class)
                .withAction(e -> System.err.println("terminated"));

        definition = builder.build();
    }

    /**
     * Every transient state, which the {@link ScenarioState#READY} state is, must specify a default transition
     * in case nothing else matches. However, in our case, if we ever take the default transition there is a bug
     * in our state machine definitions. We must have missed something so log it on warning and terminate.
     *
     * @param msg
     * @param ctx
     * @param data
     */
    private static void onDefaultTransition(final Object msg, final ScenarioFsmContext ctx, final ScenarioData data) {
        // TODO: this is an error. Deal with it.
    }

    private static void onNoMoreActions(final ScenarioMessage.Exec exec, final ScenarioFsmContext ctx, final ScenarioData data) {
        ctx.processFinalResult(exec.executions(), exec.session());
    }

    private static void onExecuteNextAction(final ScenarioMessage.Exec exec, final ScenarioFsmContext ctx, final ScenarioData data) {
        try {
            // TODO: do we simply rely on the action always taking place on a thread pool
            // on its own?
            final var nextAction = new NextAction("NextAction", ctx);
            final var action = data.nextAction().build(ctx.scenarioContext(), nextAction);
            action.execute(exec.executions(), exec.session());
        } catch (final Throwable t) {
            // TODO: do something about it.
            t.printStackTrace();
        }
    }

    private static record NextAction(String name, ScenarioFsmContext ctx) implements Action {

        @Override
        public void execute(final List<Execution> executions, final Session session) {
            ctx.processActionResult(executions, session);
        }
    }

}
