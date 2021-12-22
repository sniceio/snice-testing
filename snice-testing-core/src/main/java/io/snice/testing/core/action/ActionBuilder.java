package io.snice.testing.core.action;

import io.snice.testing.core.scenario.ScenarioContex;

public interface ActionBuilder {

    Action build(ScenarioContex ctx, Action next);
}
