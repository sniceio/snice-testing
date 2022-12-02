package io.snice.testing.runtime;

import io.snice.testing.core.scenario.Simulation;
import io.snice.testing.core.scenario.SimulationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SniceTest {

    @Test
    public void loadRuntime() {
        final var snice = Snice.start("--runtime", FakeRuntimeProvider.class.getName());
        assertThat(snice, notNullValue());
    }

    /**
     * Ensure that we will properly bail out of starting Snice if the given Simulation turns out to be bad
     * in any way...
     */
    @ParameterizedTest
    @CsvSource({"io.snice.does.not.exist.MySimulation, Unknown Simulation \"io.snice.does.not.exist.MySimulation\"",
            "io.snice.testing.runtime.SniceTest$MyBadSimulation, Missing default public no-arg constructor for Simulation io.snice.testing.runtime.SniceTest$MyBadSimulation",
            "io.snice.testing.runtime.SniceTest$MyBadSimulation2, Missing default public no-arg constructor for Simulation io.snice.testing.runtime.SniceTest$MyBadSimulation2"
    })
    public void testLoadBadSimulation(final String badClass, final String expectedErrorMessage) {
        final var e = assertThrows(SimulationException.LoadSimulationException.class, () ->
                Snice.start("--simulation", badClass));

        assertThat(e.getMessage(), is(expectedErrorMessage));
    }

    @Test
    public void testRunSimulationFromTheGetGo() {
        Snice.start("--runtime", FakeRuntimeProvider.class.getName(), "--simulation", MySimulation.class.getName());
        final var sims = FakeRuntimeProvider.runtime.simulations;
        assertThat(sims.size(), is(1));
        assertThat(sims.get(0).simulation() instanceof MySimulation, is(true));
    }

    /**
     * A good simulation that can be loaded. Not that it does much but other tests will test
     * kicking things off etc.
     */
    public static class MySimulation extends Simulation {

    }


    /**
     * No no-arg public constructor - no-go
     */
    public static class MyBadSimulation extends Simulation {
        public MyBadSimulation(final String ops) {

        }
    }

    /**
     * Private constructor, no-go...
     */
    public static class MyBadSimulation2 extends Simulation {
        private MyBadSimulation2() {

        }
    }

}