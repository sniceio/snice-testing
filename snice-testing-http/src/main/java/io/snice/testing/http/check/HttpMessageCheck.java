package io.snice.testing.http.check;

import io.snice.codecs.codec.http.HttpMessage;
import io.snice.functional.Either;
import io.snice.testing.core.Session;
import io.snice.testing.core.check.Check;
import io.snice.testing.core.check.CheckResult;
import io.snice.testing.core.check.Extractor;
import io.snice.testing.core.check.Validator;

import java.util.Optional;

import static io.snice.preconditions.PreConditions.assertNotEmpty;
import static io.snice.preconditions.PreConditions.assertNotNull;

public record HttpMessageCheck<T extends HttpMessage, X>(Extractor<T, X> extractor,
                                                         Validator<X> validator,
                                                         Optional<String> saveAs) implements Check<T> {

    public HttpMessageCheck {
        assertNotNull(extractor);
        assertNotNull(validator);
        assertNotNull(saveAs);
    }

    @Override
    public CheckResult<T, X> check(final T message, final Session session) {
        final Either<Throwable, Optional<X>> actual = extractor.apply(message);

        return actual.fold(t -> {
            final var msg = "Failed to extract value due to exception " + t.getMessage();
            return new CheckResult<>(this, Optional.empty(), Optional.empty(), Optional.of(msg));
        }, actualValue -> {
            final var validation = validator.apply(actualValue);
            return validation.fold(
                    errMsg -> new CheckResult<>(this, Optional.empty(), saveAs, Optional.of(errMsg)),
                    v -> new CheckResult<>(this, v, saveAs, Optional.empty())
            );
        });
    }

    public HttpMessageCheck<T, X> saveAs(final String key) {
        assertNotEmpty(key);
        return new HttpMessageCheck<>(extractor, validator, Optional.of(key));
    }

}
