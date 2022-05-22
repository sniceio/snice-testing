package io.snice.testing.http;

import io.snice.codecs.codec.http.HttpMessage;
import io.snice.testing.core.Session;
import io.snice.testing.core.common.Expression;

import static io.snice.preconditions.PreConditions.assertNotNull;

public interface Auth {

    <T extends HttpMessage> HttpMessage.Builder<T> apply(Session session, HttpMessage.Builder<T> builder);

    static Auth basicAuth(final Expression username, final Expression password) {
        return new BasicAuth(username, password);
    }

    record BasicAuth(Expression username, Expression password) implements Auth {

        public BasicAuth {
            assertNotNull(username);
            assertNotNull(password);
        }

        @Override
        public <T extends HttpMessage> HttpMessage.Builder<T> apply(final Session session, final HttpMessage.Builder<T> builder) {
            final var resolvedUsername = username.apply(session);
            final var resolvedPassword = password.apply(session);
            return builder.auth(resolvedUsername, resolvedPassword);
        }
    }
}
