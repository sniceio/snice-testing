package io.snice.testing.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.hektor.config.DispatcherConfiguration;
import io.hektor.config.HektorConfiguration;
import io.hektor.config.WorkerThreadExecutorConfig;

import java.util.Map;

public class SniceConfig {

    @JsonProperty("hektor")
    private HektorConfiguration hektorConfig = defaultHektorConfig();

    public HektorConfiguration getHektorConfig() {
        return hektorConfig;
    }

    public void setHektorConfig(final HektorConfiguration hektorConfig) {
        this.hektorConfig = hektorConfig;
    }

    private static HektorConfiguration defaultHektorConfig() {
        final var conf = new HektorConfiguration();

        /*
        hektor:
        dispatchers:
        my-dispatcher:
        executor: worker-thread-executor
        workerThreadExecutor:
        noOfWorkers: 4
        throughput: 75
         */
        final var dispatcherConf = new DispatcherConfiguration.Builder()
                .withExecutor("worker-thread-executor")
                .withThroughput(75)
                .withWorkerThreadExecutor(new WorkerThreadExecutorConfig.Builder().withNoOfWorkers(4).build())
                .build();

        final var dispatchers = Map.of("default-dispatcher", dispatcherConf);
        conf.dispatchers(dispatchers);
        return conf;
    }
}
