package io.snice.testing.http.action;

import io.snice.identity.sri.ActionResourceIdentifier;
import io.snice.testing.core.action.Action;
import io.snice.testing.core.action.ActionBuilder;
import io.snice.testing.core.scenario.ScenarioContex;
import io.snice.testing.http.AcceptHttpRequestBuilder;
import io.snice.testing.http.protocol.HttpProtocol;

import java.util.Map;

import static io.snice.preconditions.PreConditions.assertNotNull;

public record AcceptHttpRequestActionBuilder(AcceptHttpRequestBuilder builder) implements ActionBuilder {

    public AcceptHttpRequestActionBuilder {
        assertNotNull(builder);
    }

    @Override
    public Action build(final ActionResourceIdentifier sri, final ScenarioContex ctx, final Action next) {
        final var protocol = (HttpProtocol) ctx.registry().protocol(HttpProtocol.httpProtocolKey).orElseThrow(() -> new IllegalArgumentException("HTTP Protocol has not been configured"));
        final var def = builder.build();
        final var stack = protocol.newStack(def.config());

        // TODO: need to consult the IpProvider in case we need a public facing address
        final Map<String, Object> attributes = Map.of(def.saveAs(), stack.address());
        return new AcceptHttpRequestAction(def.requestName(), sri, stack, def, attributes, next);
    }
}
