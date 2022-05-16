package io.snice.testing.http.protocol;

import io.snice.codecs.codec.http.HttpResponse;

public interface HttpServerTransaction {

    HttpResponse.Builder<HttpResponse> createResponse(int statusCode);

    void sendResponse(HttpResponse response);
}
