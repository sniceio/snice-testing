package io.snice.testing.runtime;

import io.snice.testing.runtime.spi.SniceRuntimeProvider;

public class FakeRuntimeProvider implements SniceRuntimeProvider {

    /**
     * Ugly but this is for unit testing and we need to get hold of the runtime and
     * since all is loaded dynamically, we'll do it this way.
     */
    public static final FakeRuntime runtime = new FakeRuntime();

    @Override
    public SniceRuntime create(final CliArgs args) {
        return runtime;
    }
}
