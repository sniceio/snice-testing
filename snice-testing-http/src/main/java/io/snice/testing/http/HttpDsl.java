package io.snice.testing.http;


import io.snice.testing.core.SniceConfig;
import io.snice.testing.http.check.HttpCheckSupport;
import io.snice.testing.http.protocol.HttpProtocol;
import io.snice.testing.http.protocol.HttpProtocol.Builder;
import io.snice.testing.http.stack.HttpStackConfig;

import static io.snice.preconditions.PreConditions.ensureNotEmpty;

/**
 * Functions as a simple DSL to "kick-start" the creation of various HTTP
 * related objects, such as requests, configure the HTTP stack etc.
 */
public class HttpDsl extends HttpCheckSupport {

    private HttpDsl() {
        // No instantiation of this class
    }

    public static Builder http(final SniceConfig configuration) {
        // TODO: how do you turn the SniceConfig into a http config?
        final var httpConfig = new HttpConfig(new HttpStackConfig());
        return HttpProtocol.from(httpConfig);
    }

    public static HttpRequestBuilder http(final String requestName) {
        ensureNotEmpty(requestName, "The name of the HTTP request cannot be empty");
        return HttpRequestDef.of(requestName);
    }

}
