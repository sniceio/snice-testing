package io.snice.testing.http;

import io.snice.buffer.Buffer;
import io.snice.codecs.codec.http.HttpMessage;
import io.snice.testing.core.Session;
import io.snice.testing.http.impl.ContentFactory;

import java.util.Map;

public interface Content<T> {

    T content();

    <T extends HttpMessage> HttpMessage.Builder<T> apply(Session session, HttpMessage.Builder<T> builder);

    static Content<Map<String, Object>> of(final Map<String, Object> formEncodedParams) {
        return ContentFactory.of(formEncodedParams);
    }

    static Content<Buffer> of(final Buffer content) {
        return ContentFactory.of(content);
    }

}
