package io.snice.testing.runtime.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.snice.testing.runtime.CliArgs;

public class RuntimeConfig {

    @JsonProperty
    private String runtimeProvider = CliArgs.DEFAULT_RUNTIME_PROVIDER;

    @JsonProperty
    private int wait = 1;

    public String getRuntimeProvider() {
        return runtimeProvider;
    }

    public void setRuntimeProvider(final String runtimeProvider) {
        this.runtimeProvider = runtimeProvider;
    }

    public int getWait() {
        return wait;
    }

    public void setWait(final int wait) {
        if (wait < 0) {
            return;
        }

        this.wait = wait;
    }
}
