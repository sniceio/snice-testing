package io.snice.testing.runtime;

import io.snice.testing.runtime.impl.SniceLocalDevRuntimeProvider;
import io.snice.testing.runtime.spi.SniceRuntimeProvider;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutionException;

/**
 * The entry point into the runtime environment of Snice Testing.
 */
public final class Snice {

    private static final String DEFAULT_RUNTIME_PROVIDER = SniceLocalDevRuntimeProvider.class.getName();

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

    public static void main(final String... args) {
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

        final var shutdownFuture = runtime.start();
        try {
            shutdownFuture.get();
        } catch (final InterruptedException e) {
            e.printStackTrace();
        } catch (final ExecutionException e) {
            e.printStackTrace();
        }

    }
}
