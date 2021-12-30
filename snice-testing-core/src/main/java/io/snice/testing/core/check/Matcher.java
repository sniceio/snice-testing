package io.snice.testing.core.check;

import io.snice.testing.core.common.Validation;

import java.util.Optional;

import static io.snice.preconditions.PreConditions.assertNotNull;

public interface Matcher<T> extends Validator<T> {

    @Override
    Validation<Optional<T>> apply(final Optional<T> actual);

    final record IsMatcher<T>(T expected) implements Matcher<T> {

        public IsMatcher {
            assertNotNull(expected);
        }

        @Override
        public Validation<Optional<T>> apply(final Optional<T> actual) {
            if (actual.isEmpty()) {
                return Validation.failure("Found nothing");
            }
            if (expected.equals(actual.get())) {
                return Validation.success(actual);

            }

            return Validation.failure("Expected " + expected + " but found " + actual.get());
        }
    }
}
