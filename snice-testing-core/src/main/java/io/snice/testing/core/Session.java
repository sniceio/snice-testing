package io.snice.testing.core;

import io.snice.testing.core.check.CheckResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.snice.preconditions.PreConditions.assertNotEmpty;
import static io.snice.preconditions.PreConditions.assertNotNull;

public record Session(String name,
                      Status status,
                      Map<String, Object> attributes,
                      List<CheckResult<?, ?>> checkResults) {


    public Session {
        assertNotEmpty(name);
        assertNotNull(status);
        assertNotNull(attributes);
    }

    public Session(final String name) {
        this(name, Status.OK, Map.of(), List.of());
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

        return new Session(name, Status.KO, attributes, checkResults);
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

        return new Session(name, Status.OK, attributes, checkResults);
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

    public Session attributes(final String name, final Object value) {
        return new Session(name, status, extendAttributes(name, value), checkResults);
    }

    private Map<String, Object> extendAttributes(final String name, final Object value) {
        final var newAttributes = new HashMap<>(attributes);
        newAttributes.put(name, value);
        return newAttributes;
    }

    public Session processCheckResult(final CheckResult<?, ?> result) {
        assertNotNull(result);
        final var newCheckResults = new ArrayList<>(checkResults);
        newCheckResults.add(result);

        final var newAttributes =
                result.isSuccess() && result.saveAs().isPresent() && result.extractedValue().isPresent() ?
                        extendAttributes(result.saveAs().get(), result.extractedValue().get()) :
                        attributes;

        final var newStatus = result.isFailure() ? Status.KO : status;
        return new Session(name, newStatus, newAttributes, newCheckResults);
    }

    enum Status {
        OK, KO;
    }

}
