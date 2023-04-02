package io.snice.testing.runtime.impl;

import io.hektor.config.DispatcherConfiguration;
import io.hektor.config.HektorConfiguration;
import io.hektor.config.WorkerThreadExecutorConfig;
import io.hektor.core.Hektor;
import io.snice.networking.common.docker.DockerSupport;
import io.snice.testing.runtime.CliArgs;
import io.snice.testing.runtime.Snice;
import io.snice.testing.runtime.SniceRuntime;
import io.snice.testing.runtime.spi.SniceRuntimeProvider;

import java.util.Map;

public class SniceLocalDevRuntimeProvider implements SniceRuntimeProvider {

    @Override
    public SniceRuntime create(final CliArgs args) {
        // This could return null so that's why we check later because the implicit conversion
        // from Integer to int would blow up on NPE
        final var waitTime = args.namespace().getInt(Snice.ARG_WAIT_FOR_SIMULATION);
        final var hektor = Hektor.withName("Snice").withConfiguration(defaultHektorConfig()).build();
        final var dockerSupport = DockerSupport.of().withReadFromSystemProperties().build();
        return new SniceLocalDevRuntime(waitTime != null ? waitTime : 1, hektor, dockerSupport);
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
