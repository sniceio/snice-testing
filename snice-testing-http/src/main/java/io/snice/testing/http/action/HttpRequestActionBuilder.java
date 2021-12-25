package io.snice.testing.http.action;

import io.snice.testing.core.action.Action;
import io.snice.testing.core.action.ActionBuilder;
import io.snice.testing.core.scenario.ScenarioContex;
import io.snice.testing.http.HttpRequestBuilder;
import io.snice.testing.http.protocol.HttpProtocol;

import static io.snice.preconditions.PreConditions.assertNotNull;

public record HttpRequestActionBuilder(HttpRequestBuilder builder) implements ActionBuilder {

    public HttpRequestActionBuilder {
        assertNotNull(builder);
    }

    @Override
    public Action build(final ScenarioContex ctx, final Action next) {
        final var protocol = (HttpProtocol) ctx.registry().protocol(HttpProtocol.httpProtocolKey).orElseThrow(() -> new IllegalArgumentException("HTTP Protocol has not been configured"));
        final var httpRequestDef = builder.build();
        return new HttpRequestAction(httpRequestDef.requestName(), protocol, httpRequestDef, next);
    }
}
