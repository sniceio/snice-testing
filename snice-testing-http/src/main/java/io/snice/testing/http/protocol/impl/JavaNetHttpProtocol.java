package io.snice.testing.http.protocol.impl;

import io.snice.codecs.codec.http.HttpProvider;
import io.snice.preconditions.PreConditions;
import io.snice.testing.core.common.Expression;
import io.snice.testing.core.protocol.ProtocolRegistry;
import io.snice.testing.http.HttpConfig;
import io.snice.testing.http.codec.JavaNetHttpMessageFactory;
import io.snice.testing.http.protocol.HttpProtocol;
import io.snice.testing.http.stack.HttpStack;
import io.snice.testing.http.stack.HttpStackUserConfig;
import io.snice.testing.http.stack.impl.JavaNetHttpStack;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;

public record JavaNetHttpProtocol(HttpConfig config,
                                  ProtocolRegistry.Key key,
                                  JavaNetHttpStack stack,
                                  Optional<Expression> baseUrl) implements HttpProtocol {

    public static HttpProtocolBuilder newBuilder(final HttpConfig config) {
        PreConditions.assertNotNull(config);
        return new JavaNetBuilder(config);
    }

    @Override
    public void start() {
        stack.start();
    }

    @Override
    public void stop() {
        stack.stop();
    }

    @Override
    public HttpStack newStack(final HttpStackUserConfig config) {
        // This implementation doesn't handle this properly and was only used for early testing.
        // Don't use in production.
        return stack;
    }

    private static class JavaNetBuilder implements HttpProtocolBuilder {

        private final HttpConfig config;
        private Expression baseUrl;

        private JavaNetBuilder(final HttpConfig config) {
            this.config = config;
        }

        @Override
        public JavaNetHttpProtocol build() {
            final var key = HttpProtocol.httpProtocolKey;
            final var httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            final var stack = new JavaNetHttpStack(httpClient);
            final var messageFactory = new JavaNetHttpMessageFactory();
            HttpProvider.setMessageFactory(messageFactory);
            return new JavaNetHttpProtocol(config, key, stack, Optional.ofNullable(baseUrl));
        }

        @Override
        public HttpProtocolBuilder baseUrl(final String url) {
            baseUrl = Expression.of(url);
            return this;
        }
    }
}
