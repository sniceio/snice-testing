package io.snice.testing.core.scenario;

import io.snice.identity.sri.ScenarioResourceIdentifier;
import io.snice.networking.common.ConnectionId;
import io.snice.testing.core.protocol.ProtocolRegistry;

public interface ScenarioContex {

    ScenarioResourceIdentifier scenarioSri();

    ProtocolRegistry registry();

    void onConnectionEvent(ConnectionId id, Object event);
}
