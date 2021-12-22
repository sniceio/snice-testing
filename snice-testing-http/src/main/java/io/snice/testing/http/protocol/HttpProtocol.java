package io.snice.testing.http.protocol;

import io.snice.testing.core.protocol.Protocol;
import io.snice.testing.core.protocol.ProtocolRegistry;
import io.snice.testing.http.HttpConfig;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Optional;

import static io.snice.preconditions.PreConditions.assertNotNull;

public interface HttpProtocol extends Protocol {

    ProtocolRegistry.Key httpProtocolKey = Protocol.createKey(HttpProtocol.class);

    static HttpProtocolBuilder of(final HttpConfig config) {
        assertNotNull(config);
        return new DefaultHttpProtocolBuilder(config);
    }

    HttpTransaction.Builder newTransaction(HttpRequest request);

    HttpConfig config();

    Optional<URL> baseUrl();

    interface HttpProtocolBuilder extends Protocol.Builder {

        HttpProtocolBuilder baseUrl(URL url);

        default HttpProtocolBuilder baseUrl(final String url) {
            try {
                return baseUrl(new URL(url));
            } catch (final MalformedURLException e) {
                throw new IllegalArgumentException(e);
            }
        }

    }

    class DefaultHttpProtocolBuilder implements HttpProtocolBuilder {

        private final HttpConfig config;
        private URL baseUrl;

        private DefaultHttpProtocolBuilder(final HttpConfig config) {
            this.config = config;
        }

        @Override
        public HttpProtocol build() {
            final var key = HttpProtocol.httpProtocolKey;
            final var httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            return new DefaultHttpProtocol(config, key, httpClient, Optional.ofNullable(baseUrl));
        }

        @Override
        public HttpProtocolBuilder baseUrl(final URL url) {
            assertNotNull(url);
            this.baseUrl = url;
            return this;
        }

        private static record DefaultHttpProtocol(HttpConfig config,
                                                  ProtocolRegistry.Key key,
                                                  HttpClient httpClient,
                                                  Optional<URL> baseUrl) implements HttpProtocol {
            /*
            public CompletableFuture<HttpResponse> send(final HttpRequest req) {
                final var bodyHandler = HttpResponse.BodyHandlers.ofString();
                final CompletableFuture<HttpResponse<String>> future = httpClient.sendAsync(req, bodyHandler);
                future.whenComplete((s, t) -> {
                    System.err.println("Completed. Body: " + s);
                    System.err.println("Completed. Throwable: " + t);
                });
            }
             */

            @Override
            public HttpTransaction.Builder newTransaction(final HttpRequest request) {
                return null;
            }

            @Override
            public void start() {
                System.err.println("HTTP Protocol Starting");
            }

            @Override
            public void stop() {
                System.err.println("HTTP Protocol Stopping");
            }
        }
    }
}
