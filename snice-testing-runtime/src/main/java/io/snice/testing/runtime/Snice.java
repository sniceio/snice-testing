package io.snice.testing.runtime;

import io.snice.testing.core.MessageBuilder;
import io.snice.testing.core.action.ActionBuilder;
import io.snice.testing.core.protocol.Protocol;
import io.snice.testing.core.scenario.Scenario;
import io.snice.testing.core.scenario.Simulation;
import io.snice.testing.core.scenario.SimulationException;
import io.snice.testing.runtime.config.RuntimeConfig;
import io.snice.testing.runtime.config.SimulationConfig;
import io.snice.testing.runtime.spi.SniceRuntimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

/**
 * The entry point into the runtime environment of Snice Testing. The main purpose is simply to
 * parse the command line arguments, potentially process environment variables and to load the
 * proper services, which will configure themselves and provide the actual runtime.
 */
public class Snice {

    private static final Logger logger = LoggerFactory.getLogger(Snice.class);

    private final SniceRuntime runtime;

    private Snice(final SniceRuntime runtime) {
        this.runtime = runtime;
    }

    public SniceRuntime runtime() {
        return runtime;
    }

    public void sync() {
        try {
            runtime.sync().toCompletableFuture().get();
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        } catch (final ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Convenience method for kicking off a single action. Mainly used for simple test cases
     * and demos.
     */
    public static CompletionStage<Void> run(final ActionBuilder builder) {
        return start(new String[]{}).runtime().run(builder);
    }

    /**
     * Convenience method for kicking off a test with a single message to send. Mainly used for simple test cases
     * and demos.
     */
    public static CompletionStage<Void> run(final MessageBuilder... message) {
        return start(new String[]{}).runtime().run(message);
    }


    public static CompletionStage<Void> run(final Scenario scenario, final Protocol.Builder... protocols) {
        return start(new String[]{}).runtime().run(scenario, protocols);
    }

    public static CompletionStage<Void> run(final Scenario scenario, final List<Protocol> protocols) {
        return start(new String[]{}).runtime().run(scenario, protocols);
    }

    public static <T extends Simulation> CompletionStage<Void> run(final T plan) {
        return start(new String[]{}).runtime().run(plan);
    }

    /**
     * Parse the command line arguments and bootstrap the runtime system. Optionally, load and kick-off
     * any {@link Simulation}s that may have been given on the command line.
     *
     * @param args the command line arguments
     * @return an instance of {@link Snice} with a loaded and started {@link Runtime}. It is guaranteed that
     * the {@link Runtime} has successfully been started before returning. If not, we will bail out.
     * @throws SimulationException.LoadSimulationException if a {@link Simulation} has been specified on the command
     *                                                     line and we are unable to load it (for whatever reason),
     *                                                     we'll bail out and the system will grind to a halt.
     */
    public static Snice start(final String... args) throws SimulationException.LoadSimulationException {
        final var cli = CliArgs.parseArgs(args);
        final var runtimeConfig = cli.toRuntimeConfig();
        final var simulationConfig = cli.toSimulationConfig();
        return start(runtimeConfig, simulationConfig);
    }

    public static Snice start(final RuntimeConfig runtimeConfig, final Optional<SimulationConfig> simulationConfig) throws SimulationException.LoadSimulationException {
        final Optional<Simulation> simulationMaybe = loadSimulation(simulationConfig);

        final var runtimeProviderClass = runtimeConfig.getRuntimeProvider();
        final var runtime = ServiceLoader.load(SniceRuntimeProvider.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .filter(p -> runtimeProviderClass.equals(p.getClass().getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unable to load an appropriate SniceRuntime given " +
                        "the supplied SniceRuntimeProvider of \"" + runtimeProviderClass + "\". " +
                        "Check your CLI argument --runtime to ensure you've specified the fully-qualified " +
                        "java class name of the provider"))
                .create(runtimeConfig);

        final var startFuture = runtime.start();
        try {
            startFuture.get();
        } catch (final InterruptedException e) {
            e.printStackTrace();
        } catch (final ExecutionException e) {
            e.printStackTrace();
        }

        final var snice = new Snice(runtime);
        simulationMaybe.ifPresent(sim -> snice.runtime().run(sim));
        return snice;

    }

    private static <T extends Simulation> Optional<T> loadSimulation(final Optional<SimulationConfig> config) throws SimulationException.LoadSimulationException {
        return config.map(SimulationConfig::getSimulation)
                .or(Snice::findSimulationAutomatically)
                .map(Snice::loadSimulationClass);
    }

    private static <T extends Simulation> T loadSimulationClass(final String className) {
        try {
            final var clazz = (Class<T>) Class.forName(className);
            final var constructor = clazz.getConstructor(null);
            return constructor.newInstance(null);
        } catch (final ClassCastException e) {
            throw new SimulationException.LoadSimulationException("The class \"" + className + "\" is not of type Simulation", e);
        } catch (final ClassNotFoundException e) {
            throw new SimulationException.LoadSimulationException("Unknown Simulation \"" + className + "\"", e);
        } catch (final NoSuchMethodException e) {
            throw new SimulationException.LoadSimulationException("Missing default public no-arg constructor for Simulation " + className, e);
        } catch (final InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new SimulationException.LoadSimulationException("Unable to load Simulation " + className, e);
        }
    }

    /**
     * Overly simplified but good enough for now. It tries to find the main class, assuming it is also
     * a {@link Simulation}, which it currently doesn't check but that'll blow up in the {@link #loadSimulation(Optional)}
     * so the user would know.
     *
     * @return
     */
    private static Optional<String> findSimulationAutomatically() {
        final var stack = Thread.currentThread().getStackTrace();
        final var mainMaybe = stack[stack.length - 1];
        final var className = mainMaybe.getClassName();
        try {
            loadSimulationClass(className);
        } catch (final SimulationException.LoadSimulationException e) {
            // doesn't implement Simulation. Ignore and move on.
            return Optional.empty();
        }
        return Optional.of(className);
    }

    public static void main(final String... args) throws Exception {
        start(args).sync();
    }
}
