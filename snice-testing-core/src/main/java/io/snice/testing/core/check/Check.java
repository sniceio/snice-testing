package io.snice.testing.core.check;

import io.snice.testing.core.Session;
import io.snice.testing.core.common.Pair;

import java.util.ArrayList;
import java.util.List;

public interface Check<T> {

    static <T> Pair<Session, List<CheckResult<T, ?>>> check(final T message, final Session session, final List<Check<T>> checks) {
        return checkIt(message, session, 0, checks, new ArrayList<>());
    }

    private static <T> Pair<Session, List<CheckResult<T, ?>>> checkIt(final T message,
                                                                      final Session session,
                                                                      final int index,
                                                                      final List<Check<T>> checks,
                                                                      final List<CheckResult<T, ?>> results) {

        if (index == checks.size()) {
            return new Pair(session, results);
        }

        final CheckResult<T, ?> result = checks.get(index).check(message, session);
        final var newSession = processCheckResult(result, session);
        results.add(result);

        return checkIt(message, newSession, index + 1, checks, results);
    }

    private static Session processCheckResult(final CheckResult<?, ?> result, final Session session) {

        if (result.isFailure()) {
            return session.markAsFailed();
        }

        return result.saveAs().isPresent() && result.extractedValue().isPresent() ?
                session.attributes(result.saveAs().get(), result.extractedValue().get()) :
                session;

    }

    CheckResult<T, ?> check(T message, Session session);

}
