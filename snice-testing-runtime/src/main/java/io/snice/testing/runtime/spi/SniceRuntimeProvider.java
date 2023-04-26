package io.snice.testing.runtime.spi;

import io.snice.testing.runtime.SniceRuntime;
import io.snice.testing.runtime.config.RuntimeConfig;

public interface SniceRuntimeProvider {

    /**
     * Create a new {@link SniceRuntime}
     */
    SniceRuntime create(RuntimeConfig config);
}
