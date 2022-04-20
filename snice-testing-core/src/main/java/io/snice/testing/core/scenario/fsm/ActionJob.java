package io.snice.testing.core.scenario.fsm;

import io.snice.identity.sri.ActionResourceIdentifier;

/**
 *
 */
public interface ActionJob {

    ActionResourceIdentifier sri();

    void start();
}
