package io.snice.testing.http.action;

import io.snice.identity.sri.ActionResourceIdentifier;
import io.snice.testing.core.Execution;
import io.snice.testing.core.Session;
import io.snice.testing.core.action.Action;
import io.snice.testing.http.AcceptHttpRequestDef;
import io.snice.testing.http.response.RequestProcessor;
import io.snice.testing.http.stack.HttpStack;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public record AcceptHttpRequestAction(String name,
                                      ActionResourceIdentifier sri,
                                      HttpStack stack,
                                      AcceptHttpRequestDef def,
                                      Map<String, Object> attributes,
                                      Action next) implements Action {

    @Override
    public void execute(final List<Execution> executions, final Session session) {
        final var requestProcessor = new RequestProcessor(name, def, session, executions, next);
        stack.newHttpAcceptor(Duration.ofSeconds(10))
                .onRequest(requestProcessor::onRequest)
                .onTimeout(requestProcessor::onTimeout)
                .onAcceptorTerminated(requestProcessor::onTermination)
                .start();
    }


}
