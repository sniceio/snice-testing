package io.snice.testing.runtime.fsm;

import io.hektor.actors.fsm.FsmActor;
import io.hektor.actors.fsm.FsmActorContextSupport;
import io.hektor.actors.fsm.OnStartFunction;
import io.hektor.core.ActorRef;
import io.hektor.core.Props;
import io.snice.testing.core.Session;
import io.snice.testing.core.protocol.ProtocolRegistry;
import io.snice.testing.core.scenario.DefaultScenarioContext;
import io.snice.testing.core.scenario.Scenario;

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
    public void runScenario(final Session session, final Scenario scenario, final ProtocolRegistry registry) {
        final var props = configureScenarioFsm(session, scenario, registry);
        final var scnActor = ctx().actorOf(scenario.uuid().asString(), props);
    }

    private Props configureScenarioFsm(final Session session, final Scenario scenario, final ProtocolRegistry registry) {
        final var scenarioData = new ScenarioData();
        final OnStartFunction<ScenarioFsmContext, ScenarioData> onStart = (actorCtx, ctx, data) -> {
            actorCtx.self().tell(new ScenarioMessage.Init(session, scenario));
        };

        return FsmActor.of(ScenarioFsm.definition)
                .withContext(ref -> {
                    final var scenarioContext = new DefaultScenarioContext(ref, scenario.uuid(), registry);
                    return new DefaultScenarioFsmContext(self, ref, scenario, scenarioContext);
                })
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
