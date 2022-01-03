package io.snice.testing.core.scenario.fsm;

import io.hektor.fsm.Data;
import io.snice.testing.core.action.ActionBuilder;
import io.snice.testing.core.scenario.Scenario;

public class ScenarioData implements Data {

    private final Scenario scenario;
    private int actionIndex = 0;

    public ScenarioData(final Scenario scenario) {
        this.scenario = scenario;
    }


    public boolean hasMoreActions() {
        return actionIndex < scenario.actions().size();
    }

    public ActionBuilder nextAction() {
        return scenario.actions().get(actionIndex++);
    }
}
