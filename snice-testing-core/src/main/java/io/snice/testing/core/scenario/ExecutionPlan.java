package io.snice.testing.core.scenario;

import io.snice.testing.core.protocol.Protocol;

import java.util.List;

/**
 * The {@link Simulation} describes the user's intent of how to execute a particular test.
 * It's main purpose is to serve as a way for the user to create a plan through its
 * {@link Simulation#setUp(Scenario)} "kick-off" method and then a way for the runtime
 * environment to find and load these simulations (without the need for a registry, which would have
 * been another approach). Once the {@link Simulation} has been created, the runtime will
 * load a given {@link Simulation} and ask it to describe itself through an {@link ExecutionPlan}.
 * Although part of API, the {@link ExecutionPlan} is not really anything the user should care about.
 */
public record ExecutionPlan(String name, Scenario scenario, List<Protocol> protocols, boolean strictMode) {
}
