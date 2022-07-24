package io.snice.testing.runtime.fsm;

import io.hektor.core.Actor;
import io.hektor.core.LifecycleEvent;
import io.hektor.fsm.Definition;
import io.hektor.fsm.FSM;
import io.hektor.fsm.visitor.PlantUmlVisitor;
import io.snice.functional.Either;
import io.snice.identity.sri.ActionResourceIdentifier;
import io.snice.testing.core.action.Action;
import io.snice.testing.core.scenario.Scenario;

import java.util.List;

import static io.hektor.fsm.docs.Label.label;

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

    /**
     * All of the various "labels" are only for documentation purposes. Hektor.io FSM allows you to
     * put labels on actions, guards, transition etc which then e.g. the {@link PlantUmlVisitor} will
     * use to generate a more human friendly state diagram. These labels have no impact on the
     * running FSM in any way whatsoever.
     */
    private static final String PREPARE_AND_START_JOB_LABEL = "Prepare and Start Action";
    private static final String ACTION_FINISHED_LABEL = "Process Action Finished";
    private static final String ACTOR_TERMINATED_LABEL = "Process Action Actor Terminated";


    static {
        final var builder = FSM.of(ScenarioState.class).ofContextType(ScenarioFsmContext.class).withDataType(ScenarioData.class);

        final var init = builder.withInitialState(ScenarioState.INIT);
        final var exec = builder.withState(ScenarioState.EXEC);
        final var sync = builder.withState(ScenarioState.SYNC);
        final var async = builder.withTransientState(ScenarioState.ASYNC);
        final var join = builder.withState(ScenarioState.JOIN);
        final var wrap = builder.withState(ScenarioState.WRAP);
        final var error = builder.withTransientState(ScenarioState.ERROR);
        final var terminated = builder.withFinalState(ScenarioState.TERMINATED);

        exec.withEnterAction(ScenarioFsm::onEnterExec, label("Execute Next Action"));

        wrap.withEnterAction(ScenarioFsm::onEnterWrap, label("Outstanding actions?"));
        wrap.withSelfEnterAction(ScenarioFsm::onEnterWrap, label("Outstanding actions?"));

        init.transitionTo(ScenarioState.INIT).onEvent(ScenarioMessage.Init.class).withAction(ScenarioFsm::onInit, label("Validate Scenario"));

        init.transitionTo(ScenarioState.EXEC).onEvent(ScenarioMessage.OkScenario.class);
        init.transitionTo(ScenarioState.TERMINATED).onEvent(ScenarioMessage.BadScenario.class);


        exec.transitionTo(ScenarioState.ASYNC)
                .onEvent(ScenarioMessage.Exec.class)
                .withGuard((msg, ctx, data) -> msg.action().isAsync(), label("isAsync Action"))
                .withAction(ScenarioFsm::onExecute, label(PREPARE_AND_START_JOB_LABEL));

        exec.transitionTo(ScenarioState.SYNC)
                .onEvent(ScenarioMessage.Exec.class)
                .withAction(ScenarioFsm::onExecute, label(PREPARE_AND_START_JOB_LABEL));

        exec.transitionTo(ScenarioState.EXEC)
                .onEvent(ActionMessage.ActionFinished.class)
                .withAction(ScenarioFsm::onActionFinished, label(ACTION_FINISHED_LABEL));

        exec.transitionTo(ScenarioState.EXEC).onEvent(LifecycleEvent.Terminated.class)
                .withAction(ScenarioFsm::onActorTerminated, label(ACTOR_TERMINATED_LABEL));

        exec.transitionTo(ScenarioState.WRAP).onEvent(ScenarioMessage.NoMoreActions.class);

        sync.transitionTo(ScenarioState.ERROR).onEvent(ActionMessage.ActionFinished.class)
                .withGuard(ScenarioFsm::isUnknownAction, label("Unknown Action"))
                .withTransformation(actionFinished -> ScenarioState.SYNC.toString(), label("evt -> \"SYNC\""))
                .withAction((evt, ctx, data) -> onUnknownJob(ScenarioState.SYNC, evt, ctx), label("Process unknown action"));

        sync.transitionTo(ScenarioState.EXEC).onEvent(ActionMessage.ActionFinished.class)
                .withGuard(ScenarioFsm::isOutstandingSynchronousActionGuard, label("isOutstandingSyncAction"))
                .withAction(ScenarioFsm::onSyncActionFinished, label("Process sync action finished"));

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
        sync.transitionTo(ScenarioState.SYNC).onEvent(ActionMessage.ActionFinished.class)
                .withAction(ScenarioFsm::onActionFinished, label(ACTION_FINISHED_LABEL));

        sync.transitionTo(ScenarioState.SYNC).onEvent(LifecycleEvent.Terminated.class)
                .withAction(ScenarioFsm::onActorTerminated, label(ACTOR_TERMINATED_LABEL));

        async.transitionTo(ScenarioState.EXEC).asDefaultTransition();

        wrap.transitionTo(ScenarioState.WRAP).onEvent(ActionMessage.ActionFinished.class)
                .withAction(ScenarioFsm::onActionFinished, label(ACTION_FINISHED_LABEL));

        wrap.transitionTo(ScenarioState.WRAP).onEvent(LifecycleEvent.Terminated.class)
                .withAction(ScenarioFsm::onActorTerminated, label(ACTOR_TERMINATED_LABEL));

        wrap.transitionTo(ScenarioState.TERMINATED).onEvent(ScenarioMessage.Terminate.class);

        // The ERROR state is a transient state whose only purpose is to show in the state transitions
        // that an error occurred. It will always transition back to the state from where it came, which
        // is managed by the original state doing a transformation of the original message to a string
        // which we'll map on below.
        error.transitionTo(ScenarioState.SYNC).onEvent(String.class).withGuard(ScenarioState.SYNC.toString()::equals, label("s == \"SYNC\""));
        error.transitionTo(ScenarioState.EXEC).asDefaultTransition();

        // TODO:
        join.transitionTo(ScenarioState.EXEC).onEvent(String.class);

        definition = builder.build();
    }

    /**
     * While in the {@link ScenarioState#SYNC} state and we receive an {@link ActionMessage.ActionFinished} message, we
     * have to ensure that it is indeed for the "job" we are waiting to finish. Remember, there could be asynchronous
     * actions executing in parallel to the synchronous single job we are waiting for. As such, when transitioning
     * away from the {@link ScenarioState#SYNC} back to the {@link ScenarioState#EXEC} phase we need to ensure
     * we only do so when our synchronous action is finished.
     */
    private static boolean isOutstandingSynchronousActionGuard(final ActionMessage.ActionFinished msg, final ScenarioFsmContext ctx, final ScenarioData data) {
        return data.isTheOutstandingSynchronousAction(msg.sri());
    }

    private static boolean isUnknownAction(final ActionMessage.ActionFinished msg, final ScenarioFsmContext ctx, final ScenarioData data) {
        return !data.isKnownJob(msg.sri());
    }

    private static void onUnknownJob(final ScenarioState state, final ActionMessage.ActionFinished msg, final ScenarioFsmContext ctx) {
        final var error = new ScenarioMessage.ErrorAction(state, msg);
        ctx.reportError(error);
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

        // Note that we need to make the potential updated session the "latest and greatest" in case the Action
        // we are about to execute is an async action and as such, the ActionBuilder may have updated the session
        // with the FQDN of where the listening point is.
        data.session(job.session());
        job.start();
    }

    /**
     * The only difference between a synchronous job finishing and an async one is that there can only be a single
     * synchronous job running at any given point in time and there is a guard on the FSM that ensures this. As such,
     * we also need to clear out the outstanding synchronous job since it just completed (duh).
     * <p>
     * Note: we RELY on the FSM setup properly so there is no additional check to see if the {@link ActionMessage.ActionFinished}
     * message is indeed for the outstanding action. If we mess up we'll have an issue but that's why we have unit tests...
     */
    private static void onSyncActionFinished(final ActionMessage.ActionFinished msg, final ScenarioFsmContext ctx, final ScenarioData data) {
        data.clearOutstandingSynchronousJob();
        onActionFinished(msg, ctx, data);
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
     * to execute it. If there are no more actions to execute, we'll simple issue a message
     * stating as much, which will allow us to transition to the next state.
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
