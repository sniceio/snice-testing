package io.snice.testing.http.codec;

import io.snice.codecs.codec.http.HttpHeader;
import io.snice.codecs.codec.http.HttpMethod;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.Optional;

import static io.snice.preconditions.PreConditions.assertNotNull;

public final class NioHttpRequest extends io.snice.codecs.codec.http.HttpRequest {

    private final HttpRequest req;

    private NioHttpRequest(final HttpRequest req) {
        this.req = req;
    }


    public static Builder<io.snice.codecs.codec.http.HttpRequest> createRequest(final HttpMethod method, final URI target) {
        assertNotNull(method);
        assertNotNull(target);
        return new HttpRequestBuilder(method, target);
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.valueOf(req.method().toUpperCase());
    }

    @Override
    public URI uri() {
        return req.uri();
    }

    @Override
    public <T> Optional<HttpHeader<T>> header(final String name) {
        return NioHttpHelper.header(req.headers(), name);
    }

    @Override
    public <T> List<HttpHeader<T>> headers(final String name) {
        return NioHttpHelper.headers(req.headers(), name);
    }

    @Override
    public List<HttpHeader<?>> headers() {
        return NioHttpHelper.headers(req.headers());
    }

    private static class HttpRequestBuilder extends NioHttpBuilder<io.snice.codecs.codec.http.HttpRequest> {

        private final java.net.http.HttpRequest.Builder builder;

        private HttpRequestBuilder(final HttpMethod method, final URI target) {
            super(method, target);
            builder = java.net.http.HttpRequest.newBuilder(target);
        }

        @Override
        protected Builder<io.snice.codecs.codec.http.HttpRequest> addHeader(final String name, final String value) {
            builder.header(name, value);
            return this;
        }

        @Override
        public io.snice.codecs.codec.http.HttpRequest build() {
            return new NioHttpRequest(builder.build());
        }

    }
}
