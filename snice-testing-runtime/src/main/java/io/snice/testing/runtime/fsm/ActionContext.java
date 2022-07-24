package io.snice.testing.runtime.fsm;

import io.hektor.fsm.Context;

public interface ActionContext extends Context {

    void actionFinished(ActionMessage.ActionFinished msg);
}
