package io.snice.testing.core.scenario;

import io.snice.identity.sri.ActionResourceIdentifier;
import io.snice.testing.core.Session;
import io.snice.testing.core.action.Action;
import io.snice.testing.core.action.ActionBuilder;

/**
 * Offering a more rich internal version of the {@link ActionBuilder} since we often
 * also need to know whether this is an async action, a sub-scenario etc etc.
 */
public interface InternalActionBuilder extends ActionBuilder {

    /**
     * The {@link ActionBuilder} is immutable so when we create a new such builder we will also
     * allocate the {@link ActionResourceIdentifier}, which eventually will be the unique identifier
     * for the actual {@link Action} that we'll be executing.
     */
    ActionResourceIdentifier sri();

    /**
     * If true, then this {@link Action} should be executed as an asynchronous action.
     */
    boolean isAsync();

    /**
     * If true, then this {@link Action} is an entire {@link Session}.
     */
    boolean isScenario();

    Action build(ScenarioContex ctx, Action next);
}
