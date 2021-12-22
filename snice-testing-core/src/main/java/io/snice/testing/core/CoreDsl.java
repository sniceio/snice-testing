package io.snice.testing.core;

import io.snice.testing.core.scenario.Scenario;

public final class CoreDsl {

    private CoreDsl() {
        // No instances of this class
    }

    public static Scenario scenario(final String name) {
        return new Scenario(name);
    }

}
