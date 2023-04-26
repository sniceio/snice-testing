package io.snice.testing.runtime;

import io.snice.testing.core.MessageBuilder;
import io.snice.testing.core.action.ActionBuilder;
import io.snice.testing.core.protocol.Protocol;
import io.snice.testing.core.scenario.Scenario;
import io.snice.testing.core.scenario.Simulation;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

public class RuntimeNeverStarts implements SniceRuntime {

    public CompletableFuture<SniceRuntime> startFuture = new CompletableFuture<>();

    @Override
    public Future<SniceRuntime> start() {
        System.out.println("Never!");
        return new CompletableFuture<>();
    }

    @Override
    public CompletionStage<Void> sync() {
        return null;
    }

    @Override
    public CompletionStage<Void> run(final Scenario scenario, final List<Protocol> protocols) {
        return null;
    }

    @Override
    public <T extends Simulation> CompletionStage<Void> run(final T simulation) {
        return null;
    }

    @Override
    public CompletionStage<Void> run(final ActionBuilder builder) {
        return null;
    }

    @Override
    public CompletionStage<Void> run(final MessageBuilder... message) {
        return null;
    }
}
