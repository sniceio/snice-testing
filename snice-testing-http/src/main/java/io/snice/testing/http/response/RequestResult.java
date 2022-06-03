package io.snice.testing.http.response;

import io.snice.codecs.codec.http.HttpRequest;
import io.snice.codecs.codec.http.HttpResponse;
import io.snice.testing.http.protocol.HttpServerTransaction;

/**
 * When the {@link RequestProcessor#onRequest(HttpServerTransaction, HttpRequest)} is called, this is
 * the result that is produced.
 */
public record RequestResult(HttpResponse response,
                            boolean closeConnection,
                            boolean isLast) {
}
