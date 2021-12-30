package io.snice.testing.core.check;

import io.snice.functional.Either;

import java.util.Optional;

public interface Extractor<T, X> {

    Either<Throwable, Optional<X>> apply(T object);
}
