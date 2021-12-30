package io.snice.testing.http.check;

import io.snice.codecs.codec.http.HttpMessage;
import io.snice.testing.core.Session;
import io.snice.testing.core.check.Check;
import io.snice.testing.core.check.CheckResult;
import io.snice.testing.core.check.Extractor;
import io.snice.testing.core.check.Validator;
import io.snice.testing.core.common.Validation;

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
    public Validation<CheckResult<?>> check(final T message, final Session session) {
        final var actual = extractor.apply(message).get();
        final var validation = validator.apply(actual);
        return validation.map(actualValue -> new CheckResult(actualValue, saveAs));
    }

    public HttpMessageCheck<T, X> saveAs(final String key) {
        assertNotEmpty(key);
        return new HttpMessageCheck<>(extractor, validator, Optional.of(key));
    }

}
