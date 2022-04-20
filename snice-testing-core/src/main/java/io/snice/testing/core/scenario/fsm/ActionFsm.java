package io.snice.testing.core.scenario.fsm;

import io.hektor.fsm.Definition;
import io.hektor.fsm.FSM;

import static io.snice.testing.core.scenario.fsm.ActionState.INIT;
import static io.snice.testing.core.scenario.fsm.ActionState.TERMINATED;


public class ActionFsm {

    public static final Definition<ActionState, ActionContext, ActionData> definition;

    static {

        final var builder = FSM.of(ActionState.class).ofContextType(ActionContext.class).withDataType(ActionData.class);

        final var init = builder.withInitialState(INIT);
        // final var exec = builder.withState(EXEC);
        final var terminated = builder.withFinalState(TERMINATED);

        init.transitionTo(TERMINATED).onEvent(String.class);

        definition = builder.build();
    }
}
