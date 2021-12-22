package io.snice.testing.core.action;

import io.snice.testing.core.Session;

public interface Action {

    /**
     * The name of this {@link Action}.
     */
    String name();

    /**
     * Execute this action.
     */
    void execute(Session session);
}
