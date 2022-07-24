package io.snice.testing.runtime.spi;

import io.snice.testing.runtime.CliArgs;
import io.snice.testing.runtime.SniceRuntime;

public interface SniceRuntimeProvider {

    /**
     * Create a new {@link SniceRuntime}
     *
     * @param args the command line arguments as given on the command line (duh). Depending
     *             on the {@link SniceRuntimeProvider}, this information may, or may not, be used.
     * @return
     */
    SniceRuntime create(CliArgs args);
}
