package io.snice.testing.core.check;

import io.snice.testing.core.Session;

import java.util.List;

public interface Check<T> {

    static <T> Session check(final T message, final Session session, final List<Check<T>> checks) {
        return checkIt(message, session, 0, checks);
    }

    private static <T> Session checkIt(final T message,
                                       final Session session,
                                       final int index,
                                       final List<Check<T>> checks) {

        if (index == checks.size()) {
            return session;
        }

        final CheckResult<T, ?> result = checks.get(index).check(message, session);
        final var newSession = session.processCheckResult(result);
        /*
        final var newSession = result.fold(msg -> {
            failures.add((Validation.Failure) result);
            return session.markAsFailed();
        }, checkResult -> checkResult.saveAs()
                .map(name -> checkResult.extractedValue()
                        .map(value -> session.attributes(name, value))
                        .orElse(session))
                .orElse(session));

         */

        return checkIt(message, newSession, index + 1, checks);
    }

    CheckResult<T, ?> check(T message, Session session);

}
