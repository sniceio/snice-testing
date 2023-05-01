package io.snice.testing.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author borjesson.jonas@gmail.com
 */
public class CliArgTest {

    private String[] splitLine(final String line) {
        if (line == null) {
            return new String[0];
        }
        return line.split("\\s+");
    }

    @ParameterizedTest
    @CsvSource({
            "--runtime io.snice.whatever.HelloRuntime --wait 45, io.snice.whatever.HelloRuntime, 45",
            ", io.snice.testing.runtime.impl.SniceLocalDevRuntimeProvider, 1", // default runtime and wait time
            "--wait 12, io.snice.testing.runtime.impl.SniceLocalDevRuntimeProvider, 12",
    })
    public void testCreateArgsForRuntime(final String args, final String expectedRuntimeClass, final int waitTime) {
        final var config = CliArgs.parseArgs(splitLine(args)).toRuntimeConfig();
        assertThat(config.getRuntimeProvider(), is(expectedRuntimeClass));
        assertThat(config.getWait(), is(waitTime));
       
        // no simulation configured
        assertThat(CliArgs.parseArgs().toSimulationConfig(), is(Optional.empty()));
    }

    @ParameterizedTest
    @CsvSource({
            "--simulation com.example.MySimulation, com.example.MySimulation",
    })
    public void testCreateArgsForSimulation(final String args, final String expectedSimulationClass) {
        final var configMaybe = CliArgs.parseArgs(splitLine(args)).toSimulationConfig();
        assertThat(configMaybe.get().getSimulation(), is(expectedSimulationClass));
    }

    @Test
    public void testCreateArgsForNoSimulation() {
        assertThat(CliArgs.parseArgs().toSimulationConfig(), is(Optional.empty()));
    }
}
