package io.snice.testing.core.common;

import java.util.function.Function;

import static io.snice.preconditions.PreConditions.assertNotEmpty;
import static io.snice.preconditions.PreConditions.assertNotNull;

public sealed interface Validation<T> permits Validation.Success, Validation.Failure {

    static <T> Success<T> success(final T value) {
        return new Success(value);
    }

    static Failure failure(final String msg) {
        return new Failure(msg);
    }

    <R> Validation<R> map(Function<T, R> f);

    <U> U fold(Function<String, ? extends U> failureMapper, Function<? super T, ? extends U> successMapper);

    T value();


    final record Success<T>(T v) implements Validation<T> {

        public Success {
            assertNotNull(v);
        }

        @Override
        public <R> Validation<R> map(final Function<T, R> f) {
            return new Success(f.apply(v));
        }

        @Override
        public <U> U fold(final Function<String, ? extends U> failureMapper, final Function<? super T, ? extends U> successMapper) {
            assertNotNull(successMapper);
            return successMapper.apply(v);
        }

        @Override
        public T value() {
            return v;
        }

    }

    final record Failure<T>(String msg) implements Validation<T> {

        public Failure {
            assertNotEmpty(msg);
        }

        @Override
        public <U> U fold(final Function<String, ? extends U> failureMapper, final Function<? super T, ? extends U> successMapper) {
            assertNotNull(successMapper);
            return failureMapper.apply(msg);
        }

        @Override
        public <R> Validation<R> map(final Function<T, R> f) {
            return new Failure(msg);
        }

        @Override
        public T value() {
            return null;
        }
    }
}
