package io.snice.testing.core;

import io.snice.identity.sri.SessionResourceIdentifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.snice.preconditions.PreConditions.assertNotEmpty;
import static io.snice.preconditions.PreConditions.assertNotNull;

public record Session(SessionResourceIdentifier uuid,
                      String name,
                      Status status,
                      Map<String, Object> attributes,
                      Map<String, Object> environment) {


    public Session {
        assertNotNull(uuid);
        assertNotEmpty(name);
        assertNotNull(status);
        assertNotNull(attributes);
        assertNotNull(environment);
    }

    public Session(final String name) {
        this(SessionResourceIdentifier.of(), name, Status.OK, Map.of(), Map.of());
    }

    public boolean isFailed() {
        return status() == Status.KO;
    }

    public boolean isSucceeded() {
        return status() == Status.OK;
    }

    /**
     * Mark the session as not OK.
     *
     * @return a new instance of {@link Session} where the status has been set to not ok
     */
    public Session markAsFailed() {
        if (isFailed()) {
            return this;
        }

        return new Session(uuid, name, Status.KO, attributes, environment);
    }

    /**
     * Mark the session as OK.
     *
     * @return a new instance of {@link Session} where the status has been set to ok
     */
    public Session markAsSucceeded() {
        if (isSucceeded()) {
            return this;
        }

        return new Session(uuid, name, Status.OK, attributes, environment);
    }


    /**
     * Retrieve a given attribute based on the name under which it was stored.
     *
     * @param name the name of the attribute
     * @return an {@link Optional} containing the value of the attribute if it exists, otherwise
     * an empty {@link Optional} will be returned.
     */
    public Optional<Object> attributes(final String name) {
        return Optional.ofNullable(attributes.get(name));
    }

    /**
     * Retrieve a given environment variable based on the name under which it was stored.
     *
     * @param name the name of the environment variable
     * @return an {@link Optional} containing the value of the environment variable if it exists, otherwise
     * an empty {@link Optional} will be returned.
     */
    public Optional<Object> environment(final String name) {
        return Optional.ofNullable(environment.get(name));
    }

    /**
     * Update the {@link Session} with all the given attributes.
     */
    public Session attributes(final Map<String, Object> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return this;
        }

        final var extended = extendMap(this.attributes, attributes);
        return new Session(uuid, name, status, extended, environment);
    }

    /**
     * Update the {@link Session} with all the given environmental variables.
     */
    public Session environment(final Map<String, String> environment) {
        if (environment == null || environment.isEmpty()) {
            return this;
        }

        final var extended = extendMap(this.environment, environment);
        return new Session(uuid, name, status, attributes, extended);
    }

    public Session attributes(final String name, final Object value) {
        return new Session(uuid, name, status, extendAttributes(name, value), environment);
    }

    public Session environment(final String name, final Object value) {
        return new Session(uuid, name, status, attributes, extendEnvironment(name, value));
    }

    private Map<String, Object> extendAttributes(final String name, final Object value) {
        return extendMap(attributes, name, value);
    }

    private Map<String, Object> extendEnvironment(final String name, final Object value) {
        return extendMap(environment, name, value);
    }

    private Map<String, Object> extendMap(final Map<String, Object> map, final String name, final Object value) {
        final var newMap = new HashMap<>(map);
        newMap.put(name, value);
        return newMap;
    }

    private Map<String, Object> extendMap(final Map<String, Object> map, final Map<String, ? extends Object> additionalValues) {
        final var newMap = new HashMap<>(map);
        newMap.putAll(additionalValues);
        return newMap;
    }

    enum Status {
        OK, KO;
    }

}
