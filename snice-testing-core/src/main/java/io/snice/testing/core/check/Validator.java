package io.snice.testing.core.check;

import io.snice.testing.core.common.Validation;

import java.util.Optional;

public interface Validator<T> {

    Validation<Optional> FOUND_NOTHING_FAILURE = Validation.failure("Found nothing");

    Validation<Optional<T>> apply(Optional<T> actual);
}
