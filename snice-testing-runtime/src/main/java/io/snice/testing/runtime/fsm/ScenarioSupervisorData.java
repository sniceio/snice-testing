package io.snice.testing.runtime.fsm;

import io.hektor.fsm.Data;
import io.snice.identity.sri.ScenarioResourceIdentifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ScenarioSupervisorData implements Data {

    private final Map<ScenarioResourceIdentifier, ScenarioSupervisorMessages.Run> currentRuns = new HashMap<>();

    public void storeRun(final ScenarioSupervisorMessages.Run run) {
        currentRuns.put(run.scenario().uuid(), run);
    }

    public Optional<ScenarioSupervisorMessages.Run> removeRun(final ScenarioResourceIdentifier sri) {
        return Optional.ofNullable(currentRuns.get(sri));
    }
}
