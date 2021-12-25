package io.snice.testing.http.action;

import io.snice.codecs.codec.http.HttpRequest;
import io.snice.functional.Either;
import io.snice.testing.core.Session;
import io.snice.testing.core.action.Action;
import io.snice.testing.http.HttpRequestDef;
import io.snice.testing.http.protocol.HttpProtocol;

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

                // TODO:
                http.stack().newTransaction(request);
            }

            next.execute(newSession);
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static HttpRequest map(final Session session, final HttpRequestDef def, final URL target) throws URISyntaxException {
        final var builder = HttpRequest.create(def.method(), target.toURI());
        return builder.build();
    }

    private Session processResolveUriError(final Session session, final String errorMsg) {
        return session.markAsFailed();
    }

    /**
     * The {@link HttpRequestDef} may contain expressions that we need to resolve
     * in order to build up the full URI.
     *
     * @param session
     */
    private Either<Throwable, URL> resolveURI(final Session session) {
        // TODO: would also need to figure out if the URL contains
        // an expression.
        /*
        return httpDef.baseUrl().or(() -> http.baseUrl())
                .map(base -> createUrl(base, httpDef.uri()))
                .orElse(createUrl(null, httpDef.uri()));

         */
        return Either.left(new IllegalArgumentException("apa"));
    }

}
