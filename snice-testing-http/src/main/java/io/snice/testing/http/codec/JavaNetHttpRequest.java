package io.snice.testing.http.codec;

import io.snice.codecs.codec.http.HttpMethod;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;

import static io.snice.preconditions.PreConditions.assertNotNull;

public final class JavaNetHttpRequest extends JavaNetHttpMessage implements io.snice.codecs.codec.http.HttpRequest {

    private final HttpRequest req;

    private JavaNetHttpRequest(final HttpRequest req) {
        this.req = req;
    }

    /**
     * Return the actual http request, which is the one we'll be sending off... This is an internal API only
     */
    public HttpRequest javaNetHttpRequest() {
        return req;
    }

    @Override
    protected HttpHeaders internalHeaders() {
        return req.headers();
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
            return new JavaNetHttpRequest(builder.build());
        }

    }
}
