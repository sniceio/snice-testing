package io.snice.testing.core.scenario.fsm;

import io.snice.testing.core.Session;
import io.snice.testing.core.scenario.Scenario;
import io.snice.testing.core.scenario.ScenarioContex;

public interface ScenarioSupervisorMessages {

    record Init() {
    }

    record Run(Scenario scenario, Session session, ScenarioContex ctx) {
    }

    record Terminate() {
    }
}
