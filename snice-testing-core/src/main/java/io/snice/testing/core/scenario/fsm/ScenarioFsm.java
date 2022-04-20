package io.snice.testing.core.scenario.fsm;

import io.hektor.fsm.Definition;
import io.hektor.fsm.FSM;
import io.snice.functional.Either;
import io.snice.identity.sri.ActionResourceIdentifier;
import io.snice.testing.core.scenario.Scenario;

import java.util.List;

import static io.snice.testing.core.scenario.fsm.ScenarioState.ASYNC;
import static io.snice.testing.core.scenario.fsm.ScenarioState.EXEC;
import static io.snice.testing.core.scenario.fsm.ScenarioState.INIT;
import static io.snice.testing.core.scenario.fsm.ScenarioState.JOIN;
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
        final var async = builder.withTransientState(ASYNC);
        final var join = builder.withState(JOIN);
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
        // TODO: need to sit and wait in sync until the underlying ActionActor finishes or we timeout
        sync.transitionTo(EXEC).onEvent(String.class);

        async.transitionTo(EXEC).onEvent(ScenarioMessage.Exec.class).withGuard((msg, ctx, data) -> data.hasMoreActions());
        async.transitionTo(JOIN).asDefaultTransition().withAction(ScenarioFsm::onDefaultTransition);

        exec.transitionTo(TERMINATED)
                .onEvent(ScenarioMessage.Terminate.class)
                .withAction(e -> System.err.println("terminated"));

        // just so we can build the scenario
        join.transitionTo(TERMINATED).onEvent(String.class);

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

    /**
     * Whenever we are asked to execute an action (sync or async - doesn't matter), we will call this method.
     * The main purpose is to ask our context to prepare the execution of the action, which it does and is then
     * wrapped in a "job", which we need to keep track of. Each job has a unique SRI - {@link ActionResourceIdentifier}
     * and whenever a job is finished, we need to mark this job as done. The reason for this is a {@link Scenario}
     * will never exit until all jobs (execution steps within the scenario) has been completed (success or failure,
     * doesn't matter). This means that there will always be a JOIN at the end before we transition to TERMINATED.
     * There may not be any action steps left to "join" on but we will still transition through that step. Keeps the FSM
     * simpler and easier to test all possible paths.
     */
    private static void onExecute(final ScenarioMessage.Exec exec, final ScenarioFsmContext ctx, final ScenarioData data) {
        final var builder = exec.action();
        final var session = exec.session();
        final var job = ctx.prepareExecution(builder, session);
        data.storeActionJob(job);
        job.start();
    }

    /**
     * Whenever we enter the EXEC state we will fetch the next action and ask the {@link ScenarioFsmContext}
     * to execute it. As such, it is important that all state transitions that lead to the EXEC state are correct
     * in that there are still actions to be executed. This is not checked here (except of course you'll get an
     * exception). Unit tests should ensure all of this is true.
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


}
