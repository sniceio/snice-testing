package io.snice.testing.http.codec;

import io.snice.codecs.codec.http.HttpMessageFactory;
import io.snice.codecs.codec.http.HttpProvider;

public final class NioHttpProvider implements HttpProvider {
    @Override
    public HttpMessageFactory messageFactory() {
        return new NioHttpMessageFactory();
    }

}
