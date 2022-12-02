package io.snice.testing.core.scenario;

import io.hektor.core.ActorRef;
import io.snice.identity.sri.ScenarioResourceIdentifier;
import io.snice.networking.common.ConnectionId;
import io.snice.testing.core.protocol.Protocol;
import io.snice.testing.core.protocol.ProtocolRegistry;

import java.util.Optional;

public class DefaultScenarioContext implements ScenarioContex, ProtocolRegistry {

    private final ActorRef actor;
    private final ScenarioResourceIdentifier sri;
    private final ProtocolRegistry actualRegistry;

    public DefaultScenarioContext(final ActorRef actor, final ScenarioResourceIdentifier sri, final ProtocolRegistry actualRegistry) {
        this.actor = actor;
        this.sri = sri;
        this.actualRegistry = actualRegistry;
    }

    @Override
    public ScenarioResourceIdentifier scenarioSri() {
        return sri;
    }

    @Override
    public ProtocolRegistry registry() {
        return this;
    }

    @Override
    public void onConnectionEvent(final ConnectionId id, final Object event) {
        actor.tell(event);
    }

    /**
     * Trap all queries to the protocol registry
     */
    @Override
    public <T extends Protocol> Optional<T> protocol(final Key key) {
        return actualRegistry.protocol(key);
    }
}
