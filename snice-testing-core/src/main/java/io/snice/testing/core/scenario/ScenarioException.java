package io.snice.testing.core.scenario;

public class ScenarioException extends RuntimeException {

    public ScenarioException(final String message) {
        super(message);
    }

    public ScenarioException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public static class NoSuchAttributeException extends ScenarioException {

        public NoSuchAttributeException(final String message) {
            super(message);
        }

        public NoSuchAttributeException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
