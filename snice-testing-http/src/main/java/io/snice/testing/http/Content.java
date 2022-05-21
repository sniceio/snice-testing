package io.snice.testing.http;

import io.snice.testing.core.common.Expression;
import io.snice.testing.http.impl.ContentFactory;

import java.util.Map;

public interface Content<T> {

    T content();

    static Content<Map<String, Expression>> of(final Map<String, Expression> formEncodedParams) {
        return ContentFactory.of(formEncodedParams);
    }

}
