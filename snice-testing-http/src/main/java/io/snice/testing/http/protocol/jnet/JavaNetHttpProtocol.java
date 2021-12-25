package io.snice.testing.http.protocol.jnet;

import io.snice.preconditions.PreConditions;
import io.snice.testing.core.expression.Expression;
import io.snice.testing.core.protocol.ProtocolRegistry;
import io.snice.testing.http.HttpConfig;
import io.snice.testing.http.protocol.HttpProtocol;
import io.snice.testing.http.stack.jnet.JavaNetHttpStack;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;

public record JavaNetHttpProtocol(HttpConfig config,
                                  ProtocolRegistry.Key key,
                                  JavaNetHttpStack stack,
                                  Optional<Expression> baseUrl) implements HttpProtocol {

    public static Builder newBuilder(final HttpConfig config) {
        PreConditions.assertNotNull(config);
        return new JavaNetBuilder(config);
    }

    @Override
    public void start() {
        System.err.println("HTTP Protocol Starting");
        stack.start();
    }

    @Override
    public void stop() {
        System.err.println("HTTP Protocol Stopping");
        stack.stop();
    }

    private static class JavaNetBuilder implements Builder {

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
            return new JavaNetHttpProtocol(config, key, stack, Optional.ofNullable(baseUrl));
        }

        @Override
        public Builder baseUrl(final String url) {
            baseUrl = Expression.of(url);
            return this;
        }
    }
}
