package io.snice.testing.http.stack;

import io.snice.codecs.codec.http.HttpRequest;
import io.snice.testing.http.protocol.HttpTransaction;

public interface HttpStack {

    HttpTransaction.Builder newTransaction(HttpRequest request);

}
