package io.snice.testing.core.scenario;

import io.snice.testing.core.protocol.Protocol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static io.snice.preconditions.PreConditions.assertArrayNotEmpty;
import static io.snice.preconditions.PreConditions.assertCollectionNotEmpty;
import static io.snice.preconditions.PreConditions.assertNotNull;

/**
 * The {@link ExecutionPlan} is just that, a plan of how to execute a particular {@link Scenario} using the
 * associated {@link Protocol}(s) that {@link Scenario} needs and any other additional settings along with that
 * plan.
 */
public abstract class ExecutionPlan {

    private Planner planner;
    private Scenario scenario;

    private List<Protocol> protocols;
    private Protocol.Builder[] protocolsBuilders;
    private boolean strictMode;

    protected ExecutionPlan() {

    }

    public final Planner setUp(final Scenario scenario) {
        if (planner != null) {
            throw new IllegalStateException("You can only call setUp once");
        }

        assertNotNull(scenario);

        this.scenario = scenario;

        planner = new DefaultPlanner();
        return planner;
    }

    public ExecutionSettings settings() {
        return new ExecutionSettings(scenario.name(), scenario, gatherProtocols(), strictMode);
    }

    private List<Protocol> gatherProtocols() {
        final var p = processProtocolBuilders();
        if (protocols == null && p.isEmpty()) {
            return List.of();
        }

        if (protocols == null) {
            return p;
        }

        return mergeLists(protocols, p);
    }

    private static List<Protocol> mergeLists(final List<Protocol> p1, final List<Protocol> p2) {
        final var all = new ArrayList<>(p1);
        all.addAll(p2);
        return Collections.unmodifiableList(all);
    }

    private List<Protocol> processProtocolBuilders() {
        if (protocolsBuilders == null) {
            return List.of();
        }

        return Arrays.stream(protocolsBuilders)
                .map(Protocol.Builder::build)
                .collect(Collectors.toUnmodifiableList());
    }


    public interface Planner {

        Planner protocols(List<Protocol> protocols);

        Planner protocols(Protocol.Builder... protocols);

        /**
         * In strict mode, all the {@link Protocol}s needed by the {@link Scenario} has to be supplied
         * with the {@link ExecutionPlan}. If not in strict mode, default versions of any "missing" {@link Protocol}
         * is created.
         */
        Planner strictMode(boolean value);

    }

    private class DefaultPlanner implements Planner {

        @Override
        public ExecutionPlan.Planner protocols(final List<Protocol> protocols) {
            assertCollectionNotEmpty(protocols, "The list of protocols cannot be null or the empty list");
            ExecutionPlan.this.protocols = List.copyOf(protocols);
            return this;
        }

        @Override
        public ExecutionPlan.Planner protocols(final Protocol.Builder... protocols) {
            assertArrayNotEmpty(protocols, "The array of protocol builders cannot be null or empty");
            protocolsBuilders = Arrays.copyOf(protocols, protocols.length);
            return this;
        }

        @Override
        public Planner strictMode(final boolean value) {
            strictMode = value;
            return this;
        }

    }

}
