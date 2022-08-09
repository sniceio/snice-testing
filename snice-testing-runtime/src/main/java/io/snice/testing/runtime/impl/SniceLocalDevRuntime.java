package io.snice.testing.runtime.impl;

import io.hektor.actors.fsm.FsmActor;
import io.hektor.actors.fsm.OnStartFunction;
import io.hektor.core.ActorRef;
import io.hektor.core.Hektor;
import io.hektor.core.Props;
import io.snice.testing.core.CoreDsl;
import io.snice.testing.core.MessageBuilder;
import io.snice.testing.core.Session;
import io.snice.testing.core.action.ActionBuilder;
import io.snice.testing.core.protocol.Protocol;
import io.snice.testing.core.protocol.ProtocolProvider;
import io.snice.testing.core.protocol.ProtocolRegistry;
import io.snice.testing.core.scenario.DefaultScenarioContext;
import io.snice.testing.core.scenario.Scenario;
import io.snice.testing.runtime.SniceRuntime;
import io.snice.testing.runtime.fsm.DefaultScenarioSupervisorCtx;
import io.snice.testing.runtime.fsm.ScenarioSupervisorCtx;
import io.snice.testing.runtime.fsm.ScenarioSupervisorData;
import io.snice.testing.runtime.fsm.ScenarioSupervisorFsm;
import io.snice.testing.runtime.fsm.ScenarioSupervisorMessages;
import io.snice.util.concurrent.SniceThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.snice.preconditions.PreConditions.assertArrayNotEmpty;
import static io.snice.preconditions.PreConditions.assertNotNull;
import static java.util.stream.Collectors.toList;

public class SniceLocalDevRuntime implements SniceRuntime {

    private static final Logger logger = LoggerFactory.getLogger(SniceLocalDevRuntime.class);

    // TODO: make it configurable
    private static final int noOfScnSupervisors = 5;

    private final Hektor hektor;
    private List<ActorRef> supervisors;

    private final List<CompletionStage<Void>> runningScenarios = Collections.synchronizedList(new ArrayList<>());

    private final CompletableFuture<Void> doneFuture = new CompletableFuture<>();

    private final CountDownLatch firstScenarioScheduledLatch = new CountDownLatch(1);

    SniceLocalDevRuntime(final Hektor hektor) {
        this.hektor = hektor;
    }

    @Override
    public Future<Void> sync() {
        return doneFuture;
    }

    private boolean waitForFirstTaskToBeScheduled() {
        try {
            firstScenarioScheduledLatch.await(1, TimeUnit.SECONDS);
            return firstScenarioScheduledLatch.getCount() <= 0;
        } catch (final InterruptedException e) {
            return false;
        }
    }

    private void monitor() {
        logger.info("Starting monitoring system");
        boolean done = false;

        if (waitForFirstTaskToBeScheduled()) {

            while (!done) {
                try {
                    if (areAllScenariosCompleted()) {
                        done = true;
                    } else {
                        sleep(100);
                    }
                } catch (final Throwable t) {
                    // TODO
                    done = true;
                    t.printStackTrace();
                }
            }

            logger.info("All tasks completed, shutting down system");
        } else {
            logger.info("No tasks were ever scheduled, shutting down system");
        }
        sleep(10000);
        doneFuture.complete(null);
        hektor.terminate();

        // TODO: stupid hektor that isn't shutting down because it isn't implemented!
        System.exit(1);
    }


    private void sleep(final int ms) {
        try {
            Thread.sleep(ms);
        } catch (final InterruptedException e) {
            // ignore
        }
    }

    private boolean areAllScenariosCompleted() {
        return !runningScenarios.stream()
                .map(CompletionStage::toCompletableFuture)
                .filter(f -> !f.isDone())
                .findFirst()
                .isPresent();
    }

    @Override
    public Future<SniceRuntime> start() {

        final var startFuture = new CompletableFuture<SniceRuntime>();

        // TODO: after re-structuring and moving this from the Snice main class, the below comments
        // TODO: will have to be revisited. Overall they kind of apply but even so, take the below as
        // TODO: all suggestions/thinking of how to proceed.

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
        // protocols.forEach(Protocol::start);

        final var latch = new CountDownLatch(noOfScnSupervisors);
        final var scnSupervisorProps = configureScenarioSupervisor(latch);
        supervisors = IntStream.range(0, noOfScnSupervisors).boxed()
                .map(i -> hektor.actorOf("ScenarioSupervisor-" + i, scnSupervisorProps))
                .collect(toList());

        // TODO: what to do if the supervisors doesn't start?
        try {
            latch.await(100, TimeUnit.MILLISECONDS);
        } catch (final InterruptedException e) {
            throw new RuntimeException("Unable to start the Snice Runtime. Exiting.", e);
        }

        final var threadGroup = SniceThreadFactory.withNamePrefix("main-snice-").withDaemon(true).build();
        Executors.newSingleThreadExecutor(threadGroup).submit(() -> monitor());

        // TODO: This obviously is not how you do it. Need to kick off the above in a different thread etc.
        startFuture.complete(null);
        return startFuture;
    }

