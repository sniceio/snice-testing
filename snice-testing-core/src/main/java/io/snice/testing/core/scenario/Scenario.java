package io.snice.testing.core.scenario;

import io.snice.functional.Either;
import io.snice.identity.sri.ScenarioResourceIdentifier;
import io.snice.testing.core.Execution;
import io.snice.testing.core.MessageBuilder;
import io.snice.testing.core.Session;
import io.snice.testing.core.action.Action;
import io.snice.testing.core.action.ActionBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.snice.preconditions.PreConditions.assertNotEmpty;
import static io.snice.preconditions.PreConditions.assertNotNull;

public record Scenario(ScenarioResourceIdentifier uuid, String name, List<ActionBuilder> actions) {

    public Scenario(final String name) {
        this(name, List.of());
    }

    public Scenario(final String name, final List<ActionBuilder> actions) {
        this(ScenarioResourceIdentifier.of(), name, actions);

    }

    public Scenario(final ScenarioResourceIdentifier uuid, final String name, final List<ActionBuilder> actions) {
        assertNotNull(uuid, "You must specify the UUID for this scenario");
        assertNotEmpty(name, "You must specify a name for the scenario");
        this.uuid = uuid;
        this.name = name;
        this.actions = actions == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(actions));
    }

    // TODO: no longer valid. Will do async stuff instead.
    public ParallelExecution.Builder executeInParallel() {
        return new ParallelExecution.Builder(List.of());
    }

    public Scenario execute(final Function<Session, Session> f) {
        assertNotNull(f);
        return execute(new GenericSessionActionBuilder(f));
    }

    public static class ParallelExecution implements ActionBuilder {

        private final List<ActionBuilder> actions;

        private ParallelExecution(final List<ActionBuilder> actions) {
            this.actions = actions;
        }

        @Override
        public Action build(final ScenarioContex ctx, final Action next) {
            return null;
        }

        public static class Builder {
            private final List<ActionBuilder> actions;

            private Builder(final List<ActionBuilder> actions) {
                this.actions = actions;
            }

            public Builder execute(final ActionBuilder b) {
                assertNotNull(b);

                final var extended = new ArrayList<>(actions);
                extended.add(b);
                return new Builder(extended);
            }

            public Builder execute(final MessageBuilder b) {
                assertNotNull(b);
                return execute(new ActionBuilderMessageWrapper(b));
            }

            public Builder execute(final Function<Session, Session> f) {
                assertNotNull(f);
                return execute(new GenericSessionActionBuilder(f));
            }
        }
    }

    public Scenario execute(final ActionBuilder action) {
        // this is annoying because we will once again copy the entire list in the
        // constructor of the Scenario. Java really needs a proper immutable list and not the
        // unmodifiable one. Really annoying...
        final var extended = new ArrayList<>(actions);
        extended.add(action);
        return new Scenario(name, extended);
    }

    public <T> Scenario execute(final T... maybe) {
        if (maybe == null || maybe.length == 0) {
            return this;
        }

        final var eitherActions = Arrays.stream(maybe).map(object -> {
            if (object instanceof Function) {
                System.err.println("yeah, function");
                return Either.right((Function) object);
            } else if (object instanceof MessageBuilder) {
                System.err.println("yeah, messageBuilder");
                return Either.right(object);
            } else if (object instanceof ActionBuilder) {
                System.err.println("yeah, ActionBuilder");
                return Either.right(object);
            }
            return Either.left("Unknown object type " + object.getClass().getName());
        }).collect(Collectors.toList());

        eitherActions.stream().filter(Either::isLeft).findAny().ifPresent(error -> {
            throw new IllegalArgumentException((String) error.getLeft());
        });

        System.err.println(eitherActions);

        return this;
    }

    public Scenario execute(final MessageBuilder message) {
        return execute(new ActionBuilderMessageWrapper(message));
    }

    private static record GenericSessionActionBuilder(Function<Session, Session> f) implements ActionBuilder {

        @Override
        public Action build(final ScenarioContex ctx, final Action next) {
            return new GenericSessionAction("generic-exec", f, next);
        }
    }

    private static record GenericSessionAction(String name,
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

    private static record ActionBuilderMessageWrapper(MessageBuilder message) implements ActionBuilder {

        @Override
        public Action build(final ScenarioContex ctx, final Action next) {
            final var builder = message.toActionBuilder();
            return builder.build(ctx, next);
        }
    }

}

