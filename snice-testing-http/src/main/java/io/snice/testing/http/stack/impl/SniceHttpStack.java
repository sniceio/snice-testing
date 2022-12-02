package io.snice.testing.http.stack.impl;

import io.snice.codecs.codec.http.HttpHeader;
import io.snice.codecs.codec.http.HttpRequest;
import io.snice.codecs.codec.http.HttpResponse;
import io.snice.identity.sri.ActionResourceIdentifier;
import io.snice.identity.sri.ScenarioResourceIdentifier;
import io.snice.networking.common.ConnectionId;
import io.snice.networking.common.Transport;
import io.snice.networking.core.ListeningPoint;
import io.snice.networking.http.HttpApplication;
import io.snice.networking.http.HttpBootstrap;
import io.snice.networking.http.HttpConnection;
import io.snice.networking.http.HttpEnvironment;
import io.snice.networking.http.event.HttpEvent;
import io.snice.networking.http.event.HttpMessageEvent;
import io.snice.testing.core.action.Action;
import io.snice.testing.core.protocol.Protocol;
import io.snice.testing.core.scenario.Scenario;
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
import static io.snice.preconditions.PreConditions.assertNull;
import static io.snice.testing.http.stack.impl.HttpStackUtils.extractSri;

public class SniceHttpStack extends HttpApplication<HttpConfig> {

    private static final Logger logger = LoggerFactory.getLogger(SniceHttpStack.class);

    private HttpEnvironment<HttpConfig> env;

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

    /**
     * Create a new {@link HttpStack} for the given {@link ScenarioResourceIdentifier} and
     * {@link ActionResourceIdentifier}.
     *
     * @param scenarioSri  the SRI of the {@link Scenario} that ultimately "owns" this stack since it owns
     *                     the execution of actions.
     * @param actionSri    the SRI of the {@link Action} that requested this stack (or rather, the {@link Protocol}
     *                     the action is using requested this stack to be created)
     * @param eventHandler there are several informational events that are being emitted by the underlying stack and
     *                     if registered, those are delivered to this event handler.
     * @param config       unique http configuration just for this "user".
     * @return
     */
    public HttpStack newStack(final ScenarioResourceIdentifier scenarioSri,
                              final ActionResourceIdentifier actionSri,
                              final BiConsumer<ConnectionId, Object> eventHandler,
                              final HttpStackUserConfig config) {
        assertNotNull(scenarioSri);
        assertNotNull(actionSri);
        assertNotNull(config);
        final var address = allocateNewAddress(actionSri, config);
        final var stack = new HttpStackWrapper(scenarioSri, actionSri, config, eventHandler, this, address);
        stacks.put(actionSri, stack);
        return stack;
    }

    private void onApplicationEvent(final HttpConnection connection, final Object event) {
        // TODO: this is an error. All should be caught by the c.onConnectionInfoEvent as registered
        //       when the connection was established
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

    private HttpTransaction.Builder newTransaction(final HttpStackWrapper wrapper, final HttpRequest request) {
        assertNotNull(request);
        return new HttpTransactionBuilder(this, env, wrapper, request);
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

    private static record HttpStackWrapper(ScenarioResourceIdentifier scenarioSri,
                                           ActionResourceIdentifier actionSri,
                                           HttpStackUserConfig config,
                                           BiConsumer<ConnectionId, Object> eventHandler,
                                           SniceHttpStack actualStack,
                                           URL address) implements HttpStack {

        @Override
        public HttpAcceptor.Builder newHttpAcceptor(final Duration timeout) {
            // TODO: overloaded version with no timeout and then we grab from the HttpStackUserConfig? or some default?
            assertNotNull(timeout);
            // TODO: perhaps some sane timeout too? 6 hrs is probably not ok!
            return actualStack.newHttpAcceptor(actionSri, timeout);
        }

        @Override
        public HttpTransaction.Builder newTransaction(final HttpRequest request) {
            final var builder = actualStack.newTransaction(this, request);
            builder.applicationData(scenarioSri);
            return builder;
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

        private final SniceHttpStack sniceHttpStack;
        private final HttpEnvironment<HttpConfig> env;
        private final HttpRequest request;
        private final HttpStackWrapper wrapper;

        private BiConsumer<HttpTransaction, HttpResponse> onResponseFunction;
        private Object applicationData;

        private HttpTransactionBuilder(final SniceHttpStack sniceHttpStack,
                                       final HttpEnvironment<HttpConfig> env,
                                       final HttpStackWrapper wrapper,
                                       final HttpRequest request) {
            this.sniceHttpStack = sniceHttpStack;
            this.env = env;
            this.wrapper = wrapper;
            this.request = request;
        }

        @Override
        public HttpTransaction.Builder onResponse(final BiConsumer<HttpTransaction, HttpResponse> f) {
            assertNotNull(f);
            onResponseFunction = f;
            return this;
        }

        @Override
        public HttpTransaction.Builder applicationData(final Object data) {
            assertNull(applicationData, "The application data has already been set. You cannot set it again. Reason, it could hide a bug");
            applicationData = data;
            return this;
        }

        @Override
        public HttpTransaction start() {
            return new DefaultHttpTransaction(sniceHttpStack, env, request, onResponseFunction, wrapper, Optional.ofNullable(applicationData)).start();
        }

    }

    private record DefaultHttpTransaction(SniceHttpStack sniceHttpStack,
                                          HttpEnvironment<HttpConfig> env,
                                          HttpRequest request,
                                          BiConsumer<HttpTransaction, HttpResponse> onResponse,
                                          HttpStackWrapper wrapper,
                                          Optional<Object> applicationData)
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
            // TODO: in the case the connection fails, we also want to register those connection events etc
            //       so they are visble in our stack. This is just the happy case right now.
            env.connect(transport, remoteHost, remotePort).thenAccept(c -> {

                // TODO: we probably want to send an event regarding which address the remoteHost:port
                //      actually resolved to (assuming it is a FQDN).
                //      We could just create that event here and ask the SniceHttpStack to dispatch it.

                c.onConnectionInfoEvent((con, event) -> {
                    if (wrapper.eventHandler != null) {
                        wrapper.eventHandler.accept(con.id(), event);
                    }
                });

                final var transactionBuilder = c.createNewTransaction(request)
                        .onResponse((tx, resp) -> onResponse.accept(this, resp))
                        .onTransactionTimeout(tx -> logger.warn("Currently not handling the transaction timing out"))
                        .onTransactionTerminated(tx -> logger.info("HTTP Transaction terminated"));
                applicationData.ifPresent(transactionBuilder::withApplicationData);
                transactionBuilder.start();
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