    @Override
    public CompletionStage<Void> run(final Scenario scenario, final List<Protocol> protocols) {
        return internalRun(scenario, protocols, true);
    }

    @Override
    public CompletionStage<Void> run(final ActionBuilder builder) {
        assertNotNull(builder);
        return internalRun(CoreDsl.scenario("Default Scenario").execute(builder), List.of(), false);
    }

    @Override
    public CompletionStage<Void> run(final MessageBuilder... messages) {
        assertArrayNotEmpty(messages);
        var scenario = CoreDsl.scenario("Default Scenario");
        for (int i = 0; i < messages.length; ++i) {
            scenario = scenario.execute(messages[i]);
        }

        return internalRun(scenario, List.of(), false);
    }

    /**
     * Create the necessary components representing the environment in which the {@link Scenario}
     * is executing and ensure that all necessary {@link Protocol}s needed by the scenario is actually
     * configured. If we run in strict mode then if a protocol is missing, we will bail out with an error
     * but if non-strict mode then we'll try and create a default protocol to satisfy the requirements of
     * the {@link Scenario}.
     *
     * @param scenario   the scenario to run.
     * @param protocols  the protocols to use in the scenario.
     * @param strictMode if true then the list of protocols must satisfy all the protocols needed by the given scenario.
     * @return
     */
    public CompletionStage<Void> internalRun(final Scenario scenario, final List<Protocol> protocols, final boolean strictMode) {
        final var protocolsMap = protocols.stream().collect(Collectors.toMap(Protocol::key, Function.identity()));
        final var requiredProtocols = scenario.protocols();
        final var missingProtocols = requiredProtocols.stream().filter(key -> !protocolsMap.containsKey(key)).collect(toList());

        if (!missingProtocols.isEmpty() && strictMode) {
            throw new IllegalArgumentException("The following required protocol(s) for running the scenario are missing: " + missingProtocols);
        }

        createDefaultProtocols(missingProtocols).stream().forEach(p -> protocolsMap.put(p.key(), p));

        final var registry = new SimpleProtocolRegistry<>(protocolsMap);

        // TODO: when we are running many scenarios that are using the same underlying protocol stack (such
        //      as an HTTP stack) the Protocol.start() must ensure that the stack isn't started again etc.
        //      Currently, that is not the case.
        protocolsMap.values().forEach(Protocol::start);

        final var ctx = new DefaultScenarioContext(registry);
        final var session = new Session(scenario.name());

        final var future = new CompletableFuture<Void>();
        nextSupervisor().tell(new ScenarioSupervisorMessages.Run(scenario, session, ctx, future));
        runningScenarios.add(future);
        firstScenarioScheduledLatch.countDown();
        return future;
    }

    /**
     * When not in strict mode and if there are missing protocols, create default versions of them.
     *
     * @param missingProtocols
     * @return
     */
    private List<Protocol> createDefaultProtocols(final List<ProtocolRegistry.Key> missingProtocols) {

        // so we don't have to load providers if we don't have to
        if (missingProtocols.isEmpty()) {
            return List.of();
        }

        final var protocolProviders = ProtocolProvider.load();
        return missingProtocols.stream()
                .map(key -> protocolProviders.get(key))
                .map(Optional::ofNullable)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(ProtocolProvider::createDefaultProtocol)
                .collect(Collectors.toUnmodifiableList());
    }

    private ProtocolRegistry extendRegistry(final ProtocolRegistry registry, final List<Protocol> protocols) {
        return ((SimpleProtocolRegistry) registry).extend(protocols);
    }

    private ActorRef nextSupervisor() {
        return supervisors.get(new Random().nextInt(noOfScnSupervisors));
    }

    private <T extends Protocol> ProtocolRegistry configureProtocolRegistry(final List<T> protocols) {
        final Map<ProtocolRegistry.Key, T> map = new HashMap<>();
        protocols.forEach(p -> map.put(p.key(), p));
        return new SimpleProtocolRegistry<>(map);
    }

    private static record SimpleProtocolRegistry<T extends Protocol>(
            Map<Key, T> protocols) implements ProtocolRegistry {

        public SimpleProtocolRegistry<T> extend(final List<T> protocols) {
            final var extendedProtocols = new HashMap<Key, T>();
            this.protocols.forEach((key, value) -> extendedProtocols.put(key, value));
            protocols.forEach(p -> extendedProtocols.put(p.key(), p));
            return new SimpleProtocolRegistry<>(extendedProtocols);
        }

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

}
