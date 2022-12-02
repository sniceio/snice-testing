package io.snice.testing.runtime;

import io.snice.testing.runtime.spi.SniceRuntimeProvider;

public class RuntimeNeverStartsProvider implements SniceRuntimeProvider {

    @Override
    public SniceRuntime create(final CliArgs args) {
        return new RuntimeNeverStarts();
    }
}
