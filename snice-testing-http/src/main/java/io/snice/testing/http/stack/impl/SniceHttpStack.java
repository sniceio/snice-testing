package io.snice.testing.http.stack.impl;

import io.snice.codecs.codec.http.HttpHeader;
import io.snice.codecs.codec.http.HttpRequest;
import io.snice.codecs.codec.http.HttpResponse;
import io.snice.identity.sri.ActionResourceIdentifier;
import io.snice.networking.common.Transport;
import io.snice.networking.http.HttpApplication;
import io.snice.networking.http.HttpBootstrap;
import io.snice.networking.http.HttpConnection;
import io.snice.networking.http.HttpEnvironment;
import io.snice.networking.http.event.HttpEvent;
import io.snice.networking.http.event.HttpMessageEvent;
import io.snice.testing.http.HttpConfig;
import io.snice.testing.http.protocol.HttpAcceptor;
import io.snice.testing.http.protocol.HttpServerTransaction;
import io.snice.testing.http.protocol.HttpTransaction;
import io.snice.testing.http.stack.HttpStack;
import io.snice.testing.http.stack.HttpStackUserConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static io.snice.preconditions.PreConditions.assertNotNull;
import static io.snice.testing.http.stack.impl.HttpStackUtils.extractSri;

public class SniceHttpStack extends HttpApplication<HttpConfig> {

    private static final Logger logger = LoggerFactory.getLogger(SniceHttpStack.class);

    private HttpEnvironment<HttpConfig> env;

    // TODO: not so sure we need this one anymore
    private final ConcurrentMap<ActionResourceIdentifier, HttpStackWrapper> stacks = new ConcurrentHashMap<>();

    // TODO: need sane default values for the size of the map...
    private final ConcurrentMap<ActionResourceIdentifier, DefaultHttpAcceptor> acceptors = new ConcurrentHashMap<>();

    @Override
    public void initialize(final HttpBootstrap<HttpConfig> bootstrap) {
        bootstrap.onConnection(id -> true).accept(b -> {
            b.match(HttpEvent::isHttpRequest).map(HttpEvent::toMessageEvent).consume(this::onHttpRequest);
            b.match(HttpEvent::isHttpResponse).map(HttpEvent::toMessageEvent).consume(SniceHttpStack::onHttpResponse);
        });
    }

    private HttpAcceptor.Builder newHttpAcceptor(final ActionResourceIdentifier sri,
                                                 final Duration timeout) {
        return new HttpAcceptorBuilder(sri, timeout);
    }

    public HttpStack newStack(final ActionResourceIdentifier sri, final HttpStackUserConfig config) {
        assertNotNull(config);
        final var address = allocateNewAddress(sri);
        final var stack = new HttpStackWrapper(sri, config, this, address);
        stacks.put(sri, stack);
        return stack;
    }

    /**
     * Whenever a new request to create a new "stack" is made, we need to allocate a new unique
     * address so that we can dispatch traffic properly
     *
     * @param sri
     * @return
     */
    private URL allocateNewAddress(final ActionResourceIdentifier sri) {
        try {
            return new URL("http://localhost:7777/" + sri);
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void run(final HttpConfig configuration, final HttpEnvironment<HttpConfig> environment) {
        env = environment;
    }

    private HttpTransaction.Builder newTransaction(final HttpRequest request) {
        assertNotNull(request);
        return new HttpTransactionBuilder(env, request);
    }

    private void onHttpRequest(final HttpConnection connection, final HttpMessageEvent event) {
        final var req = event.getHttpRequest();
        final var resp = mapRequest(req).map(acceptor -> {
            final var transaction = new HttpServerTransactionImpl(connection, event);
            return acceptor.onRequest.apply(transaction, req);
        }).orElseGet(SniceHttpStack::notFound);

        connection.send(resp);
    }

    private static HttpResponse notFound() {
        return HttpResponse.create(404)
                .header(HttpHeader.CONNECTION, "Close")
                .build();
    }

    private void registerHttpAcceptor(final DefaultHttpAcceptor acceptor) {
        acceptors.put(acceptor.sri, acceptor);
    }

    private Optional<DefaultHttpAcceptor> mapRequest(final HttpRequest req) {
        return extractSri(ActionResourceIdentifier.PREFIX, ActionResourceIdentifier::from, req.uri())
                .map(acceptors::get);
    }

    private static void onHttpResponse(final HttpConnection connection, final HttpMessageEvent event) {
        final var resp = event.getHttpResponse();
        System.err.println("Received HTTP response outside of a Transaction: " + resp.statusCode() + " " + resp.reasonPhrase());
        resp.headers().forEach(System.err::println);
    }

    private static record HttpStackWrapper(ActionResourceIdentifier sri,
                                           HttpStackUserConfig config,
                                           SniceHttpStack actualStack,
                                           URL address) implements HttpStack {

        @Override
        public HttpAcceptor.Builder newHttpAcceptor(final Duration timeout) {
            // TODO: overloaded version with no timeout and then we grab from the HttpStackUserConfig? or some default?
            assertNotNull(timeout);
            // TODO: perhaps some sane timeout too? 6 hrs is probably not ok!
            return actualStack.newHttpAcceptor(sri, timeout);
        }

        @Override
        public HttpTransaction.Builder newTransaction(final HttpRequest request) {
            return actualStack.newTransaction(request);
        }
    }

    private class HttpAcceptorBuilder implements HttpAcceptor.Builder {

        private final ActionResourceIdentifier sri;
        private final Duration timeout;

        private BiFunction<HttpServerTransaction, HttpRequest, HttpResponse> onRequest;
        private Consumer<HttpAcceptor> onTimeout;

        HttpAcceptorBuilder(final ActionResourceIdentifier sri, final Duration timeout) {
            this.sri = sri;
            this.timeout = timeout;
        }

        @Override
        public HttpAcceptor.Builder onRequest(final BiFunction<HttpServerTransaction, HttpRequest, HttpResponse> f) {
            assertNotNull(f);
            onRequest = f;
            return this;
        }

        @Override
        public HttpAcceptor.Builder onTimeout(final Consumer<HttpAcceptor> f) {
            assertNotNull(f);
            onTimeout = f;
            return this;
        }

        @Override
        public HttpAcceptor start() {
            assertNotNull(onRequest, "You must specify a function for handling the incoming Http Request");
            assertNotNull(onTimeout, "You must specify a function for handling the timeout");
            final var acceptor = new DefaultHttpAcceptor(sri, timeout, onRequest, onTimeout);
            registerHttpAcceptor(acceptor);
            return acceptor;
        }

    }

    private record DefaultHttpAcceptor(ActionResourceIdentifier sri,
                                       Duration timeout,
                                       BiFunction<HttpServerTransaction, HttpRequest, HttpResponse> onRequest,
                                       Consumer<HttpAcceptor> onTimeout)
            implements HttpAcceptor {

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
                    c.createNewTransaction(request)
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
