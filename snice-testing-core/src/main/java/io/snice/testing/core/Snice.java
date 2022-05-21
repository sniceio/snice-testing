package io.snice.testing.core;

import io.hektor.actors.fsm.FsmActor;
import io.hektor.actors.fsm.OnStartFunction;
import io.hektor.core.ActorRef;
import io.hektor.core.Hektor;
import io.hektor.core.Props;
import io.snice.testing.core.protocol.Protocol;
import io.snice.testing.core.protocol.ProtocolRegistry;
import io.snice.testing.core.scenario.Scenario;
import io.snice.testing.core.scenario.ScenarioContex;
import io.snice.testing.core.scenario.fsm.DefaultScenarioSupervisorCtx;
import io.snice.testing.core.scenario.fsm.ScenarioSupervisorCtx;
import io.snice.testing.core.scenario.fsm.ScenarioSupervisorData;
import io.snice.testing.core.scenario.fsm.ScenarioSupervisorFsm;
import io.snice.testing.core.scenario.fsm.ScenarioSupervisorMessages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.snice.preconditions.PreConditions.assertNotNull;

public final class Snice {

    // TODO: make it configurable
    private static final int noOfScnSupervisors = 5;

    private final SniceConfig config;
    private final Scenario scenario;
    private final List<Protocol> protocols;
    private final Hektor hektor;

    private List<ActorRef> supervisors;

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

    private Snice(final SniceConfig config, final Hektor hektor, final List<Protocol> protocols, final Scenario scenario) {
        this.config = config;
        this.hektor = hektor;
        this.scenario = scenario;
        this.protocols = protocols;
    }


    private Snice run() throws InterruptedException {

        // TODO: new flow, something like this:
        // 1. Gather all protocol settings from all the Scenarios that we are supposed to execute.
        // 2. Ensure there are no conflicts (not sure what those conflicts would be. Shouldn't really be any)
        // 3. Allocate UUIDs for all scenarios. These UUIDs will be part of any potential "accept" listening points.
        // 4. Configure IpProviders and if any scenario needs it then allocate a unique URL per "accept"
        //    and create a FQDN based on the "accepts" potential additional "path" (only for certain protocols, such as
        //    HTTP based ones).

        // Note: these set of protcools are uniquely configured for the one single Scenario
        // and right now we are mixing concepts. The ScenarioSupervisors are kind of per
        // system but then we run a single scenario etc. Need to separate it all since
        // either it's a single run or a system.
        protocols.forEach(Protocol::start);

        final var latch = new CountDownLatch(noOfScnSupervisors);
        final var scnSupervisorProps = configureScenarioSupervisor(latch);
        supervisors = IntStream.range(0, noOfScnSupervisors).boxed()
                .map(i -> hektor.actorOf("ScenarioSupervisor-" + i, scnSupervisorProps))
                .collect(Collectors.toList());

        latch.await(100, TimeUnit.MILLISECONDS);
        runScenario(scenario);

        return this;
    }

    private ActorRef nextSupervisor() {
        return supervisors.get(new Random().nextInt(noOfScnSupervisors));
    }

    private void runScenario(final Scenario scenario) {
        final var registry = configureProtocolRegistry(protocols);
        final var ctx = new ScenarioContex(registry);
        final var session = new Session(scenario.name());

        nextSupervisor().tell(new ScenarioSupervisorMessages.Run(scenario, session, ctx));
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

    private static Props configureScenarioSupervisor(final CountDownLatch latch) {

        final OnStartFunction<ScenarioSupervisorCtx, ScenarioSupervisorData> onStart = (actorCtx, ctx, data) -> {
            actorCtx.self().tell(new ScenarioSupervisorMessages.Init());
        };

        return FsmActor.of(ScenarioSupervisorFsm.definition)
                .withContext(ref -> DefaultScenarioSupervisorCtx.of(ref, latch))
                .withData(() -> new ScenarioSupervisorData())
                .withStartFunction(onStart)
                .build();
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
            final var hektor = Hektor.withName("Snice")
                    .withConfiguration(config.getHektorConfig()).build();


            try {
                final var snice = new Snice(config, hektor, protocols, scenario).run();
                return snice;
            } catch (final Exception e) {
                throw new RuntimeException("Unable to start Snice", e);
            }
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
