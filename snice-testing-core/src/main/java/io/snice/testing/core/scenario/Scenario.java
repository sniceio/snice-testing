package io.snice.testing.core.scenario;

import io.snice.functional.Either;
import io.snice.functional.TriFunction;
import io.snice.identity.sri.ActionResourceIdentifier;
import io.snice.identity.sri.ScenarioResourceIdentifier;
import io.snice.testing.core.Execution;
import io.snice.testing.core.MessageBuilder;
import io.snice.testing.core.Session;
import io.snice.testing.core.action.Action;
import io.snice.testing.core.action.ActionBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import static io.snice.preconditions.PreConditions.assertArrayNotEmpty;
import static io.snice.preconditions.PreConditions.assertNotEmpty;
import static io.snice.preconditions.PreConditions.assertNotNull;

public record Scenario(ScenarioResourceIdentifier uuid, String name, List<ActionBuilder> actions) {

    public Scenario(final String name) {
        this(name, List.of());
    }

    public Scenario(final String name, final List<ActionBuilder> actions) {
        this(ScenarioResourceIdentifier.of(), name, actions);

    }

    /**
     * Ensure that the {@link Scenario} is valid.
     * <p>
     * Note that everything cannot be validated until runtime when some of the variable expressions
     * are resolved. This check does the most basic checks such as if you have a {@link #join(BiFunction, String...)}
     * operation that the names you are joining on actually were defined earlier in the scenario etc.
     *
     * @return if successful, {@link Either#right(Object)} will be returned with <code>this</code> and if there are
     * failures, a left-based {@link Either} is returned with a {@link List} containing human-readable error reasons.
     */
    public Either<List<String>, Scenario> validate() {
        return Either.right(this);
    }

    public Scenario(final ScenarioResourceIdentifier uuid, final String name, final List<ActionBuilder> actions) {
        assertNotNull(uuid, "You must specify the UUID for this scenario");
        assertNotEmpty(name, "You must specify a name for the scenario");
        this.uuid = uuid;
        this.name = name;
        this.actions = actions == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(actions));
    }

    public Scenario execute(final Function<Session, Session> f) {
        assertNotNull(f);
        return execute(new GenericSessionActionBuilder(f));
    }

    private Scenario extendActions(final InternalActionBuilderWrapper action) {
        // this is annoying because we will once again copy the entire list in the
        // constructor of the Scenario. Java really needs a proper immutable list and not the
        // unmodifiable one. Really annoying...
        final var extended = new ArrayList<>(actions);
        extended.add(action);
        return new Scenario(name, extended);
    }

    public Scenario execute(final ActionBuilder builder) {
        final var sri = ActionResourceIdentifier.of();
        return extendActions(new InternalActionBuilderWrapper(sri, false, false, builder::build));
    }

    public Scenario execute(final MessageBuilder message) {
        return extendActions(new InternalActionBuilderWrapper(ActionResourceIdentifier.of(), false, false,
                (sri, ctx, action) -> message.toActionBuilder().build(sri, ctx, action)));
    }

    public Scenario executeAsync(final Function<Session, Session> f) {
        throw new RuntimeException("TODO - not yet implemented");
    }

    public Scenario executeAsync(final ActionBuilder builder) {
        return extendActions(new InternalActionBuilderWrapper(ActionResourceIdentifier.of(), true, false,
                builder::build));
    }

    public Scenario executeAsync(final MessageBuilder message) {
        return extendActions(new InternalActionBuilderWrapper(ActionResourceIdentifier.of(), true, false,
                (sri, ctx, action) -> message.toActionBuilder().build(sri, ctx, action)));
    }

    /**
     * Ensure that any previous asynchronous executions are finished before moving on.
     *
     * @param fn    a function that takes the current {@link Session}, as returned by the last
     *              synchronous execution before this join, and a {@link List} of all the {@link Session}s
     *              returned by the asynchronous executions and returns a new {@link Session}. You can add code
     *              in this function to e.g. merge information from the {@link Session}s of your async steps.
     * @param names the names of the asynchronous actions to join on.
     * @return the updated {@link Session} that now will be the latest and greatest session within
     * the {@link Scenario} and is what will be fed into the next execution step.
     */
    public Scenario join(final BiFunction<Session, List<Session>, Session> fn, final String... names) {
        assertNotNull(fn);
        assertArrayNotEmpty(names, "You must specify at least one Action to wait for");

        return extendActions(new InternalActionBuilderWrapper(ActionResourceIdentifier.of(), false, false,
                (sri, ctx, action) -> new JoinAction(fn, sri, names)));
    }

    /**
     * Same as {@link #join(BiFunction, String...)} but where you don't care about processing
     * the various {@link Session}s.
     * <p>
     * This is just a convenience method for:
     *
     * <code>join((session, sessions) -> session, names);</code>
     */
    public Scenario join(final String... names) {
        return join((session, sessions) -> session, names);
    }

    private static record InternalActionBuilderWrapper(ActionResourceIdentifier sri,
                                                       boolean isAsync,
                                                       boolean isScenario,
                                                       TriFunction<ActionResourceIdentifier, ScenarioContex, Action, Action> builder) implements InternalActionBuilder {

        InternalActionBuilderWrapper {
            assertNotNull(sri);
            assertNotNull(builder);
        }

        /*
        InternalActionBuilderWrapper(final BiFunction<ScenarioContex, Action, Action> builder,
                                     final boolean isAsync,
                                     final boolean isScenario) {
            this(ActionResourceIdentifier.of(), isAsync, isScenario, builder);
        }
         */

        @Override
        public Action build(final ActionResourceIdentifier sri, final ScenarioContex ctx, final Action next) {
            throw new RuntimeException("The generic ActionBuilder::build method should never be " +
                    "used on the InternalActionBuilderWrapper. Internal bug");
        }

        @Override
        public Action build(final ScenarioContex ctx, final Action next) {
            return builder.apply(sri, ctx, next);
        }
    }

    private static record JoinAction(BiFunction<Session, List<Session>, Session> fn,
                                     ActionResourceIdentifier sri,
                                     String... names) implements Action {


        @Override
        public String name() {
            return "join";
        }

        @Override
        public void execute(final List<Execution> executions, final Session session) {
            System.err.println("JJJJJJOOOOOIIIIINNNNNN " + Objects.toString(names));
        }
    }

    private static record GenericSessionActionBuilder(Function<Session, Session> f) implements ActionBuilder {

        @Override
        public Action build(final ActionResourceIdentifier sri, final ScenarioContex ctx, final Action next) {
            return new GenericSessionAction("generic-exec", sri, f, next);
        }
    }

    private static record GenericSessionAction(String name,
                                               ActionResourceIdentifier sri,
                                               Function<Session, Session> f,
                                               Action next) implements Action {

        @Override
        public void execute(final List<Execution> executions, final Session session) {
            // TODO: add the "execution" of this step as well.
            try {
                next.execute(executions, f.apply(session));
            } catch (final Throwable t) {
                // TODO: what to do with the exception?
                next.execute(executions, session.markAsFailed());
            }
        }
    }
}

