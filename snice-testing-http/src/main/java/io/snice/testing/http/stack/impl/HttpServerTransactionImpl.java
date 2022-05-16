package io.snice.testing.http.stack.impl;

import io.snice.codecs.codec.http.HttpResponse;
import io.snice.networking.http.HttpConnection;
import io.snice.networking.http.event.HttpMessageEvent;
import io.snice.testing.http.protocol.HttpServerTransaction;

public record HttpServerTransactionImpl(HttpConnection connection,
                                        HttpMessageEvent event) implements HttpServerTransaction {

    @Override
    public HttpResponse.Builder<HttpResponse> createResponse(final int statusCode) {
        return HttpResponse.create(statusCode);
    }

    @Override
    public void sendResponse(final HttpResponse response) {
        connection.send(response);
    }
}
