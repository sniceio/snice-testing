package io.snice.testing.http.response;

import io.snice.codecs.codec.http.HttpRequest;
import io.snice.codecs.codec.http.HttpResponse;
import io.snice.testing.core.Session;
import io.snice.testing.core.action.Action;
import io.snice.testing.http.protocol.HttpTransaction;

public record ResponseProcessor(String name,
                                HttpRequest req,
                                Session session,
                                Action next) {

    public void process(final HttpTransaction transaction, final HttpResponse response) {
        System.err.println("Processing the response: " + response);
    }
}
