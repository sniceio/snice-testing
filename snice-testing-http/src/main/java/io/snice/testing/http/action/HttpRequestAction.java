package io.snice.testing.http.action;

import io.snice.testing.core.Session;
import io.snice.testing.core.action.Action;
import io.snice.testing.http.HttpRequestDef;
import io.snice.testing.http.protocol.HttpProtocol;

public record HttpRequestAction(String name, HttpProtocol http, HttpRequestDef httpDef, Action next) implements Action {

    @Override
    public void execute(final Session session) {
        try {
            System.err.println("Executing the HTTP Request Definition");
            // http.send(httpDef);
            next.execute(session);
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
