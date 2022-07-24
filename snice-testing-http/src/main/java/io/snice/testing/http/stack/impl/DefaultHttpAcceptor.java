package io.snice.testing.http.stack.impl;

import io.snice.codecs.codec.http.HttpRequest;
import io.snice.identity.sri.ActionResourceIdentifier;
import io.snice.networking.http.event.HttpMessageEvent;
import io.snice.testing.http.protocol.HttpAcceptor;
import io.snice.testing.http.protocol.HttpServerTransaction;
import io.snice.testing.http.response.RequestResult;

import java.time.Duration;
import java.util.function.BiFunction;
import java.util.function.Consumer;

class DefaultHttpAcceptor implements HttpAcceptor {

    private final ActionResourceIdentifier sri;
    private final Duration timeout;
    private final BiFunction<HttpServerTransaction, HttpRequest, RequestResult> onRequest;
    private final Consumer<HttpAcceptor> onTimeout;
    private final Consumer<HttpAcceptor> onTermination;

    DefaultHttpAcceptor(final ActionResourceIdentifier sri,
                        final Duration timeout,
                        final BiFunction<HttpServerTransaction, HttpRequest, RequestResult> onRequest,
                        final Consumer<HttpAcceptor> onTimeout,
                        final Consumer<HttpAcceptor> onTermination) {
        this.sri = sri;
        this.timeout = timeout;
        this.onRequest = onRequest;
        this.onTimeout = onTimeout;
        this.onTermination = onTermination;
    }

    ActionResourceIdentifier sri() {
        return sri;
    }

    public RequestResult processRequest(final HttpMessageEvent event) {
        // TODO: a single request will result in our termination. Eventually, this acceptor needs to
        // be able to handle multiple http requests etc.
        final var transaction = new HttpServerTransactionImpl(event);
        return onRequest.apply(transaction, event.getHttpRequest());
    }

    /**
     * Although the state of whether this acceptor is "done" is managed within the acceptor itself,
     * the request to terminate is issued by the execution environment so that e.g. the response
     * to the latest request is sent before the termination occurs etc.
     */
    public void terminate() {
        onTermination.accept(this);
    }

}
