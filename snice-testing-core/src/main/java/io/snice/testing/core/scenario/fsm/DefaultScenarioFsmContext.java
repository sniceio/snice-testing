package io.snice.testing.core.scenario.fsm;

import io.hektor.actors.fsm.FsmActor;
import io.hektor.actors.fsm.FsmActorContextSupport;
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
    public ActionJob prepareExecution(final InternalActionBuilder builder, final Session session) {
        final var props = configureActionFsm();
        final var actor = ctx().actorOf(builder.sri().asString(), props);

        final var action = builder.build(scenarioContext, new NextAction("Next", actor, builder.sri()));


        return new ActionJobImpl(builder.sri(), builder.isAsync(), session, action, actor);
    }

    private record ActionJobImpl(ActionResourceIdentifier sri, boolean isAsync, Session session, Action action,
                                 ActorRef actor) implements ActionJob {

        @Override
        public void start() {
            actor.tell(new ActionMessage.StartAction(session, action));
        }
    }

    private Props configureActionFsm() {
        final var actionData = new ActionData();
        final var actionCtx = new DefaultActionContext(self);

        return FsmActor.of(ActionFsm.definition)
                .withContext(actionCtx)
                .withData(actionData)
                .build();
    }

    /**
     * This is how we "trap" the action of handing control back to us. Each {@link Action} is unaware of the
     * actual execution environment and just calls "nextAction.execute", which is why we insert this "fake" action
     * as the next action to execute.
     */
    private static record NextAction(String name, ActorRef actor, ActionResourceIdentifier sri) implements Action {

        @Override
        public void execute(final List<Execution> executions, final Session session) {
            actor.tell(new ActionMessage.ActionFinished(sri, session, executions));
        }
    }


}
