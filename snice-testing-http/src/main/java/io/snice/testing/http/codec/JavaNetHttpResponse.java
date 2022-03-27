package io.snice.testing.http.codec;

import io.snice.buffer.Buffer;
import io.snice.codecs.codec.http.HttpHeader;

import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

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
    public int statusCode() {
        return resp.statusCode();
    }

    @Override
    public String reasonPhrase() {
        return "N/A";
    }

    @Override
    protected HttpHeaders internalHeaders() {
        return resp.headers();
    }

    @Override
    public <T> Optional<HttpHeader<T>> header(final CharSequence name) {
        throw new RuntimeException("To be deleted");
    }

    @Override
    public <T> List<HttpHeader<T>> headers(final CharSequence name) {
        throw new RuntimeException("To be deleted");
    }

    @Override
    public Optional<Buffer> content() {
        return Optional.empty();
    }
}
