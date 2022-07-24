package io.snice.testing.core.scenario.fsm;

import io.snice.testing.core.scenario.Scenario;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.snice.testing.core.scenario.fsm.ScenarioState.INIT;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Test all events that INIT handles.
 */
class ScenarioFsmInitTest extends ScenarioFsmTestBase {

    @Test
    public void testInit() {
        final var scenario = newScenario("Init");
        fsm.onEvent(new ScenarioMessage.Init(newSession("Unit Test"), scenario));
        assertState(INIT);
        assertTransition(INIT, INIT);

        verify(ctx).tell(new ScenarioMessage.OkScenario());
        verifyNoMoreInteractions(ctx);
    }

    @Test
    public void testInitBadScenario() {
        fsm.onEvent(new ScenarioMessage.Init(newSession("Unit Test"), new Scenario("No Actions")));
        assertState(INIT);
        assertTransition(INIT, INIT);

        verify(ctx).tell(new ScenarioMessage.BadScenario(List.of("No actions to execute")));
        verifyNoMoreInteractions(ctx);
    }

}