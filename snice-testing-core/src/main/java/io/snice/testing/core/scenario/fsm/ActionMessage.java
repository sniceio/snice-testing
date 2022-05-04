package io.snice.testing.core.scenario.fsm;

import io.snice.identity.sri.ActionResourceIdentifier;
import io.snice.testing.core.Execution;
import io.snice.testing.core.Session;
import io.snice.testing.core.action.Action;

import java.util.List;

public interface ActionMessage {

    /**
     * Message used to kick-off the execution of an {@link Action} within the {@link ActionFsm}.
     */
    record StartAction(Session session, Action action) {
    }

    /**
     * Message to signal that an action has finished executing.
     */
    record ActionFinished(ActionResourceIdentifier sri, Session session, List<Execution> executions) {
    }
}
