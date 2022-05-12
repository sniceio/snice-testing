package io.snice.testing.core.action;

import io.snice.identity.sri.ActionResourceIdentifier;
import io.snice.testing.core.scenario.ScenarioContex;

public interface ActionBuilder {

    Action build(ActionResourceIdentifier sri, ScenarioContex ctx, Action next);
}
