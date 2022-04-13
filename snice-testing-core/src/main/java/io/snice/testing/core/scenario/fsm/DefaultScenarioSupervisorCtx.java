package io.snice.testing.core.scenario.fsm;

import io.hektor.actors.fsm.FsmActor;
import io.hektor.actors.fsm.FsmActorContextSupport;
import io.hektor.actors.fsm.OnStartFunction;
import io.hektor.core.ActorRef;
import io.hektor.core.Props;
import io.snice.testing.core.Session;
import io.snice.testing.core.scenario.Scenario;
import io.snice.testing.core.scenario.ScenarioContex;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static io.snice.preconditions.PreConditions.assertNotNull;

public class DefaultScenarioSupervisorCtx implements ScenarioSupervisorCtx, FsmActorContextSupport {

    private final ActorRef self;
    private final CountDownLatch latch;

    public static ScenarioSupervisorCtx of(final ActorRef self, final CountDownLatch latch) {
        assertNotNull(self);
        assertNotNull(latch);
        return new DefaultScenarioSupervisorCtx(self, latch);
    }

    private DefaultScenarioSupervisorCtx(final ActorRef self, final CountDownLatch latch) {
        this.self = self;
        this.latch = latch;
    }

    @Override
    public void runScenario(final Session session, final Scenario scenario, final ScenarioContex ctx) {
        final var props = configureScenarioFsm(session, scenario, ctx);
        final var scnActor = ctx().actorOf(scenario.uuid().asString(), props);
    }


    private Props configureScenarioFsm(final Session session, final Scenario scenario, final ScenarioContex scenarioContext) {
        final var scenarioData = new ScenarioData(scenario);
        final OnStartFunction<ScenarioFsmContext, ScenarioData> onStart = (actorCtx, ctx, data) -> {
            actorCtx.self().tell(new ScenarioMessage.Exec(List.of(), session));
        };

        return FsmActor.of(ScenarioFsm.definition)
                .withContext(ref -> new DefaultScenarioFsmContext(self, ref, scenario, scenarioContext))
                .withData(scenarioData)
                .withStartFunction(onStart)
                .build();
    }

    /**
     * Called by the FSM when we enter the {@link ScenarioSupervisorState#RUNNING}
     */
    @Override
    public void isRunning() {

    }


}
