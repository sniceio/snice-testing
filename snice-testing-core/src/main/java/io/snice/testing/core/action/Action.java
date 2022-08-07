package io.snice.testing.core.action;

import io.snice.identity.sri.ActionResourceIdentifier;
import io.snice.testing.core.Execution;
import io.snice.testing.core.MessageBuilder;
import io.snice.testing.core.Session;
import io.snice.testing.core.protocol.Protocol;
import io.snice.testing.core.protocol.ProtocolRegistry;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface Action {

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

    /**
     * The name of this {@link Action}.
     */
    String name();

    /**
     * Every {@link Action} has a unique resource identifier associated with it.
     *
     * @return
     */
    ActionResourceIdentifier sri();

    /**
     * An {@link Action} may have attributes that should be updated on the {@link Session} before the
     * action is actually executed. The execution environment will, as soon as the {@link Action} has been built,
     * update the {@link Session} with these attributes so that e.g. an async action that listens to a particular
     * address, that address is made available to other actions after it (needed to make webhooks work e.g.)
     *
     * @return
     */
    default Map<String, Object> attributes() {
        return Map.of();
    }

    /**
     * Execute this action.
     */
    void execute(List<Execution> executions, Session session);
}
