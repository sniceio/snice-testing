package io.snice.testing.http.stack.impl;

import io.snice.codecs.codec.http.HttpHeader;
import io.snice.codecs.codec.http.HttpRequest;
import io.snice.codecs.codec.http.HttpResponse;
import io.snice.identity.sri.ActionResourceIdentifier;
import io.snice.networking.common.Transport;
import io.snice.networking.core.ListeningPoint;
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
import io.snice.testing.http.response.RequestResult;
import io.snice.testing.http.stack.HttpStack;
import io.snice.testing.http.stack.HttpStackUserConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
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
            b.matchEvent(o -> true).consume(this::onApplicationEvent);
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
        final var address = allocateNewAddress(sri, config);
        final var stack = new HttpStackWrapper(sri, config, this, address);
        stacks.put(sri, stack);
        return stack;
    }

    private void onApplicationEvent(final HttpConnection connection, final Object event) {
        System.err.println("Snice Http Stack onAppEvent: " + event);
    }

    /**
     * Whenever a new request to create a new "stack" is made, we need to allocate a new unique
     * address so that we can dispatch traffic properly. We rely on the underlying Snice Networking
     * to provide a so-called VIP Address, which is typically an external load balancer so that
     * external traffic can reach out.
     *
     * @param sri
     * @return
     */
    private URL allocateNewAddress(final ActionResourceIdentifier sri, final HttpStackUserConfig config) {
        try {
            final var nic = env.getDefaultNetworkInterface();

            // TODO: would need to look into the user preference to see what they prefer. http or https etc...
            final ListeningPoint<HttpEvent> lp;
            if (nic.isSupportingTransport(Transport.tls)) {
                lp = nic.getListeningPoint(Transport.tls);
            } else if (nic.isSupportingTransport(Transport.tcp)) {
                lp = nic.getListeningPoint(Transport.tcp);
            } else {
                // TODO: not sure how to handle but bail for now.
                throw new IllegalArgumentException("Unable to find suitable Network Interface. The default " +
                        "did not support TLS nor TCP, which seems odd");
            }

            // TODO: need to ensure that we can just append slash + SRI (in case it already ends on slash etc)
            final var url = lp.getVipAddress().orElse(lp.getListenAddress());
            return new URL(url.toString() + "/" + sri.asString());

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
        // TODO: should perhaps give this to the ActionFsm instead since now it is a bit hard
        // to look at the logs and see what's going on.
        final var req = event.getHttpRequest();
        final var acceptorMaybe = mapRequest(req);
        final var result = acceptorMaybe
                .map(acceptor -> acceptor.processRequest(event))
                .orElseGet(SniceHttpStack::notFound);

        connection.send(result.response());

        if (result.isLast() && acceptorMaybe.isPresent()) {
            final var acceptor = acceptorMaybe.get();
            deRegisterHttpAcceptor(acceptor);
            acceptor.terminate();
        }

        if (result.closeConnection()) {
            connection.close();
        }
    }

    private static RequestResult notFound() {
        final var notFound = HttpResponse.create(404)
                .header(HttpHeader.CONNECTION, "Close")
                .build();
        return new RequestResult(notFound, true, true);
    }

    private void deRegisterHttpAcceptor(final DefaultHttpAcceptor acceptor) {
        acceptors.remove(acceptor.sri());
    }

    private void registerHttpAcceptor(final DefaultHttpAcceptor acceptor) {
        acceptors.put(acceptor.sri(), acceptor);
    }

    private Optional<DefaultHttpAcceptor> mapRequest(final HttpRequest req) {
        return extractSri(ActionResourceIdentifier.PREFIX, ActionResourceIdentifier::from, req.uri())
                .map(acceptors::get);
    }

    private static void onHttpResponse(final HttpConnection connection, final HttpMessageEvent event) {
        final var resp = event.getHttpResponse();
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

        private BiFunction<HttpServerTransaction, HttpRequest, RequestResult> onRequest;
        private Consumer<HttpAcceptor> onTimeout;
        private Consumer<HttpAcceptor> onTermination;

        HttpAcceptorBuilder(final ActionResourceIdentifier sri, final Duration timeout) {
            this.sri = sri;
            this.timeout = timeout;
        }

        @Override
        public HttpAcceptor.Builder onRequest(final BiFunction<HttpServerTransaction, HttpRequest, RequestResult> f) {
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
        public HttpAcceptor.Builder onAcceptorTerminated(final Consumer<HttpAcceptor> f) {
            assertNotNull(f);
            onTermination = f;
            return this;

        }

        @Override
        public HttpAcceptor start() {
            assertNotNull(onRequest, "You must specify a function for handling the incoming Http Request");
            assertNotNull(onTimeout, "You must specify a function for handling the timeout");
            assertNotNull(onTermination, "You must specify a function for handling the termination of the "
                    + HttpAcceptor.class.getSimpleName());
            final var acceptor = new DefaultHttpAcceptor(sri, timeout, onRequest, onTimeout, onTermination);
            registerHttpAcceptor(acceptor);
            return acceptor;
        }

    }

    private static class HttpTransactionBuilder implements HttpTransaction.Builder {

        private final HttpEnvironment<HttpConfig> env;
        private final HttpRequest request;

        private BiConsumer<HttpTransaction, HttpResponse> onResponseFunction;
        private BiConsumer<HttpTransaction, Object> onEventFunction;

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
        public HttpTransaction.Builder onEvent(final BiConsumer<HttpTransaction, Object> f) {
            assertNotNull(f);
            onEventFunction = f;
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
                final var remoteDest = resolveRemoteDest(request);
                final var remoteHost = remoteDest.getHost();
                final int remotePort = resolveRemotePort(request, remoteDest);
                final var transport = resolveTransport(request);

                // TODO: we may, or may not, want to share an existing connection with others. This is
                //      currently not supported by Snice Networking but it really should be. However, in
                //      Sncie Testing, most of the time we probably don't want to share connection outside
                //      the same Scenario (so we get the statistics etc just for a single scenario and so on...
                env.connect(transport, remoteHost, remotePort).thenAccept(c -> {
                    logger.debug("Successfully connected to " + remoteDest);
                    final var transaction = c.createNewTransaction(request)
                            .onResponse((tx, resp) -> onResponse.accept(this, resp))
                            .onTransactionTimeout(tx -> logger.warn("Currently not handling the transaction timing out"))
                            .onTransactionTerminated(tx -> logger.info("HTTP Transaction terminated"))
                            .start();
                });
                return this;
            }

            private static URI resolveRemoteDest(final HttpRequest req) {
                return req.header("Host")
                        .map(host -> (req.isSecure() ? "https://" : "http://") + host.value())
                        .map(URI::create)
                        // TODO: check the URI and if still not there, we'll throw an exception.
                        .orElseThrow(() -> new RuntimeException("Unable to figure out the host"));
            }

            private static int resolveRemotePort(final HttpRequest req, final URI remoteDest) {
                if (remoteDest.getPort() != -1) {
                    return remoteDest.getPort();
                }

                return req.isSecure() ? 443 : 80;
            }

            private static Transport resolveTransport(final HttpRequest req) {
                // TLS is poorly supported by Snice Networking right now so we'll just claim
                // TCP but configure the Snice stack to do TLS anyway...
                /*
                if (req.isSecure()) {
                    return Transport.tls;
                }
                 */

                return Transport.tcp;
            }
        }
    }

}
