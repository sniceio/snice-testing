package io.snice.testing.http.protocol;

import io.snice.testing.core.expression.Expression;
import io.snice.testing.core.protocol.Protocol;
import io.snice.testing.core.protocol.ProtocolRegistry;
import io.snice.testing.http.HttpConfig;
import io.snice.testing.http.protocol.jnet.JavaNetHttpProtocol;
import io.snice.testing.http.stack.HttpStack;

import java.util.Optional;

import static io.snice.preconditions.PreConditions.assertNotNull;

public interface HttpProtocol extends Protocol {

    ProtocolRegistry.Key httpProtocolKey = Protocol.createKey(HttpProtocol.class);

    static Builder from(final HttpConfig config) {
        assertNotNull(config);
        return JavaNetHttpProtocol.newBuilder(config);
    }

    HttpStack stack();

    HttpConfig config();

    Optional<Expression> baseUrl();

    interface Builder extends Protocol.Builder {
        Builder baseUrl(final String url);
    }

}
