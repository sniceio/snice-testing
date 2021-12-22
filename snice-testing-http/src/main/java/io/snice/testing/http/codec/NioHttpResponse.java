package io.snice.testing.http.codec;

import io.snice.codecs.codec.http.HttpHeader;
import io.snice.codecs.codec.http.HttpMethod;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

public final class NioHttpResponse<T> extends io.snice.codecs.codec.http.HttpResponse {

    private final HttpResponse<T> resp;

    private NioHttpResponse(final HttpResponse<T> resp) {
        this.resp = resp;
    }

    @Override
    public <T> Optional<HttpHeader<T>> header(final String name) {
        return NioHttpHelper.header(resp.headers(), name);
    }

    @Override
    public <T> List<HttpHeader<T>> headers(final String name) {
        return NioHttpHelper.headers(resp.headers(), name);
    }

    @Override
    public List<HttpHeader<?>> headers() {
        return NioHttpHelper.headers(resp.headers());
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.valueOf(resp.request().method().toUpperCase());
    }

    @Override
    public URI uri() {
        return resp.uri();
    }

    @Override
    public int statusCode() {
        return resp.statusCode();
    }

    @Override
    public String statusMessage() {
        return "N/A";
    }
}
