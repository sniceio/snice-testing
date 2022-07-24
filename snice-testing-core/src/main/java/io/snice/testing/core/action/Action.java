package io.snice.testing.core.action;

import io.snice.identity.sri.ActionResourceIdentifier;
import io.snice.testing.core.Execution;
import io.snice.testing.core.Session;

import java.util.List;
import java.util.Map;

public interface Action {

    /**
     * The name of this {@link Action}.
     */
    String name();

    /**
     * Every {@link Action} has a unique resource identifier associated with it.
     *
     * @return
     */
    ActionResourceIdentifier sri();

    /**
     * An {@link Action} may have attributes that should be updated on the {@link Session} before the
     * action is actually executed. The execution environment will, as soon as the {@link Action} has been built,
     * update the {@link Session} with these attributes so that e.g. an async action that listens to a particular
     * address, that address is made available to other actions after it (needed to make webhooks work e.g.)
     *
     * @return
     */
    default Map<String, Object> attributes() {
        return Map.of();
    }

    /**
     * Execute this action.
     */
    void execute(List<Execution> executions, Session session);
}
