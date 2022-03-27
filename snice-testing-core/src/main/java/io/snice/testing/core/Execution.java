package io.snice.testing.core;

import io.snice.testing.core.check.CheckResult;
import io.snice.testing.core.scenario.Scenario;

import java.util.List;

import static io.snice.preconditions.PreConditions.assertNotEmpty;

/**
 * For every {@link Scenario} exec that is executed, the result, logs, metrics etc etc is all
 * saved in an {@link Execution} and propagated along the chain of executions and eventually
 * collected and processed.
 */
public record Execution<T extends Object>(String name,
                                          boolean success,
                                          List<CheckResult<T, ?>> checkResults) {
    public Execution {
        assertNotEmpty(name);
        checkResults = checkResults == null ? List.of() : checkResults;
    }

    public Execution(final String name, final boolean success) {
        this(name, success, List.of());
    }
}
