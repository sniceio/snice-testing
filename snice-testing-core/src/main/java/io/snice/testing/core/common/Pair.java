package io.snice.testing.core.common;

import static io.snice.preconditions.PreConditions.assertNotNull;

public record Pair<L, R>(L left, R right) {

    public Pair {
        assertNotNull(left);
        assertNotNull(right);
    }
}
