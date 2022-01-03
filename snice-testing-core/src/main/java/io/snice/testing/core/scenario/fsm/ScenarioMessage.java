package io.snice.testing.core.scenario.fsm;

import io.snice.testing.core.Session;

public interface ScenarioMessage {

    record Init() {
    }

    record Exec(Session session) {
    }

    record Terminate() {
    }
}
