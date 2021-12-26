package io.snice.testing.http.stack.jnet;

import io.snice.codecs.codec.http.HttpRequest;
import io.snice.codecs.codec.http.HttpResponse;
import io.snice.testing.http.codec.JavaNetHttpRequest;
import io.snice.testing.http.codec.JavaNetHttpResponse;
import io.snice.testing.http.protocol.HttpTransaction;
import io.snice.testing.http.stack.HttpStack;

import java.net.http.HttpClient;
import java.util.function.BiConsumer;

import static io.snice.preconditions.PreConditions.assertNotNull;

public record JavaNetHttpStack(HttpClient client) implements HttpStack {

    public JavaNetHttpStack {
        assertNotNull(client);
    }

    @Override
    public HttpTransaction.Builder newTransaction(final HttpRequest request) {
        try {
            assertNotNull(request);
            return new HttpTransactionBuilder(client, (JavaNetHttpRequest) request);
        } catch (final ClassCastException e) {
            throw new IllegalArgumentException("Internal implementation error - the given HttpRequest was " +
                    "not the expected implementation, which should be impossible so there must be a bug. " +
                    "Expected class " + JavaNetHttpRequest.class.getName() + " but got " + request.getClass().getName());
        }
    }

    public void start() {
        System.err.println("HTTP Stack Starting");
    }

    public void stop() {
        System.err.println("HTTP Stack Stopping");
    }

    private static class HttpTransactionBuilder implements HttpTransaction.Builder {

        private final JavaNetHttpRequest request;
        private final HttpClient client;
        private BiConsumer<HttpTransaction, HttpResponse> onResponseFunction;

        private HttpTransactionBuilder(final HttpClient client, final JavaNetHttpRequest request) {
            this.client = client;
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
            return new DefaultHttpTransaction(client, request, onResponseFunction).start();
        }
    }

    private static record DefaultHttpTransaction(HttpClient client,
                                                 JavaNetHttpRequest request,
                                                 BiConsumer<HttpTransaction, HttpResponse> onResponse)
            implements HttpTransaction {

        private DefaultHttpTransaction start() {
            final var future = client.sendAsync(request.javaNetHttpRequest(), java.net.http.HttpResponse.BodyHandlers.ofInputStream());
            future.whenComplete((resp, t) -> {
                if (resp != null) {
                    onResponse.accept(this, JavaNetHttpResponse.of(resp));
                } else {
                    // TODO: deal with exception
                }
            });
            return this;
        }

    }
}
