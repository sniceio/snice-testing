package io.snice.testing.http.stack.impl;

import io.snice.codecs.codec.http.HttpHeader;
import io.snice.codecs.codec.http.HttpRequest;
import io.snice.codecs.codec.http.HttpResponse;
import io.snice.networking.common.Transport;
import io.snice.networking.http.HttpApplication;
import io.snice.networking.http.HttpBootstrap;
import io.snice.networking.http.HttpConnection;
import io.snice.networking.http.HttpEnvironment;
import io.snice.networking.http.event.HttpEvent;
import io.snice.networking.http.event.HttpMessageEvent;
import io.snice.testing.http.HttpConfig;
import io.snice.testing.http.protocol.HttpTransaction;
import io.snice.testing.http.stack.HttpStack;
import io.snice.testing.http.stack.HttpStackUserConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.BiConsumer;

import static io.snice.preconditions.PreConditions.assertNotNull;

public class SniceHttpStack extends HttpApplication<HttpConfig> {

    private static final Logger logger = LoggerFactory.getLogger(SniceHttpStack.class);

    private HttpEnvironment<HttpConfig> env;

    @Override
    public void initialize(final HttpBootstrap<HttpConfig> bootstrap) {
        bootstrap.onConnection(id -> true).accept(b -> {
            b.match(HttpEvent::isHttpRequest).map(HttpEvent::toMessageEvent).consume(SniceHttpStack::onHttpRequest);
            b.match(HttpEvent::isHttpResponse).map(HttpEvent::toMessageEvent).consume(SniceHttpStack::onHttpResponse);
        });
    }

    public HttpStack newStack(final HttpStackUserConfig config) {
        assertNotNull(config);
        final var address = getUrl();
        return new HttpStackWrapper(config, this, address);
    }

    private URL getUrl() {
        try {
            return new URL("http://localhost:7777");
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void run(final HttpConfig configuration, final HttpEnvironment<HttpConfig> environment) {
        env = environment;
    }

    public HttpTransaction.Builder newTransaction(final HttpRequest request) {
        assertNotNull(request);
        return new HttpTransactionBuilder(env, request);
    }

    private static void onHttpRequest(final HttpConnection connection, final HttpMessageEvent event) {
        final var req = event.getHttpRequest();
        final var resp = HttpResponse.create(201).build();
        connection.send(resp);
    }

    private static void onHttpResponse(final HttpConnection connection, final HttpMessageEvent event) {
        final var resp = event.getHttpResponse();
        System.err.println("Received HTTP response outside of a Transaction: " + resp.statusCode() + " " + resp.reasonPhrase());
        resp.headers().forEach(System.err::println);
    }

    private static record HttpStackWrapper(HttpStackUserConfig config,
                                           SniceHttpStack actualStack,
                                           URL address) implements HttpStack {

        @Override
        public HttpTransaction.Builder newTransaction(final HttpRequest request) {
            return actualStack.newTransaction(request);
        }
    }

    private static class HttpTransactionBuilder implements HttpTransaction.Builder {

        private final HttpEnvironment<HttpConfig> env;
        private final HttpRequest request;

        private BiConsumer<HttpTransaction, HttpResponse> onResponseFunction;

        private HttpTransactionBuilder(final HttpEnvironment<HttpConfig> env, final HttpRequest request) {
            this.env = env;
            this.request = request;
        }

        @Override
        public HttpTransaction.Builder onResponse(final BiConsumer<HttpTransaction, HttpResponse> f) {
            assertNotNull(f);
            onResponseFunction = f;
            return this;
        }

        @Override
        public HttpTransaction start() {
            return new DefaultHttpTransaction(env, request, onResponseFunction).start();
        }

        private static record DefaultHttpTransaction(HttpEnvironment<HttpConfig> env,
                                                     HttpRequest request,
                                                     BiConsumer<HttpTransaction, HttpResponse> onResponse)
                implements HttpTransaction {

            private DefaultHttpTransaction start() {
                final var remoteHost = resolveRemoteHost(request);
                final int remotePort = resolveRemotePort(request);
                final var transport = resolveTransport(request);
                env.connect(transport, remoteHost, remotePort).thenAccept(c -> {
                    logger.debug("Successfully connected to " + remoteHost);
                    final var transaction = c.createNewTransaction(request)
                            .onResponse((tx, resp) -> onResponse.accept(this, resp))
                            .onTransactionTimeout(tx -> logger.warn("Currently not handling the transaction timing out"))
                            .onTransactionTerminated(tx -> logger.info("HTTP Transaction terminated"))
                            .start();
                });
                return this;
            }

            /**
             * Figure out where to actually connect to by looking into the request and extract
             * out the host and port.
             */
            private static String resolveRemoteHost(final HttpRequest req) {
                return req.header("Host")
                        .map(HttpHeader::value)
                        .map(Object::toString)
                        // TODO: check the URI and if still not there, we'll throw an exception.
                        .orElseThrow(() -> new RuntimeException("Unable to figure out the host"));
            }

            private static int resolveRemotePort(final HttpRequest req) {
                // TODO: for now let's keep it simple.
                return req.isSecure() ? 443 : 80;
            }

            private static Transport resolveTransport(final HttpRequest req) {
                // TODO: for now let's keep it simple.
                return Transport.tcp;
            }

        }
    }

}
