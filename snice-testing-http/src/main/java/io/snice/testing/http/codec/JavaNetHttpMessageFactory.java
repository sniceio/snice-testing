package io.snice.testing.http.codec;

import io.snice.codecs.codec.http.HttpMessage.Builder;
import io.snice.codecs.codec.http.HttpMessageFactory;
import io.snice.codecs.codec.http.HttpMethod;
import io.snice.codecs.codec.http.HttpRequest;
import io.snice.codecs.codec.http.HttpResponse;

import java.net.URI;

public class JavaNetHttpMessageFactory implements HttpMessageFactory {

    @Override
    public Builder<HttpRequest> createRequest(final HttpMethod method, final URI target) {
        return JavaNetHttpRequest.createRequest(method, target);
    }

    @Override
    public Builder<HttpResponse> createResponse(final int status) {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public Builder<HttpResponse> createResponse(final int status, final String reasonPhrase) {
        throw new RuntimeException("not yet implemented");
    }

}
