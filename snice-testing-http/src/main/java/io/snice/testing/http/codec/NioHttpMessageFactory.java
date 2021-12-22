package io.snice.testing.http.codec;

import io.snice.codecs.codec.http.HttpMessage.Builder;
import io.snice.codecs.codec.http.HttpMessageFactory;
import io.snice.codecs.codec.http.HttpMethod;
import io.snice.codecs.codec.http.HttpRequest;

import java.net.URI;

public class NioHttpMessageFactory implements HttpMessageFactory {

    @Override
    public Builder<HttpRequest> createRequest(final HttpMethod method, final URI target) {
        return NioHttpRequest.createRequest(method, target);
    }

}
