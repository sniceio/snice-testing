package io.snice.testing.runtime;

import io.snice.testing.core.MessageBuilder;
import io.snice.testing.core.action.ActionBuilder;
import io.snice.testing.core.protocol.Protocol;
import io.snice.testing.core.scenario.ExecutionPlan;
import io.snice.testing.core.scenario.Scenario;
import io.snice.testing.runtime.impl.SniceLocalDevRuntimeProvider;
import io.snice.testing.runtime.spi.SniceRuntimeProvider;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

/**
 * The entry point into the runtime environment of Snice Testing. The main purpose is simply to
 * parse the command line arguments, potentially process environment variables and to load the
 * proper services, which will configure themselves and provide the actual runtime.
 */
public class Snice {

    private static final String DEFAULT_RUNTIME_PROVIDER = SniceLocalDevRuntimeProvider.class.getName();

    private final SniceRuntime runtime;

    private Snice(final SniceRuntime runtime) {
        this.runtime = runtime;
    }

    public SniceRuntime runtime() {
        return runtime;
    }

    public void sync() {
        runtime.sync();
    }

    private static ArgumentParser createArgParser() {
        final var parser = ArgumentParsers.newFor("snice").build();
        parser.addArgument("--runtime")
                .help("The fully-qualified class name of the runtime provider")
                .setDefault(DEFAULT_RUNTIME_PROVIDER);

        return parser;
    }

    private static CliArgs parseArgs(final String... args) {
        try {
            final var unknownArgs = new ArrayList<String>();
            final var attrs = new HashMap<String, Object>();
            createArgParser().parseKnownArgs(args, unknownArgs, attrs);
            return new CliArgs(args, new Namespace(attrs), unknownArgs);
        } catch (final ArgumentParserException e) {
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

    public static <T extends ExecutionPlan> CompletionStage<Void> run(final T plan) {
        return start(new String[]{}).runtime().run(plan);
    }

    public static Snice start(final String... args) {
        final var cli = parseArgs(args);
        final var runtimeProviderClass = cli.namespace().get("runtime");
        final var runtime = ServiceLoader.load(SniceRuntimeProvider.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .filter(p -> runtimeProviderClass.equals(p.getClass().getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unable to load an appropriate SniceRuntime given " +
                        "the supplied SniceRuntimeProvider of \"" + runtimeProviderClass + "\". " +
                        "Check your CLI argument --runtime to ensure you've specified the fully-qualified " +
                        "java class name of the provider"))
                .create(cli);

        final var startFuture = runtime.start();
        try {
            startFuture.get();
        } catch (final InterruptedException e) {
            e.printStackTrace();
        } catch (final ExecutionException e) {
            e.printStackTrace();
        }

        final var snice = new Snice(runtime);
        return snice;
    }

    public static void main(final String... args) {
        start(args).sync();
    }
}
