package io.snice.testing.http.response;

import io.snice.codecs.codec.http.HttpRequest;
import io.snice.codecs.codec.http.HttpResponse;
import io.snice.testing.core.Session;
import io.snice.testing.core.action.Action;
import io.snice.testing.core.check.Check;
import io.snice.testing.http.protocol.HttpTransaction;

import java.util.List;

public record ResponseProcessor(String name,
                                HttpRequest req,
                                List<Check<HttpResponse>> checks,
                                Session session,
                                Action next) {

    public void process(final HttpTransaction transaction, final HttpResponse response) {
        // TODO: now I need to check if there are any checks etc..

        final var result = Check.check(response, session, checks);
        result.left().forEach(failure -> {
            System.err.println("Apparently we had a failure: " + failure);
        });
        final var newSession = result.right();
        next.execute(newSession);
    }
}
