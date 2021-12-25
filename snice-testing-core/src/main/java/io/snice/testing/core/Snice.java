package io.snice.testing.core;

import io.snice.testing.core.action.Action;
import io.snice.testing.core.protocol.Protocol;
import io.snice.testing.core.protocol.ProtocolRegistry;
import io.snice.testing.core.scenario.Scenario;
import io.snice.testing.core.scenario.ScenarioContex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.snice.preconditions.PreConditions.assertNotNull;

public final class Snice {

    private final SniceConfig config;
    private final Scenario scenario;
    private final List<Protocol> protocols;

    public static Builder run(final Scenario scenario) {
        return new BuilderImpl(scenario);
    }

    public interface Builder {

        Builder configuration(SniceConfig config);

        Builder protocols(Protocol... protocols);

        Builder protocols(Protocol.Builder... builders);

        /**
         * Start the {@link Scenario}
         *
         * @return
         */
        Snice start();
    }

    private Snice(final SniceConfig config, final List<Protocol> protocols, final Scenario scenario) {
        this.config = config;
        this.scenario = scenario;
        this.protocols = protocols;
    }

    private <T extends Protocol> ProtocolRegistry configureProtocolRegistry(final List<T> protocols) {
        final Map<ProtocolRegistry.Key, T> map = new HashMap<>();
        protocols.forEach(p -> map.put(p.key(), p));
        return new SimpleProtocolRegistry<>(map);
    }

    private static record SimpleProtocolRegistry<T extends Protocol>(
            Map<Key, T> protocols) implements ProtocolRegistry {
        @Override
        public Optional<T> protocol(final Key key) {
            return Optional.ofNullable(protocols.get(key));
        }
    }

    private Snice run() {
        // TODO: will turn it all into a Scenario actor of sorts and each Scenario
        // TODO: will be built up as an FSM

        protocols.forEach(Protocol::start);

        final var registry = configureProtocolRegistry(protocols);
        final var ctx = new ScenarioContex(registry);

        Action currentAction = new FinalAction("terminating");
        for (int i = scenario.actions().size() - 1; i >= 0; --i) {
            currentAction = scenario.actions().get(i).build(ctx, currentAction);
        }

        final var session = new Session(scenario.name());

        currentAction.execute(session);
        try {
            Thread.sleep(1000);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
        return this;
    }

    private static record FinalAction(String name) implements Action {

        @Override
        public void execute(final Session session) {
            System.err.println("The Terminating Action is being executed");
        }
    }

    private static class BuilderImpl implements Builder {
        private final Scenario scenario;
        private SniceConfig config;
        private final List<Protocol> protocols = new ArrayList<>();

        private BuilderImpl(final Scenario scenario) {
            this.scenario = scenario;
        }

        @Override
        public Snice start() {
            assertNotNull(scenario, "You must specify the scenario to run");
            return new Snice(config, protocols, scenario).run();
        }

        @Override
        public Builder configuration(final SniceConfig config) {
            assertNotNull(config);
            this.config = config;
            return this;
        }

        @Override
        public Builder protocols(final Protocol... protocols) {
            assertNotNull(protocols);
            Arrays.stream(protocols).forEach(this.protocols::add);
            return this;
        }

        @Override
        public Builder protocols(final Protocol.Builder... builders) {
            assertNotNull(builders);
            Arrays.stream(builders).map(Protocol.Builder::build).forEach(this.protocols::add);
            return this;
        }

    }


}
