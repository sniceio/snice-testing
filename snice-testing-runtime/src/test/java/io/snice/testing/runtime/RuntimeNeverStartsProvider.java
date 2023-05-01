package io.snice.testing.runtime;

import io.snice.testing.runtime.config.RuntimeConfig;
import io.snice.testing.runtime.spi.SniceRuntimeProvider;

public class RuntimeNeverStartsProvider implements SniceRuntimeProvider {

    @Override
    public SniceRuntime create(final RuntimeConfig confi) {
        return new RuntimeNeverStarts();
    }
}
