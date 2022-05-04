package io.snice.testing.core.scenario.fsm;

import io.hektor.fsm.Definition;
import io.hektor.fsm.FSM;

import java.util.List;

import static io.snice.testing.core.scenario.fsm.ActionState.EXEC;
import static io.snice.testing.core.scenario.fsm.ActionState.INIT;
import static io.snice.testing.core.scenario.fsm.ActionState.TERMINATED;


public class ActionFsm {

    public static final Definition<ActionState, ActionContext, ActionData> definition;

    static {

        final var builder = FSM.of(ActionState.class).ofContextType(ActionContext.class).withDataType(ActionData.class);

        final var init = builder.withInitialState(INIT);
        final var exec = builder.withState(EXEC);
        final var terminated = builder.withFinalState(TERMINATED);

        init.transitionTo(EXEC).onEvent(ActionMessage.StartAction.class).withAction(ActionFsm::onStartAction);

        exec.transitionTo(TERMINATED).onEvent(ActionMessage.ActionFinished.class).withAction(ActionFsm::onActionFinished);

        definition = builder.build();
    }

    private static void onStartAction(final ActionMessage.StartAction start, final ActionContext ctx, final ActionData data) {
        try {
            final var session = start.session();
            start.action().execute(List.of(), session);
        } catch (final Throwable t) {
            // TODO handle it.
        }
    }

    private static void onActionFinished(final ActionMessage.ActionFinished finished, final ActionContext ctx, final ActionData data) {
        ctx.actionFinished(finished);
    }


}
