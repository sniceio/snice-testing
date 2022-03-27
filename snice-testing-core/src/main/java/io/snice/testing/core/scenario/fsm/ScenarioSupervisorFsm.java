package io.snice.testing.core.scenario.fsm;

import io.hektor.core.LifecycleEvent;
import io.hektor.fsm.Definition;
import io.hektor.fsm.FSM;

import static io.snice.testing.core.scenario.fsm.ScenarioSupervisorState.INIT;
import static io.snice.testing.core.scenario.fsm.ScenarioSupervisorState.RUNNING;
import static io.snice.testing.core.scenario.fsm.ScenarioSupervisorState.TERMINATED;

public class ScenarioSupervisorFsm {

    public static final Definition<ScenarioSupervisorState, ScenarioSupervisorCtx, ScenarioSupervisorData> definition;

    static {
        final var builder = FSM.of(ScenarioSupervisorState.class)
                .ofContextType(ScenarioSupervisorCtx.class)
                .withDataType(ScenarioSupervisorData.class);

        final var init = builder.withInitialState(INIT);
        final var running = builder.withState(RUNNING);
        final var terminated = builder.withFinalState(TERMINATED);

        init.transitionTo(RUNNING)
                .onEvent(ScenarioSupervisorMessages.Init.class)
                .withAction(i -> System.err.println("initializing"));

        running.transitionTo(RUNNING)
                .onEvent(ScenarioSupervisorMessages.Run.class)
                .withAction((run, ctx, data) -> ctx.runScenario(run.session(), run.scenario(), run.ctx()));

        running.transitionTo(RUNNING)
                .onEvent(ScenarioSupervisorMessages.RunCompleted.class)
                .withAction(ScenarioSupervisorFsm::processRunCompleted);

        running.transitionTo(RUNNING)
                .onEvent(LifecycleEvent.Terminated.class)
                .withAction(ScenarioSupervisorFsm::processScenarioTerminated);

        running.transitionTo(TERMINATED)
                .onEvent(ScenarioSupervisorMessages.Terminate.class)
                .withAction(e -> System.err.println("terminated"));

        definition = builder.build();
    }

    private static void processRunCompleted(final ScenarioSupervisorMessages.RunCompleted event,
                                            final ScenarioSupervisorCtx ctx,
                                            final ScenarioSupervisorData data) {

        System.err.println("Processing the result of a run");
    }

    private static void processScenarioTerminated(final LifecycleEvent.Terminated event,
                                                  final ScenarioSupervisorCtx ctx,
                                                  final ScenarioSupervisorData data) {

        System.err.println("Processing the death of child: " + event.getActor());
    }
}
