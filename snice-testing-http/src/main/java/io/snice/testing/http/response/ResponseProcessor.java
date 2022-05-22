package io.snice.testing.http.response;

import io.snice.codecs.codec.http.HttpRequest;
import io.snice.codecs.codec.http.HttpResponse;
import io.snice.testing.core.Execution;
import io.snice.testing.core.Session;
import io.snice.testing.core.action.Action;
import io.snice.testing.core.check.Check;
import io.snice.testing.core.check.CheckResult;
import io.snice.testing.core.common.ListOperations;
import io.snice.testing.http.protocol.HttpTransaction;

import java.util.List;

public record ResponseProcessor(String name,
                                HttpRequest req,
                                List<Check<HttpResponse>> checks,
                                Session session,
                                List<Execution> executions,
                                Action next) {

    public void process(final HttpTransaction transaction, final HttpResponse response) {

        final var result = Check.check(response, session, checks);
        final var checkResults = result.right();
        final var failedChecks = checkResults.stream().filter(CheckResult::isFailure).findAny().isPresent();

        // System.err.println(response);
        // System.err.println(response.content().get());

        final var newSession = result.left();
        final var execution = new Execution(name, !failedChecks, checkResults);
        next.execute(ListOperations.extendList(executions, execution), newSession);
    }
}
