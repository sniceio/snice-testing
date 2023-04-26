package io.snice.testing.runtime;

import io.snice.testing.runtime.config.RuntimeConfig;
import io.snice.testing.runtime.config.SimulationConfig;
import io.snice.testing.runtime.impl.SniceLocalDevRuntimeProvider;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * Simple holder for the arguments as given on the command line.
 *
 * @param args      the raw original arguments as given on the command line.
 * @param namespace the recognized arguments wrapped in a {@link Namespace}
 * @param unknown   list of unknown arguments
 */
public record CliArgs(String[] args, Namespace namespace, List<String> unknown) {

    public static final String DEFAULT_RUNTIME_PROVIDER = SniceLocalDevRuntimeProvider.class.getName();

    public static final String ARG_RUNTIME = "runtime";
    public static final String ARG_SIMULATION = "simulation";

    /**
     * When we start Snice, this is the amount of time we will wait for a simulation to
     * be schedule and start running.
     */
    public static final String ARG_WAIT_FOR_SIMULATION = "wait";

    public RuntimeConfig toRuntimeConfig() {
        final var config = new RuntimeConfig();
        config.setRuntimeProvider(namespace.getString(ARG_RUNTIME));
        final var wait = namespace().getInt(ARG_WAIT_FOR_SIMULATION);
        if (wait != null) {
            config.setWait(wait);
        }

        return config;
    }

    public Optional<SimulationConfig> toSimulationConfig() {
        final var simulation = namespace.getString(ARG_SIMULATION);
        if (simulation == null) {
            return Optional.empty();
        }

        final var config = new SimulationConfig();
        config.setSimulation(simulation);
        return Optional.of(config);
    }

    private static ArgumentParser createArgParser() {
        final var parser = ArgumentParsers.newFor("snice").build();
        parser.addArgument("--" + ARG_RUNTIME)
                .help("The fully-qualified class name of the runtime provider")
                .setDefault(DEFAULT_RUNTIME_PROVIDER);

        parser.addArgument("--" + ARG_WAIT_FOR_SIMULATION)
                .help("The amount of time (in seconds) we will wait for a scenario to be scheduled, after which the container will shut down again")
                .type(Integer.TYPE)
                .setDefault(1);

        parser.addArgument("--" + ARG_SIMULATION)
                .help("The fully-qualified class name of the Simulation to run");

        return parser;
    }

    public static CliArgs parseArgs(final String... args) {
        try {
            final var unknownArgs = new ArrayList<String>();
            final var attrs = new HashMap<String, Object>();
            createArgParser().parseKnownArgs(args, unknownArgs, attrs);
            return new CliArgs(args, new Namespace(attrs), unknownArgs);
        } catch (final ArgumentParserException e) {
            throw new RuntimeException(e);
        }
    }
}
