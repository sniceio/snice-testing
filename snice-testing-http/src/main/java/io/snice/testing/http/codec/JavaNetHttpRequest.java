package io.snice.testing.http.codec;

import io.snice.buffer.Buffer;
import io.snice.codecs.codec.http.HttpHeader;
import io.snice.codecs.codec.http.HttpMethod;
import io.snice.codecs.codec.http.HttpScheme;
import io.snice.codecs.codec.http.HttpVersion;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    public HttpScheme scheme() {
        return null;
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.valueOf(req.method().toUpperCase());
    }

    @Override
    public HttpVersion version() {
        return null;
    }

    @Override
    public URI uri() {
        return req.uri();
    }

    @Override
    public <T> Optional<HttpHeader<T>> header(final CharSequence name) {
        return Optional.empty();
    }

    @Override
    public <T> List<HttpHeader<T>> headers(final CharSequence name) {
        return null;
    }

    @Override
    public Optional<Buffer> content() {
        return Optional.empty();
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
        public Builder<io.snice.codecs.codec.http.HttpRequest> content(final Buffer content) {
            throw new RuntimeException("To be deleted");
        }

        @Override
        public Builder<io.snice.codecs.codec.http.HttpRequest> content(final Map<String, ?> content) {
            throw new RuntimeException("To be deleted");
        }

        @Override
        public Builder<io.snice.codecs.codec.http.HttpRequest> auth(final String username, final String password) {
            throw new RuntimeException("To be deleted");
        }

        @Override
        public Builder<io.snice.codecs.codec.http.HttpRequest> noDefaults() {
            throw new RuntimeException("To be deleted");
        }

        @Override
        public Builder<io.snice.codecs.codec.http.HttpRequest> version(final HttpVersion version) {
            return null;
        }

        @Override
        public io.snice.codecs.codec.http.HttpRequest build() {
            return new JavaNetHttpRequest(builder.build());
        }

    }
}
