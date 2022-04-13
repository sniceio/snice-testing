package io.snice.testing.http.action;

import io.snice.testing.core.action.Action;
import io.snice.testing.core.action.ActionBuilder;
import io.snice.testing.core.scenario.ScenarioContex;
import io.snice.testing.http.AcceptHttpRequestBuilder;
import io.snice.testing.http.protocol.HttpProtocol;

import static io.snice.preconditions.PreConditions.assertNotNull;

public record AcceptHttpRequestActionBuilder(AcceptHttpRequestBuilder builder) implements ActionBuilder {

    public AcceptHttpRequestActionBuilder {
        assertNotNull(builder);
    }

    @Override
    public Action build(final ScenarioContex ctx, final Action next) {
        final var protocol = (HttpProtocol) ctx.registry().protocol(HttpProtocol.httpProtocolKey).orElseThrow(() -> new IllegalArgumentException("HTTP Protocol has not been configured"));
        // TODO: need to ensure that we are listening, get the address and insert it into the Session
        // Note: perhaps we insert the "listening address", which may need to be a public addressable
        // address and as such, we need to grab that one from the IpProvider into the builder
        // since the final "web hook" URL is dependent on potentially other session variables. Or should we allow
        // the path actually be dynamic? Are we complicating things? Would be nice if we could, when the scenario
        // starts, check all "accepts" and allocate those addresses right there and then? So before we
        // get to this ActionBuilder.build method.
        System.err.println("Need to figure shit out here");
        final var def = builder.build();
        return new AcceptHttpRequestAction(def.requestName(), protocol, def, next);
    }
}
