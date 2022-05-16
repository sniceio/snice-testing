package io.snice.testing.http.response;

import io.snice.codecs.codec.http.HttpRequest;
import io.snice.codecs.codec.http.HttpResponse;
import io.snice.testing.core.Execution;
import io.snice.testing.core.Session;
import io.snice.testing.core.action.Action;
import io.snice.testing.core.check.Check;
import io.snice.testing.core.check.CheckResult;
import io.snice.testing.core.common.ListOperations;
import io.snice.testing.http.protocol.HttpAcceptor;
import io.snice.testing.http.protocol.HttpServerTransaction;

import java.util.List;

/**
 * The {@link RequestProcessor} is processing an incoming request and is responsible for computing the
 * answer and then kick off the next action.
 */
public record RequestProcessor(String name,
                               List<Check<HttpRequest>> checks,
                               Session session,
                               List<Execution> executions,
                               Action next) {

    public HttpResponse onRequest(final HttpServerTransaction transaction, final HttpRequest request) {
        final var result = Check.check(request, session, checks);
        final var checkResults = result.right();
        final var failedChecks = checkResults.stream().filter(CheckResult::isFailure).findAny().isPresent();

        final var newSession = result.left();
        final var execution = new Execution(name, !failedChecks, checkResults);
        next.execute(ListOperations.extendList(executions, execution), newSession);

        return transaction.createResponse(200).header("Yeah", "Cool").build();
    }

    public void onTimeout(final HttpAcceptor transaction) {
        System.err.println("Ops, timeout");
    }
}
