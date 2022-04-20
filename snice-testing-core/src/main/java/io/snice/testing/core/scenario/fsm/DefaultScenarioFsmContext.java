package io.snice.testing.core.scenario.fsm;

import io.hektor.actors.fsm.FsmActor;
import io.hektor.actors.fsm.FsmActorContextSupport;
import io.hektor.actors.fsm.OnStartFunction;
import io.hektor.core.ActorContext;
import io.hektor.core.ActorRef;
import io.hektor.core.Props;
import io.snice.identity.sri.ActionResourceIdentifier;
import io.snice.testing.core.Execution;
import io.snice.testing.core.Session;
import io.snice.testing.core.action.Action;
import io.snice.testing.core.scenario.InternalActionBuilder;
import io.snice.testing.core.scenario.Scenario;
import io.snice.testing.core.scenario.ScenarioContex;

import java.util.List;

import static io.snice.preconditions.PreConditions.assertNotNull;

public record DefaultScenarioFsmContext(ActorRef parent,
                                        ActorRef self,
                                        Scenario scenario,
                                        ScenarioContex scenarioContext) implements ScenarioFsmContext, FsmActorContextSupport {

    public DefaultScenarioFsmContext {
        assertNotNull(parent);
        assertNotNull(self);
        assertNotNull(scenario);
        assertNotNull(scenarioContext);
    }

    @Override
    public void tell(final ScenarioMessage msg) {
        assertNotNull(msg);
        ctx().self().tell(msg);
    }

    @Override
    public void processActionResult(final List<Execution> executions, final Session session) {
        System.err.println("Apparently processing the action result");
        // self.tell(new ScenarioMessage.Exec(executions, session));
    }

    @Override
    public void processFinalResult(final List<Execution> executions, final Session session) {
        parent.tell(new ScenarioSupervisorMessages.RunCompleted(scenario, session, executions));
    }

    @Override
    public ActionJob prepareExecution(final InternalActionBuilder builder, final Session session) {
        final var action = builder.build(scenarioContext, new NextAction("Next", this));
        final var sri = ActionResourceIdentifier.of();

        final var props = configureActionFsm(session, action);
        final var job = new ActionJobImpl(sri, props, ctx());
        return job;
    }

    private record ActionJobImpl(ActionResourceIdentifier sri, Props props,
                                 ActorContext actorContext) implements ActionJob {

        @Override
        public void start() {
            final var actionActor = actorContext.actorOf(sri.asString(), props);
        }
    }

    private Props configureActionFsm(final Session session, final Action action) {
        final var actionData = new ActionData();
        final var actionCtx = new DefaultActionContext();

        final OnStartFunction<ActionContext, ActionData> onStart = (actorCtx, ctx, data) -> {
            actorCtx.self().tell(new ActionMessage.StartAction(session, action));
        };

        return FsmActor.of(ActionFsm.definition)
                .withContext(actionCtx)
                .withData(actionData)
                .withStartFunction(onStart)
                .build();
    }

    /**
     * This is how we "trap" the action of handing control back to us. Each {@link Action} is unaware of the
     * actual execution environment and just calls "nextAction.execute", which is why we insert this "fake" action
     * as the next action to execute.
     */
    private static record NextAction(String name, ScenarioFsmContext ctx) implements Action {

        @Override
        public void execute(final List<Execution> executions, final Session session) {
            ctx.processActionResult(executions, session);
        }
    }


}
