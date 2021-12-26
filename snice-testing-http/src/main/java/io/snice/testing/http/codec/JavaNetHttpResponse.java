package io.snice.testing.http.codec;

import io.snice.codecs.codec.http.HttpMethod;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;

import static io.snice.preconditions.PreConditions.assertNotNull;

public final class JavaNetHttpResponse<T> extends JavaNetHttpMessage implements io.snice.codecs.codec.http.HttpResponse {

    private final HttpResponse<T> resp;

    public static <T> JavaNetHttpResponse<T> of(final HttpResponse<T> resp) {
        assertNotNull(resp);
        return new JavaNetHttpResponse<>(resp);
    }

    private JavaNetHttpResponse(final HttpResponse<T> resp) {
        this.resp = resp;
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

    @Override
    protected HttpHeaders internalHeaders() {
        return resp.headers();
    }
}
