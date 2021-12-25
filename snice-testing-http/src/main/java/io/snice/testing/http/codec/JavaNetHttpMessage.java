package io.snice.testing.http.codec;

import io.snice.codecs.codec.http.HttpHeader;
import io.snice.codecs.codec.http.HttpMessage;
import io.snice.codecs.codec.http.HttpMethod;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class JavaNetHttpMessage implements HttpMessage {


    @Override
    public <T> Optional<HttpHeader<T>> header(final String name) {
        return internalHeaders().firstValue(name).map(v -> new NioHttpHeader(name, v));
    }

    @Override
    public <T> List<HttpHeader<T>> headers(final String name) {
        return internalHeaders().allValues(name).stream().map(v -> new NioHttpHeader<T>(name, (T) v)).collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<HttpHeader<?>> headers() {
        final var headersMap = internalHeaders().map();
        final var headers = new ArrayList<HttpHeader<?>>();
        headersMap.entrySet().forEach(e -> {
            e.getValue().forEach(v -> headers.add(new NioHttpHeader(e.getKey(), v)));
        });
        return headers;
    }

    protected abstract HttpHeaders internalHeaders();

    private static record NioHttpHeader<T>(String name, T value) implements HttpHeader<T> {

    }

    public static abstract class NioHttpBuilder<T extends HttpMessage> implements HttpMessage.Builder<T> {

        private final HttpMethod method;
        private final URI target;

        protected NioHttpBuilder(final HttpMethod method, final URI target) {
            this.method = method;
            this.target = target;
        }

        @Override
        public HttpMessage.Builder<T> header(final String name, final String value) {
            return addHeader(name, value);
        }

        @Override
        public HttpMessage.Builder<T> header(final HttpHeader<?> header) {
            // TODO: not quite correct - will fix later
            return addHeader(header.name(), (String) header.value());
        }

        protected abstract HttpMessage.Builder<T> addHeader(String name, String value);

    }
}
