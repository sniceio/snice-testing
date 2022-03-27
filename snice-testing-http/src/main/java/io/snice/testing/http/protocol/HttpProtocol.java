package io.snice.testing.http.protocol;

import io.snice.testing.core.common.Expression;
import io.snice.testing.core.protocol.Protocol;
import io.snice.testing.core.protocol.ProtocolRegistry;
import io.snice.testing.http.HttpConfig;
import io.snice.testing.http.protocol.impl.SniceHttpProtocol;
import io.snice.testing.http.stack.HttpStack;

import java.util.Optional;

import static io.snice.preconditions.PreConditions.assertNotNull;

public interface HttpProtocol extends Protocol {

    ProtocolRegistry.Key httpProtocolKey = Protocol.createKey(HttpProtocol.class);

    static HttpProtocolBuilder from(final HttpConfig config) {
        assertNotNull(config);
        return SniceHttpProtocol.newBuilder(config);
    }

    HttpStack stack();

    HttpConfig config();

    Optional<Expression> baseUrl();

    interface HttpProtocolBuilder extends Protocol.Builder {
        HttpProtocolBuilder baseUrl(final String url);
    }

}
