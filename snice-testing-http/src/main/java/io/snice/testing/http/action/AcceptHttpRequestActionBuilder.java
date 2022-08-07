package io.snice.testing.http.action;

import io.snice.identity.sri.ActionResourceIdentifier;
import io.snice.testing.core.action.Action;
import io.snice.testing.core.action.ActionBuilder;
import io.snice.testing.core.protocol.ProtocolRegistry;
import io.snice.testing.core.scenario.ScenarioContex;
import io.snice.testing.http.AcceptHttpRequestBuilder;
import io.snice.testing.http.protocol.HttpProtocol;

import java.util.Map;
import java.util.Optional;

import static io.snice.preconditions.PreConditions.assertNotNull;

public record AcceptHttpRequestActionBuilder(AcceptHttpRequestBuilder builder) implements ActionBuilder {

    @Override
    public Optional<ProtocolRegistry.Key> protocol() {
        return Optional.of(HttpProtocol.httpProtocolKey);
    }

    public AcceptHttpRequestActionBuilder {
        assertNotNull(builder);
    }

    @Override
    public Action build(final ActionResourceIdentifier sri, final ScenarioContex ctx, final Action next) {
        final var protocol = (HttpProtocol) ctx.registry().protocol(HttpProtocol.httpProtocolKey).orElseThrow(() -> new IllegalArgumentException("HTTP Protocol has not been configured"));
        final var def = builder.build();
        final var stack = protocol.newStack(sri, def.config());

        // TODO: need to consult the IpProvider in case we need a public facing address
        final Map<String, Object> attributes = Map.of(def.saveAs(), stack.address());

        // TODO: here we need to
        return new AcceptHttpRequestAction(def.requestName(), sri, stack, def, attributes, next);
    }
}
