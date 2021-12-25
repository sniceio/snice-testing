package io.snice.testing.http.stack.jnet;

import io.snice.codecs.codec.http.HttpRequest;
import io.snice.testing.http.protocol.HttpTransaction;
import io.snice.testing.http.stack.HttpStack;

import java.net.http.HttpClient;

import static io.snice.preconditions.PreConditions.assertNotNull;

public record JavaNetHttpStack(HttpClient client) implements HttpStack {

    public JavaNetHttpStack {
        assertNotNull(client);
    }

    @Override
    public HttpTransaction.Builder newTransaction(final HttpRequest request) {
        return null;
    }

    public void start() {
        System.err.println("HTTP Stack Starting");
    }

    public void stop() {
        System.err.println("HTTP Stack Stopping");
    }
}
