package io.snice.testing.core.action;

import io.snice.identity.sri.ActionResourceIdentifier;
import io.snice.testing.core.MessageBuilder;
import io.snice.testing.core.Session;
import io.snice.testing.core.protocol.Protocol;
import io.snice.testing.core.protocol.ProtocolRegistry;
import io.snice.testing.core.scenario.ScenarioContex;

import java.util.Optional;

public interface ActionBuilder {

    /**
     * The {@link Protocol} that may be needed to execute the action.
     * <p>
     * In plain English, if the resulting action is about sending (or receiving) an HTTP
     * request we will need an HTTP stack to be available. However, there are actions
     * that just operate e.g. on a {@link Session} and they do not need any particular
     * protocols to be present, which is why this is an {@link Optional}.
     * <p>
     * Compare this with {@link MessageBuilder#protocol()} where it is not optional since
     * building that builder will result in a request/response of some sort, which will always
     * require a protocol (HTTP, SIP etc) to be present.
     *
     * @return
     */
    Optional<ProtocolRegistry.Key> protocol();

    Action build(ActionResourceIdentifier sri, ScenarioContex ctx, Action next);
}
