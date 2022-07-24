package io.snice.testing.core.scenario.fsm;

import io.hektor.fsm.Data;
import io.snice.identity.sri.ActionResourceIdentifier;
import io.snice.testing.core.Execution;
import io.snice.testing.core.Session;
import io.snice.testing.core.scenario.InternalActionBuilder;
import io.snice.testing.core.scenario.Scenario;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.snice.preconditions.PreConditions.assertArgument;
import static io.snice.preconditions.PreConditions.assertNotNull;
import static io.snice.preconditions.PreConditions.assertNull;

public class ScenarioData implements Data {

    /**
     * This is always the current "official" {@link Session} and is what will be used in the
     * next ask to execute a new action. I.e. through the {@link ScenarioMessage.Exec} message.
     */
    private Session session;

    private Optional<Scenario> scenario = Optional.empty();
    private int actionIndex = 0;

    private final Map<ActionResourceIdentifier, ActionJobStatus> jobs = new HashMap<>();

    /**
     * There can never be more than ONE outstanding SYNCHRONOUS action at any given point
     * in time and we need to keep track of it. If this one is set, we MUST be in the
     * {@link ScenarioState#SYNC} state and waiting for this particular action
     * to complete before "moving on" with the rest of the {@link Scenario}.
     * <p>
     * Note: we could scan the {@link #jobs} map too since it should be the only synchronous one in there
     * that is not finished (could be lots of unfinished asynchronous actions of course). But, this makes
     * it more explicit so...
     */
    private Optional<ActionResourceIdentifier> outstandingSyncAction = Optional.empty();

    public ScenarioData() {
    }

    public Session session() {
        return session;
    }

    /**
     * Must only be called ONCE at the time the job is kicked-off. Also, kicking off yet another
     * synchronous job when there already is one outstanding is also an internal error.
     *
     * @param job
     */
    public void storeActionJob(final ActionJob job) {
        final var previous = jobs.put(job.sri(), new ActionJobStatus(job));
        assertNull(previous, "The same Action was started twice (internal bug). SRI " + job.sri());

        if (!job.isAsync()) {
            assertArgument(outstandingSyncAction.isPresent() == false, "There already is a synchronous outstanding " +
                    "job and there cannot be two executing at the same time. Internal bug. " +
                    "Current outstanding job is " + outstandingSyncAction + " and the new one that we are " +
                    "trying to start is " + job.sri());

            outstandingSyncAction = Optional.of(job.sri());
        }
    }

    public ActionJobStatus processActionTerminated(final ActionResourceIdentifier sri) {
        assertNotNull(sri);
        final var newStatus = jobs.computeIfPresent(sri, (key, old) -> old.updateActionTerminated());
        assertNotNull(newStatus, "Received a signal that an Action has been terminated but "
                + "for an Action that doesn't belong to this Scenario. Internal bug. SRI is " + sri);
        return newStatus;
    }

    public ActionJobStatus processActionFinished(final ActionMessage.ActionFinished finished) {
        assertNotNull(finished);
        final var newStatus = jobs.computeIfPresent(finished.sri(), (sri, old) -> old.update(finished));
        assertNotNull(newStatus, "Received an " + ActionMessage.ActionFinished.class.getName()
                + " message for an Action that doesn't belong to this Scenario. Internal bug. SRI is " + finished.sri());
        return newStatus;
    }

    public boolean isAllActionsDone() {
        final var finishedCount = jobs.values().stream().filter(status -> status.actionActorTerminated && status.actionFinished).count();
        return finishedCount == jobs.size();
    }

    public boolean isKnownJob(final ActionResourceIdentifier sri) {
        return jobs.containsKey(sri);
    }

    public boolean isTheOutstandingSynchronousAction(final ActionResourceIdentifier sri) {
        return outstandingSyncAction.map(s -> s.equals(sri)).orElse(false);
    }

    public void clearOutstandingSynchronousJob() {
        outstandingSyncAction = Optional.empty();
    }

    public void scenario(final Scenario scenario) {
        assertNotNull(scenario, "If you do not wish to set the Scenario, simply " +
                "don't call the method. This method assumes the Scenario is not null");
        this.scenario = Optional.of(scenario);
    }

    public void session(final Session session) {
        assertNotNull(scenario, "If you do not wish to set the Session, simply " +
                "don't call the method. This method assumes the Session is not null");
        this.session = session;
    }

    public boolean hasMoreActions() {
        return actionIndex < scenario.map(s -> s.actions().size()).orElse(0);
    }

    /**
     * Get the next {@link InternalActionBuilder} or else throw an {@link IllegalStateException}
     *
     * @return the next action
     */
    public InternalActionBuilder nextAction() {
        return scenario.map(s -> s.actions().get(actionIndex++))
                .map(builder -> (InternalActionBuilder) builder)
                .orElseThrow(() -> new IllegalStateException("Internal Error: There should have been more " +
                        "actions or it was not checked first, which is an internal bug"));
    }

    /**
     * Simple holder of information about the job progression
     *
     * @param sri                   - the unique {@link ActionResourceIdentifier} that identifies this action and its execution
     * @param job                   - the representation of the action to be executed.
     * @param executions            - the result of the exuction of the action. If this is empty, it has not yet completed.
     * @param actionActorTerminated - whether the underlying Actor has terminated or not.
     */
    private record ActionJobStatus(ActionResourceIdentifier sri,
                                   ActionJob job,
                                   List<Execution> executions,
                                   boolean actionFinished,
                                   boolean actionActorTerminated) {
        private ActionJobStatus {
            assertNotNull(sri);
            assertNotNull(job);
            assertNotNull(executions);
        }

        private ActionJobStatus(final ActionJob job) {
            this(job.sri(), job, List.of(), false, false);
        }

        /**
         * Note that the {@link ActionJobStatus} is immutable so a new copy will be
         * returned.
         */
        ActionJobStatus update(final ActionMessage.ActionFinished msg) throws IllegalArgumentException {
            assertNotNull(msg, "The " + ActionMessage.ActionFinished.class.getName() + " cannot be null");
            assertArgument(sri.equals(msg.sri()), "The " + ActionMessage.ActionFinished.class.getName() +
                    " is not for the same SRI.");

            return new ActionJobStatus(sri, job, msg.executions(), true, actionActorTerminated);
        }

        /**
         * Called when the action has "fully" terminated, meaning, the underlying actor has died.
         *
         * @return
         * @throws IllegalArgumentException
         */
        ActionJobStatus updateActionTerminated() throws IllegalArgumentException {
            return new ActionJobStatus(sri, job, executions, actionFinished, true);
        }
    }
}
