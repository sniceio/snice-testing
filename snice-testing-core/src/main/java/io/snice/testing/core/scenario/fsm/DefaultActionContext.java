package io.snice.testing.core.scenario.fsm;

import io.hektor.actors.fsm.FsmActorContextSupport;
import io.hektor.core.ActorRef;

public record DefaultActionContext(ActorRef parent) implements ActionContext, FsmActorContextSupport {

    @Override
    public void actionFinished(final ActionMessage.ActionFinished msg) {
        parent.tell(msg);
    }
}
