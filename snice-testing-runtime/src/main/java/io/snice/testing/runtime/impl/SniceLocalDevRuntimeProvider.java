package io.snice.testing.runtime.impl;

import io.hektor.config.DispatcherConfiguration;
import io.hektor.config.HektorConfiguration;
import io.hektor.config.WorkerThreadExecutorConfig;
import io.hektor.core.Hektor;
import io.snice.networking.common.docker.DockerSupport;
import io.snice.testing.runtime.CliArgs;
import io.snice.testing.runtime.SniceRuntime;
import io.snice.testing.runtime.spi.SniceRuntimeProvider;

import java.util.Map;

public class SniceLocalDevRuntimeProvider implements SniceRuntimeProvider {

    @Override
    public SniceRuntime create(final CliArgs args) {
        final var hektor = Hektor.withName("Snice").withConfiguration(defaultHektorConfig()).build();
        final var dockerSupport = DockerSupport.of().withReadFromSystemProperties().build();
        return new SniceLocalDevRuntime(hektor, dockerSupport);
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
