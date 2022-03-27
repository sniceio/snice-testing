package io.snice.testing.core.action;

import io.snice.testing.core.Execution;
import io.snice.testing.core.Session;

import java.util.List;

public interface Action {

    /**
     * The name of this {@link Action}.
     */
    String name();

    /**
     * Execute this action.
     */
    void execute(List<Execution> executions, Session session);
}
