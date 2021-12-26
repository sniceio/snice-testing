package io.snice.testing.http.action;

import io.snice.codecs.codec.http.HttpRequest;
import io.snice.testing.core.Session;
import io.snice.testing.core.action.Action;
import io.snice.testing.http.HttpRequestDef;
import io.snice.testing.http.protocol.HttpProtocol;
import io.snice.testing.http.response.ResponseProcessor;

import java.net.URISyntaxException;
import java.net.URL;

public record HttpRequestAction(String name, HttpProtocol http, HttpRequestDef httpDef, Action next) implements Action {

    @Override
    public void execute(final Session session) {
        try {
            final var uriMaybe = httpDef.resolveTarget(http, session);
            final var newSession = uriMaybe.fold(t -> processResolveUriError(session, t), uri -> session);
            if (newSession.isSucceeded()) {
                final var request = map(session, httpDef, uriMaybe.get());
                final var transaction = http.stack().newTransaction(request);
                final var processor = new ResponseProcessor(name, request, session, next);
                transaction.onResponse(processor::process);
                transaction.start();
            } else {
                next.execute(newSession);
            }
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static HttpRequest map(final Session session, final HttpRequestDef def, final URL target) throws URISyntaxException {
        final var builder = HttpRequest.create(def.method(), target.toURI());
        def.headers().entrySet().forEach(entry -> builder.header(entry.getKey(), entry.getValue().apply(session)));
        return builder.build();
    }

    private Session processResolveUriError(final Session session, final String errorMsg) {
        return session.markAsFailed();
    }

}
