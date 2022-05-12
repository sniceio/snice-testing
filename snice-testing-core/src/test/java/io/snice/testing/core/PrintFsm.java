package io.snice.testing.core;

import io.hektor.fsm.visitor.PlantUmlVisitor;
import io.snice.testing.core.scenario.fsm.ScenarioFsm;
import org.junit.jupiter.api.Test;

/**
 * Not really a test case but until we've corporated documentation into the build process, use this to manually
 * generate uml state diagram and copy/paste the output to https://www.plantuml.com/plantuml/uml/
 */
public class PrintFsm {

    @Test
    public void printScenarioFsm() {
        final var visitor = new PlantUmlVisitor();
        visitor.start();
        ScenarioFsm.definition.acceptVisitor(visitor);
        visitor.end();

    }
}
