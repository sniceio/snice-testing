package io.snice.testing.core;

import java.util.Optional;

public interface Session {

    /**
     * The name of the current {@link io.snice.testing.core.scenario.Scenario}
     */
    String name();

    Status status();

    /**
     * Retrieve a given attribute based on the name under which it was stored.
     *
     * @param name the name of the attribute
     * @return an {@link Optional} containing the value of the attribute if it exists, otherwise
     * an empty {@link Optional} will be returned.
     */
    Optional<Object> attributes(String name);

    enum Status {
        OK, KO;
    }


}
