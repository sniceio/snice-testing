package io.snice.testing.http;


import io.snice.networking.common.NetworkingUtils;
import io.snice.networking.common.Transport;
import io.snice.networking.config.NetworkInterfaceConfiguration;
import io.snice.testing.core.SniceConfig;
import io.snice.testing.http.check.HttpCheckSupport;
import io.snice.testing.http.protocol.HttpProtocol;
import io.snice.testing.http.protocol.HttpProtocol.HttpProtocolBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static io.snice.preconditions.PreConditions.ensureNotEmpty;

/**
 * Functions as a simple DSL to "kick-start" the creation of various HTTP
 * related objects, such as requests, configure the HTTP stack etc.
 */
public class HttpDsl extends HttpCheckSupport {

    private HttpDsl() {
        // No instantiation of this class
    }

    public static HttpProtocolBuilder http(final SniceConfig configuration) {
        // TODO: how do you turn the SniceConfig into a http config?
        try {
            final var ip = NetworkingUtils.findPrimaryAddress().getHostAddress();
            final var listen = new URI("https://" + ip + ":7777");
            final var lp = new NetworkInterfaceConfiguration("default", listen, null, Transport.tcp);
            final var httpConfig = new HttpConfig();
            httpConfig.setNetworkInterfaces(List.of(lp));

            return HttpProtocol.from(httpConfig);
        } catch (final URISyntaxException e) {
            // TODO
            throw new RuntimeException(e);
        }
    }

    public static HttpRequestBuilder http(final String requestName) {
        ensureNotEmpty(requestName, "The name of the HTTP request cannot be empty");
        return HttpRequestDef.of(requestName);
    }

}
