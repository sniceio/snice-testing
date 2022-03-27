package io.snice.testing.core.scenario.fsm;

import io.snice.testing.core.Execution;
import io.snice.testing.core.Session;

import java.util.List;

public interface ScenarioMessage {

    record Init() {
    }

    record Exec(List<Execution> executions, Session session) {
    }

    record Terminate() {
    }
}
