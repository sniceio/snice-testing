package io.snice.testing.http.response;

import io.snice.codecs.codec.http.HttpRequest;
import io.snice.testing.core.Execution;
import io.snice.testing.core.Session;
import io.snice.testing.core.action.Action;
import io.snice.testing.core.check.Check;
import io.snice.testing.core.check.CheckResult;
import io.snice.testing.http.AcceptHttpRequestDef;
import io.snice.testing.http.protocol.HttpAcceptor;
import io.snice.testing.http.protocol.HttpServerTransaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The {@link RequestProcessor} is processing an incoming request and is responsible for computing the
 * answer and then kick off the next action.
 */
public class RequestProcessor {

    private final String name;
    private final Action next;

    private final List<AcceptHttpRequestDef> defs;
    private final List<Execution> executions;
    private final AtomicReference<Session> session;
    private final AtomicInteger index = new AtomicInteger(0);

    public RequestProcessor(final String name,
                            final AcceptHttpRequestDef def,
                            final Session session,
                            final List<Execution> executions,
                            final Action next) {
        this.name = name;
        this.defs = unravel(def);
        this.session = new AtomicReference<>(session);
        this.executions = Collections.synchronizedList(new ArrayList<>(executions));
        this.next = next;
    }

    private static List<AcceptHttpRequestDef> unravel(final AcceptHttpRequestDef def) {
        return def.child().map(child -> {
            final var list = unravel(child);
            list.add(0, child);
            return list;
        }).orElseGet(() -> {
            final var list = new ArrayList<AcceptHttpRequestDef>();
            list.add(def);
            return list;
        });
    }

    public RequestResult onRequest(final HttpServerTransaction transaction, final HttpRequest request) {
        final var def = next();
        final var result = Check.check(request, session.get(), def.checks());
        final var checkResults = result.right();
        final var failedChecks = checkResults.stream().filter(CheckResult::isFailure).findAny().isPresent();

        session.set(result.left());
        final var execution = new Execution(name, !failedChecks, checkResults);
        executions.add(execution);

        final var responseBuilder = transaction.createResponse(def.statusCode());
        final var currentSession = session.get();
        def.headers().entrySet().forEach(entry -> responseBuilder.header(entry.getKey(), entry.getValue().apply(currentSession)));
        def.content().ifPresent(content -> content.apply(currentSession, responseBuilder));

        // TODO: Just making it obvious but this value should be coming from the definition itself.
        // The user should say whether we should close, or to keep it open (Assuming remote doesn't close of course)
        final boolean closeConnection = false;
        return new RequestResult(responseBuilder.build(), closeConnection, isLast());
    }

    /**
     * If this was the last invocation.
     *
     * @return
     */
    private boolean isLast() {
        return index.get() >= defs.size();
    }

    private AcceptHttpRequestDef next() {
        return defs.get(index.getAndAdd(1));
    }

    public void onTimeout(final HttpAcceptor acceptor) {
        System.err.println("Ops, timeout");
    }

    public void onTermination(final HttpAcceptor acceptor) {
        final var finalExecutions = Collections.unmodifiableList(executions);
        next.execute(finalExecutions, session.get());
    }
}
