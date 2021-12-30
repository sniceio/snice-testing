package io.snice.testing.http.check;

import io.snice.codecs.codec.http.HttpMessage;
import io.snice.testing.core.check.Extractor;
import io.snice.testing.core.check.Matcher;

import java.util.Optional;

import static io.snice.preconditions.PreConditions.assertNotNull;

public class HttpCheckBuilder<T extends HttpMessage, X> {

    private final Extractor<T, X> extractor;
    private final Class<T> clazz;

    public static <T extends HttpMessage, X> HttpCheckBuilder<T, X> of(final Class<T> clazz, final Extractor<T, X> extractor) {
        assertNotNull(clazz);
        assertNotNull(extractor);
        return new HttpCheckBuilder<>(clazz, extractor);
    }

    private HttpCheckBuilder(final Class<T> clazz, final Extractor<T, X> extractor) {
        this.clazz = clazz;
        this.extractor = extractor;
    }

    public HttpMessageCheck<T, X> is(final X expectedValue) {
        final var matcher = new Matcher.IsMatcher<X>(expectedValue);
        return new HttpMessageCheck<T, X>(extractor, matcher, Optional.empty());
    }


}
