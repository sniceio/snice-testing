package io.snice.testing.core.scenario.fsm;

import io.hektor.core.Actor;
import io.hektor.core.LifecycleEvent;
import io.hektor.fsm.Definition;
import io.hektor.fsm.FSM;
import io.snice.functional.Either;
import io.snice.identity.sri.ActionResourceIdentifier;
import io.snice.testing.core.action.Action;
import io.snice.testing.core.scenario.Scenario;

import java.util.List;

import static io.snice.testing.core.scenario.fsm.ScenarioState.ASYNC;
import static io.snice.testing.core.scenario.fsm.ScenarioState.EXEC;
import static io.snice.testing.core.scenario.fsm.ScenarioState.INIT;
import static io.snice.testing.core.scenario.fsm.ScenarioState.JOIN;
import static io.snice.testing.core.scenario.fsm.ScenarioState.SYNC;
import static io.snice.testing.core.scenario.fsm.ScenarioState.TERMINATED;
import static io.snice.testing.core.scenario.fsm.ScenarioState.WRAP;

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
        final var wrap = builder.withState(WRAP);
        final var terminated = builder.withFinalState(TERMINATED);

        exec.withEnterAction(ScenarioFsm::onEnterExec);

        wrap.withEnterAction(ScenarioFsm::onEnterWrap);
        wrap.withSelfEnterAction(ScenarioFsm::onEnterWrap);

        init.transitionTo(INIT).onEvent(ScenarioMessage.Init.class).withAction(ScenarioFsm::onInit);

        init.transitionTo(EXEC).onEvent(ScenarioMessage.OkScenario.class);
        init.transitionTo(TERMINATED).onEvent(ScenarioMessage.BadScenario.class);


        exec.transitionTo(ASYNC)
                .onEvent(ScenarioMessage.Exec.class)
                .withGuard((msg, ctx, data) -> msg.action().isAsync())
                .withAction(ScenarioFsm::onExecute);

        exec.transitionTo(SYNC)
                .onEvent(ScenarioMessage.Exec.class)
                .withAction(ScenarioFsm::onExecute);

        exec.transitionTo(EXEC)
                .onEvent(ActionMessage.ActionFinished.class)
                .withAction(ScenarioFsm::onActionFinished);

        exec.transitionTo(EXEC).onEvent(LifecycleEvent.Terminated.class)
                .withAction(ScenarioFsm::onActorTerminated);

        exec.transitionTo(WRAP).onEvent(ScenarioMessage.NoMoreActions.class);

        sync.transitionTo(EXEC).onEvent(ActionMessage.ActionFinished.class)
                .withGuard(ScenarioFsm::isOutstandingSynchronousActionGuard)
                .withAction(ScenarioFsm::onActionFinished);

        // Note that this MUST be defined after the above check whether the
        // event is for a synchronous event or not. Or this will, since it is less restrictive (no guard),
        // starve out the other one.
        //
        // TODO: perhaps also should catch any unknown jobs. Shouldn't happen but perhaps we want
        // TODO: our FSM to handle it correctly. And also if it is a SYNC job but not the outstanding
        // TODO: one that we are waiting on.
        // TODO: Potential way to address them: unkown async job - refuse to process it. Transition via an "ERROR" state
        // TODO: of some kind just so that it is easy to see we took that route.
        // TODO: For parallel SYNC job, also refuse to start it (so different transition -> EXEC to SYNC) and also go via
        // TODO: the ERROR state.
        sync.transitionTo(SYNC).onEvent(ActionMessage.ActionFinished.class)
                .withAction(ScenarioFsm::onActionFinished);

        sync.transitionTo(SYNC).onEvent(LifecycleEvent.Terminated.class)
                .withAction(ScenarioFsm::onActorTerminated);

        async.transitionTo(EXEC).asDefaultTransition();

        wrap.transitionTo(WRAP).onEvent(ActionMessage.ActionFinished.class)
                .withAction(ScenarioFsm::onActionFinished);

        wrap.transitionTo(WRAP).onEvent(LifecycleEvent.Terminated.class)
                .withAction(ScenarioFsm::onActorTerminated);

        wrap.transitionTo(TERMINATED).onEvent(ScenarioMessage.Terminate.class);

        // TODO:
        join.transitionTo(EXEC).onEvent(String.class);

        definition = builder.build();
    }

    /**
     * While in the {@link ScenarioState#SYNC} state and we recieve an {@link ActionMessage.ActionFinished} message, we
     * have to ensure that it is indeed for the "job" we are waiting to finish. Remember, there could be asynchyronous
     * actions executing in parallel to the syncronous single job we are waiting for. As such, when transitioning
     * away from the {@link ScenarioState#SYNC} back to the {@link ScenarioState#EXEC} phase we need to ensure
     * we only do so when our synchronous action is finished.
     */
    private static boolean isOutstandingSynchronousActionGuard(final ActionMessage.ActionFinished msg, final ScenarioFsmContext ctx, final ScenarioData data) {
        return data.isTheOutstandingSynchronousAction(msg.sri());
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

    private static void onActionFinished(final ActionMessage.ActionFinished msg, final ScenarioFsmContext ctx, final ScenarioData data) {
        data.processActionFinished(msg);
    }

    /**
     * Not that this FSM actually cares but any {@link Action}s are executed within an {@link Actor} and when that
     * actor terminates, the action is completely done and we take note of that (to ensure we don't leak Actors etc)
     */
    private static void onActorTerminated(final LifecycleEvent.Terminated msg, final ScenarioFsmContext ctx, final ScenarioData data) {
        final var sri = ActionResourceIdentifier.from(msg.getActor().name());
        data.processActionTerminated(sri);
    }

    /**
     * Whenever we enter the EXEC state we will fetch the next action and ask the {@link ScenarioFsmContext}
     * to execute it. As such, it is important that all state transitions that lead to the EXEC state are correct
     * in that there are still actions to be executed. This is not checked here (except of course you'll get an
     * exception). Unit tests should ensure all of this is true.
     */
    private static void onEnterExec(final ScenarioFsmContext ctx, final ScenarioData data) {
        if (data.hasMoreActions()) {
            final var action = data.nextAction();
            final var session = data.session();
            ctx.tell(new ScenarioMessage.Exec(action, session));
        } else {
            ctx.tell(new ScenarioMessage.NoMoreActions());
        }
    }

    /**
     * Whenever we enter the {@link ScenarioState#WRAP} state we will check if there actually are things
     * to "wrap up". If all outstanding actions have finished and the underlying actor has terminated, we
     * are all good. Once that happens, we will process all the executions and results etc and "compute" a
     * final verdict.
     */
    private static void onEnterWrap(final ScenarioFsmContext ctx, final ScenarioData data) {
        if (data.isAllActionsDone()) {
            ctx.tell(new ScenarioMessage.Terminate());
        }
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
