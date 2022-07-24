package io.snice.testing.http.impl;

import io.snice.buffer.Buffer;
import io.snice.buffer.Buffers;
import io.snice.codecs.codec.http.HttpMessage;
import io.snice.testing.core.Session;
import io.snice.testing.core.common.Expression;
import io.snice.testing.http.Content;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.snice.preconditions.PreConditions.assertArgument;
import static io.snice.preconditions.PreConditions.assertNotNull;

public class ContentFactory {

    public static Content<Map<String, Object>> of(final Map<String, Object> formEncodedParams) {
        assertNotNull(formEncodedParams);
        assertArgument(!formEncodedParams.isEmpty(), "You must specify at least one form-encoded param");
        return new FormEncodedContent(formEncodedParams);
    }

    public static Content<Buffer> of(final Buffer content) {
        Buffers.assertNotEmpty(content);
        return new RawContent(content);
    }

    private record FormEncodedContent(Map<String, Object> content) implements Content<Map<String, Object>> {

        @Override
        public <T extends HttpMessage> HttpMessage.Builder<T> apply(final Session session, final HttpMessage.Builder<T> builder) {
            final var processed = content.entrySet().stream()
                    .map(e -> {
                        final Object newValue;
                        if (e.getValue() instanceof List) {
                            newValue = ((List) e.getValue()).stream()
                                    .map(v -> {
                                        final var expr = Expression.of(Objects.toString(v));
                                        return expr.apply(session);
                                    }).collect(Collectors.toUnmodifiableList());
                        } else {
                            newValue = Expression.of(Objects.toString(e.getValue())).apply(session);
                        }
                        return new AbstractMap.SimpleImmutableEntry<>(e.getKey(), newValue);
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            return builder.content(processed);
        }
    }

    private record RawContent(Buffer content) implements Content<Buffer> {
        @Override
        public <T extends HttpMessage> HttpMessage.Builder<T> apply(final Session session, final HttpMessage.Builder<T> builder) {
            return builder.content(content);
        }
    }

}
