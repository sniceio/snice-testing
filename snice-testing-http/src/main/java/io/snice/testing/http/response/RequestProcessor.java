package io.snice.testing.http.response;

import io.snice.codecs.codec.http.HttpRequest;
import io.snice.codecs.codec.http.HttpResponse;
import io.snice.testing.core.Execution;
import io.snice.testing.core.Session;
import io.snice.testing.core.action.Action;
import io.snice.testing.core.check.Check;
import io.snice.testing.core.check.CheckResult;
import io.snice.testing.http.protocol.HttpAcceptor;
import io.snice.testing.http.protocol.HttpServerTransaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The {@link RequestProcessor} is processing an incoming request and is responsible for computing the
 * answer and then kick off the next action.
 */
public class RequestProcessor {

    private final String name;
    private final List<Check<HttpRequest>> checks;
    private final Action next;

    private final Object lock = new Object();
    private final List<Execution> executions;
    private final AtomicReference<Session> session;

    public RequestProcessor(final String name,
                            final List<Check<HttpRequest>> checks,
                            final Session session,
                            final List<Execution> executions,
                            final Action next) {
        this.name = name;
        this.checks = checks;
        this.session = new AtomicReference<>(session);
        this.executions = Collections.synchronizedList(new ArrayList<>(executions));
        this.next = next;
    }

    public HttpResponse onRequest(final HttpServerTransaction transaction, final HttpRequest request) {
        final var result = Check.check(request, session.get(), checks);
        final var checkResults = result.right();
        final var failedChecks = checkResults.stream().filter(CheckResult::isFailure).findAny().isPresent();

        session.set(result.left());
        final var execution = new Execution(name, !failedChecks, checkResults);
        executions.add(execution);

        return transaction.createResponse(200).header("Yeah", "Cool").build();
    }

    public void onTimeout(final HttpAcceptor acceptor) {
        System.err.println("Ops, timeout");
    }

    public void onTermination(final HttpAcceptor acceptor) {
        final var finalExecutions = Collections.unmodifiableList(executions);
        next.execute(finalExecutions, session.get());
    }
}
