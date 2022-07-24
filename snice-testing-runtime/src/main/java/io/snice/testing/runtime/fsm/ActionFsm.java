package io.snice.testing.runtime.fsm;

import io.hektor.fsm.Definition;
import io.hektor.fsm.FSM;

import java.util.List;


public class ActionFsm {

    public static final Definition<ActionState, ActionContext, ActionData> definition;

    static {

        final var builder = FSM.of(ActionState.class).ofContextType(ActionContext.class).withDataType(ActionData.class);

        final var init = builder.withInitialState(ActionState.INIT);
        final var exec = builder.withState(ActionState.EXEC);
        final var terminated = builder.withFinalState(ActionState.TERMINATED);

        init.transitionTo(ActionState.EXEC).onEvent(ActionMessage.StartAction.class).withAction(ActionFsm::onStartAction);

        exec.transitionTo(ActionState.TERMINATED).onEvent(ActionMessage.ActionFinished.class).withAction(ActionFsm::onActionFinished);

        definition = builder.build();
    }

    private static void onStartAction(final ActionMessage.StartAction start, final ActionContext ctx, final ActionData data) {
        try {
            final var session = start.session();
            start.action().execute(List.of(), session);
        } catch (final Throwable t) {
            // TODO handle it.
            t.printStackTrace();
        }
    }

    private static void onActionFinished(final ActionMessage.ActionFinished finished, final ActionContext ctx, final ActionData data) {
        ctx.actionFinished(finished);
    }


}
