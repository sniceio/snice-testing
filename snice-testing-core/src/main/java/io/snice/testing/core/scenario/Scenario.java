package io.snice.testing.core.scenario;

import io.snice.testing.core.MessageBuilder;
import io.snice.testing.core.action.Action;
import io.snice.testing.core.action.ActionBuilder;
import io.snice.testing.core.protocol.Protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.snice.preconditions.PreConditions.assertNotEmpty;

public record Scenario(String name, List<ActionBuilder> actions) {

    public Scenario(final String name) {
        this(name, List.of());
    }

    public Scenario(final String name, final List<ActionBuilder> actions) {
        assertNotEmpty(name);
        this.name = name;
        this.actions = actions == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(actions));
    }

    public Scenario execute(final ActionBuilder action) {
        // this is annoying because we will once again copy the entire list in the
        // constructor of the Scenario. Java really needs a proper immutable list and not the
        // unmodifiable one. Really annoying...
        final var extended = new ArrayList<>(actions);
        extended.add(action);
        return new Scenario(name, extended);
    }

    public <T extends Protocol> Scenario execute(final MessageBuilder message) {
        return execute(new ActionBuilderMessageWrapper(message));
    }

    private static record ActionBuilderMessageWrapper(MessageBuilder message) implements ActionBuilder {

        @Override
        public Action build(final ScenarioContex ctx, final Action next) {
            final var builder = message.toActionBuilder();
            return builder.build(ctx, next);
        }
    }

}

