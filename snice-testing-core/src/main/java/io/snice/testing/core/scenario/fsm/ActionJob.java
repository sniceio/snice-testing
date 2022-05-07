package io.snice.testing.core.scenario.fsm;

import io.snice.identity.sri.ActionResourceIdentifier;
import io.snice.testing.core.Session;
import io.snice.testing.core.action.Action;
import io.snice.testing.core.action.ActionBuilder;

/**
 *
 */
public interface ActionJob {

    /**
     * Whether this job is asynchronous or not.
     */
    boolean isAsync();

    ActionResourceIdentifier sri();

    /**
     * When a new {@link ActionJob} is created, the {@link Session} it will use when the {@link Action} is actually
     * being executed may be updated by the corresponding {@link ActionBuilder} and as such, not only does that version
     * of the {@link Session} (Remember a session is immutable) be given to the {@link Action} but we also need to use
     * this updated {@link Session} and pass that down the execution chain. This primarily applies to
     * asynchronous actions since a synchronous action will have "spit out" a new {@link Session} once it is done
     * executing, which we then will use to pass to the next {@link Action} in the execution chain.
     */
    Session session();

    void start();
}
