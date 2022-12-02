package io.snice.testing.core.scenario;

public class SimulationException extends RuntimeException {

    public SimulationException(final String message) {
        super(message);
    }

    public SimulationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * When loading a {@link Simulation} dynamically, there are many things that can go wrong.
     * The simulation may not be found (wrong name, wrong jar loaded etc), missing default public
     * constructor etc. This exception captures all of those.
     */
    public static class LoadSimulationException extends SimulationException {

        public LoadSimulationException(final String message) {
            super(message);
        }

        public LoadSimulationException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
