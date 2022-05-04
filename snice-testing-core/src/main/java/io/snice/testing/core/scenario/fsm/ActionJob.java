package io.snice.testing.core.scenario.fsm;

import io.snice.identity.sri.ActionResourceIdentifier;

/**
 *
 */
public interface ActionJob {

    /**
     * Whether this job is asynchronous or not.
     */
    boolean isAsync();

    ActionResourceIdentifier sri();

    void start();
}
