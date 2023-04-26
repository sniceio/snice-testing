package io.snice.testing.runtime;

import io.snice.testing.core.MessageBuilder;
import io.snice.testing.core.action.ActionBuilder;
import io.snice.testing.core.protocol.Protocol;
import io.snice.testing.core.scenario.Scenario;
import io.snice.testing.core.scenario.Simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

public class FakeRuntime implements SniceRuntime {

    public CompletableFuture<SniceRuntime> startFuture = new CompletableFuture<>();
    public CompletableFuture<Void> syncFuture = new CompletableFuture<>();

    // Note: not thread safe but this is for unit testing so it's all good...
    public List<SimulationExec> simulations = new ArrayList<>();

    public record SimulationExec(Simulation simulation, CompletionStage<Void> future) {

    }

    @Override
    public Future<SniceRuntime> start() {
        startFuture.complete(this);
        return startFuture;
    }

    @Override
    public CompletionStage<Void> sync() {
        return syncFuture;
    }

    @Override
    public CompletionStage<Void> run(final Scenario scenario, final List<Protocol> protocols) {
        throw new RuntimeException("no");
    }

    @Override
    public <T extends Simulation> CompletionStage<Void> run(final T simulation) {
        System.err.println("Running simulation");
        final var exec = new SimulationExec(simulation, new CompletableFuture<Void>());
        simulations.add(exec);
        return exec.future;
    }

    @Override
    public CompletionStage<Void> run(final ActionBuilder builder) {
        throw new RuntimeException("no");
    }

    @Override
    public CompletionStage<Void> run(final MessageBuilder... message) {
        throw new RuntimeException("no");
    }
}
