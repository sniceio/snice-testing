package io.snice.testing.core.scenario;

import io.snice.testing.core.protocol.Protocol;
import io.snice.testing.core.protocol.ProtocolRegistry;

import java.util.Optional;

public class DefaultScenarioContext implements ScenarioContex, ProtocolRegistry {

    private final ProtocolRegistry actualRegistry;

    public DefaultScenarioContext(final ProtocolRegistry actualRegistry) {
        this.actualRegistry = actualRegistry;
    }

    @Override
    public ProtocolRegistry registry() {
        return this;
    }

    /**
     * Trap all queries to the protocol registry
     */
    @Override
    public <T extends Protocol> Optional<T> protocol(final Key key) {
        System.err.println("Looking up " + key);
        return actualRegistry.protocol(key);
    }
}
