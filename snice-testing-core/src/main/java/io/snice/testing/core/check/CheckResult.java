package io.snice.testing.core.check;

import java.util.Optional;

public record CheckResult<T, X>(Check<T> check,
                                Optional<X> extractedValue,
                                Optional<String> saveAs,
                                Optional<String> failure) {

    public boolean isSuccess() {
        return !isFailure();
    }

    public boolean isFailure() {
        return failure.isPresent();
    }

}
