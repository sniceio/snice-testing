package io.snice.testing.core.scenario;

import io.snice.testing.core.protocol.Protocol;

import java.util.List;

/**
 * The {@link ExecutionPlan} describes the user's intent of how to execute a particular test.
 * It's main purpose is to serve as a way for the user to create the plan through its
 * {@link ExecutionPlan#setUp(Scenario)} "kick-off" method and then a way for the runtime
 * environment to find and load these plans (without the need for a registry, which would have
 * been another approach). Once the plan has been set, the runtime will load a given {@link ExecutionPlan}
 * and ask it to dump itself into this {@link ExecutionSettings}. Although part of API, the {@link ExecutionSettings}
 * is not really anything the user should care about.
 */
public record ExecutionSettings(String name, Scenario scenario, List<Protocol> protocols, boolean strictMode) {
}
