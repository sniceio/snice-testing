package io.snice.testing.http.protocol.impl;

import io.snice.codecs.codec.http.HttpProvider;
import io.snice.identity.sri.ActionResourceIdentifier;
import io.snice.identity.sri.ScenarioResourceIdentifier;
import io.snice.networking.http.impl.NettyHttpMessageFactory;
import io.snice.testing.core.common.Expression;
import io.snice.testing.core.protocol.ProtocolRegistry;
import io.snice.testing.http.HttpConfig;
import io.snice.testing.http.protocol.HttpProtocol;
import io.snice.testing.http.stack.HttpStack;
import io.snice.testing.http.stack.HttpStackUserConfig;
import io.snice.testing.http.stack.impl.SniceHttpStack;

import java.util.Optional;

import static io.snice.preconditions.PreConditions.assertNotNull;

/**
 * Implementation of the {@link HttpProtocol} using Snice Networking's HTTP Stack
 */
public record SniceHttpProtocol(HttpConfig config,
                                ProtocolRegistry.Key key,
                                SniceHttpStack stack,
                                Optional<Expression> baseUrl) implements HttpProtocol {

    public static HttpProtocolBuilder newBuilder(final HttpConfig config) {
        assertNotNull(config);
        return new SniceNetworkingHttpBuilder(config);
    }

    @Override
    public void start() {
        try {
            stack.run(config);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        stack.stop();
    }

    @Override
    public HttpStack newStack(final ScenarioResourceIdentifier scenarioSri, final ActionResourceIdentifier actionSri, final HttpStackUserConfig config) {
        return stack.newStack(scenarioSri, actionSri, config);
    }

    private static class SniceNetworkingHttpBuilder implements HttpProtocolBuilder {

        private final HttpConfig config;
        private Expression baseUrl;

        private SniceNetworkingHttpBuilder(final HttpConfig config) {
            this.config = config;
        }

        @Override
        public SniceHttpProtocol build() {
            final var key = HttpProtocol.httpProtocolKey;
            final var stack = new SniceHttpStack();
            HttpProvider.setMessageFactory(new NettyHttpMessageFactory());
            return new SniceHttpProtocol(config, key, stack, Optional.ofNullable(baseUrl));
        }

        @Override
        public HttpProtocolBuilder baseUrl(final String url) {
            baseUrl = Expression.of(url);
            return this;
        }

        @Override
        public HttpProtocolBuilder auth(final String username, final String password) {
            throw new RuntimeException("Not yet implemeneted");
        }
    }
}
