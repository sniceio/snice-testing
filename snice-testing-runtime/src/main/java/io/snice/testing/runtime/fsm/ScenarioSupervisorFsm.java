package io.snice.testing.runtime.fsm;

import io.hektor.core.LifecycleEvent;
import io.hektor.fsm.Definition;
import io.hektor.fsm.FSM;
import io.snice.identity.sri.ScenarioResourceIdentifier;

import static io.snice.testing.runtime.fsm.ScenarioSupervisorState.INIT;
import static io.snice.testing.runtime.fsm.ScenarioSupervisorState.RUNNING;
import static io.snice.testing.runtime.fsm.ScenarioSupervisorState.TERMINATED;

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
                .consume(); // nothing to do at the moment

        running.transitionTo(RUNNING)
                .onEvent(ScenarioSupervisorMessages.Run.class)
                .withAction((run, ctx, data) -> {
                    data.storeRun(run);
                    ctx.runScenario(run.session(), run.scenario(), run.registry());
                });

        running.transitionTo(RUNNING)
                .onEvent(ScenarioSupervisorMessages.RunCompleted.class)
                .withAction(ScenarioSupervisorFsm::processRunCompleted);

        running.transitionTo(RUNNING)
                .onEvent(LifecycleEvent.Terminated.class)
                .withAction(ScenarioSupervisorFsm::processScenarioTerminated);

        running.transitionTo(TERMINATED)
                .onEvent(ScenarioSupervisorMessages.Terminate.class)
                .consume(); // nothing to do at the moment

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

        final var sri = ScenarioResourceIdentifier.from(event.getActor().name());
        final var originalRun = data.removeRun(sri).orElseThrow(() -> new IllegalArgumentException("Unknown Scenario completed: " + event.getActor().name()));
        originalRun.future().complete(null);
    }
}
