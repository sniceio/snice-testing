package io.snice.testing.core.scenario.fsm;

import io.snice.testing.core.Session;
import io.snice.testing.core.action.Action;

public interface ActionMessage {

    record StartAction(Session session, Action job) {

    }
}
