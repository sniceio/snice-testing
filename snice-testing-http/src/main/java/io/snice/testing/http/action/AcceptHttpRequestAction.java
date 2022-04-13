package io.snice.testing.http.action;

import io.snice.testing.core.Execution;
import io.snice.testing.core.Session;
import io.snice.testing.core.action.Action;
import io.snice.testing.http.AcceptHttpRequestDef;
import io.snice.testing.http.protocol.HttpProtocol;

import java.util.List;

public record AcceptHttpRequestAction(String name,
                                      HttpProtocol http,
                                      AcceptHttpRequestDef def,
                                      Action next) implements Action {

    @Override
    public void execute(final List<Execution> executions, final Session session) {
        // TODO
        System.err.println("Got to execute somehow");
        next.execute(executions, session);
    }

}
