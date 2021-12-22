package io.snice.testing.http.codec;

import io.snice.codecs.codec.http.HttpHeader;
import io.snice.codecs.codec.http.HttpMessage;
import io.snice.codecs.codec.http.HttpMethod;

import java.net.URI;

public abstract class NioHttpBuilder<T extends HttpMessage> implements HttpMessage.Builder<T> {

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
