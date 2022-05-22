package io.snice.testing.http.impl;

import io.snice.buffer.Buffer;
import io.snice.buffer.Buffers;
import io.snice.codecs.codec.http.HttpMessage;
import io.snice.testing.core.Session;
import io.snice.testing.core.common.Expression;
import io.snice.testing.http.Content;

import java.util.HashMap;
import java.util.Map;

import static io.snice.preconditions.PreConditions.assertArgument;
import static io.snice.preconditions.PreConditions.assertNotNull;

public class ContentFactory {

    public static Content<Map<String, Expression>> of(final Map<String, Expression> formEncodedParams) {
        assertNotNull(formEncodedParams);
        assertArgument(!formEncodedParams.isEmpty(), "You must specify at least one form-encoded param");
        return new FormEncodedContent(formEncodedParams);
    }

    public static Content<Buffer> of(final Buffer content) {
        Buffers.assertNotEmpty(content);
        return new RawContent(content);
    }

    private record FormEncodedContent(Map<String, Expression> content) implements Content<Map<String, Expression>> {

        @Override
        public <T extends HttpMessage> HttpMessage.Builder<T> apply(final Session session, final HttpMessage.Builder<T> builder) {
            final Map<String, String> processed = new HashMap<>();
            content.entrySet().stream().forEach(e -> processed.put(e.getKey(), e.getValue().apply(session)));
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
