package io.snice.testing.core.check;

import io.snice.testing.core.Session;
import io.snice.testing.core.common.Pair;
import io.snice.testing.core.common.Validation;

import java.util.ArrayList;
import java.util.List;

public interface Check<T> {

    static <T> Pair<List<Validation.Failure>, Session> check(final T message, final Session session, final List<Check<T>> checks) {
        return checkIt(message, session, 0, checks, new ArrayList<>());
    }

    private static <T> Pair<List<Validation.Failure>, Session> checkIt(final T message,
                                                                       final Session session,
                                                                       final int index,
                                                                       final List<Check<T>> checks,
                                                                       final List<Validation.Failure> failures) {
        if (index == checks.size()) {
            return new Pair<>(failures, session);
        }

        final var result = checks.get(index).check(message, session);
        final var newSession = result.fold(msg -> {
            failures.add((Validation.Failure) result);
            return session.markAsFailed();
        }, checkResult -> checkResult.saveAs()
                .map(name -> checkResult.extractedValue()
                        .map(value -> session.attributes(name, value))
                        .orElse(session))
                .orElse(session));

        return checkIt(message, newSession, index + 1, checks, failures);
    }

    Validation<CheckResult<?>> check(T message, Session session);

}
