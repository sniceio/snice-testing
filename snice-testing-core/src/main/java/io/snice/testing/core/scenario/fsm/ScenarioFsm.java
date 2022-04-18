package io.snice.testing.core.scenario.fsm;

import io.hektor.fsm.Definition;
import io.hektor.fsm.FSM;
import io.snice.functional.Either;
import io.snice.testing.core.Execution;
import io.snice.testing.core.Session;
import io.snice.testing.core.action.Action;
import io.snice.testing.core.scenario.Scenario;

import java.util.List;

import static io.snice.testing.core.scenario.fsm.ScenarioState.ASYNC;
import static io.snice.testing.core.scenario.fsm.ScenarioState.EXEC;
import static io.snice.testing.core.scenario.fsm.ScenarioState.INIT;
import static io.snice.testing.core.scenario.fsm.ScenarioState.SYNC;
import static io.snice.testing.core.scenario.fsm.ScenarioState.TERMINATED;

/**
 * Implements the following FSM (plantuml, just to save it with the code, copy paste to plantuml.com)
 * <code>
 *
 * @startuml hide empty description
 * state INIT
 * state EXEC : On Enter/NextAction
 * state SYNC
 * state ASYNC
 * state JOIN
 * state TERMINATED
 * <p>
 * [*] --> INIT : ScenarioMessage::Init
 * INIT --> TERMINATED : ScenarioMessage::BadScenario
 * INIT --> EXEC : ScenarioMessage::OkScenario
 * <p>
 * EXEC --> EXEC :  [isAsync]
 * EXEC --> SYNC : Start New Action [isSync]
 * SYNC--> EXEC
 * <p>
 * EXEC --> ASYNC : Start New Action [isAsync]
 * ASYNC --> EXEC
 * <p>
 * EXEC --> JOIN : Join Action
 * JOIN --> JOIN : Action Terminated\n[moreActionsWaiting]
 * JOIN --> EXEC : Action Terminated\n[noMoreActionsWaiting]
 * EXEC --> TERMINATED : [noMoreActions]
 * TERMINATED --> [*]
 * @enduml </code>
 */
public class ScenarioFsm {

    public static final Definition<ScenarioState, ScenarioFsmContext, ScenarioData> definition;

    static {
        final var builder = FSM.of(ScenarioState.class).ofContextType(ScenarioFsmContext.class).withDataType(ScenarioData.class);

        final var init = builder.withInitialState(INIT);
        final var exec = builder.withState(EXEC);
        final var sync = builder.withState(SYNC);
        final var async = builder.withState(ASYNC);
        // final var join = builder.withState(JOIN);
        final var terminated = builder.withFinalState(TERMINATED);

        init.transitionTo(INIT).onEvent(ScenarioMessage.Init.class).withAction(ScenarioFsm::onInit);

        init.transitionTo(EXEC).onEvent(ScenarioMessage.OkScenario.class);
        init.transitionTo(TERMINATED).onEvent(ScenarioMessage.BadScenario.class);

        exec.withEnterAction(ScenarioFsm::onEnterExec);

        exec.transitionTo(ASYNC)
                .onEvent(ScenarioMessage.Exec.class)
                .withGuard((msg, ctx, data) -> msg.action().isAsync())
                .withAction(ScenarioFsm::onExecute);

        exec.transitionTo(SYNC)
                .onEvent(ScenarioMessage.Exec.class)
                .withAction(ScenarioFsm::onExecute);

        // TODO: just so that we can build the scenario
        sync.transitionTo(EXEC).onEvent(String.class);

        async.transitionTo(EXEC).asDefaultTransition().withAction(ScenarioFsm::onDefaultTransition);

        exec.transitionTo(TERMINATED)
                .onEvent(ScenarioMessage.Terminate.class)
                .withAction(e -> System.err.println("terminated"));

        definition = builder.build();
    }

    private static void onInit(final ScenarioMessage.Init init, final ScenarioFsmContext ctx, final ScenarioData data) {
        data.session(init.session());

        final var result = validateScenario(init.scenario());
        final var msg = result.fold(errors ->
                        new ScenarioMessage.BadScenario(errors),
                scenario -> {
                    data.scenario(scenario);
                    return new ScenarioMessage.OkScenario();
                });
        ctx.tell(msg);
    }

    private static Either<List<String>, Scenario> validateScenario(final Scenario scenario) {
        final var result = scenario.validate();
        if (result.isLeft()) {
            return result;
        }

        // note that for a {@link Scenario}, no actions is actually valid but for the execution
        // environment it is not.
        if (scenario.actions().isEmpty()) {
            return Either.left(List.of("No actions to execute"));
        }

        return Either.right(scenario);
    }

    private static void onExecute(final ScenarioMessage.Exec exec, final ScenarioFsmContext ctx, final ScenarioData data) {
        System.err.println("OnExecute: need to start new Action FSM. Async: " + exec.action().isAsync());
        // action.execute(exec.executions(), exec.session());

        // if async action, store async UUID. Perhaps we will do this alwyas? Even for sync it's just that it gets
        // updated on that transition from SYNC back to EXEC? Seems better.
    }

    /**
     * Whenever we enter the EXEC state we will fetch the next action and ask the {@link ScenarioFsmContext}
     * to execute it.
     *
     * @param ctx
     * @param data
     */
    private static void onEnterExec(final ScenarioFsmContext ctx, final ScenarioData data) {
        final var action = data.nextAction();
        final var session = data.session();

        ctx.tell(new ScenarioMessage.Exec(action, session));
    }

    /**
     * Every transient state, which the {@link ScenarioState#EXEC} state is, must specify a default transition
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
        // ctx.processFinalResult(exec.executions(), exec.session());
    }

    /**
     * This is how we "trap" the action of handing control back to us. Each {@link Action} is unaware of the
     * actual execution environment and just calls "nextAction.execute", which is why we insert this "fake" action
     * as the next action to execute.
     */
    private static record NextAction(String name, ScenarioFsmContext ctx) implements Action {

        @Override
        public void execute(final List<Execution> executions, final Session session) {
            ctx.processActionResult(executions, session);
        }
    }

}
