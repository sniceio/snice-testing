package io.snice.testing.http.protocol;

import io.snice.codecs.codec.http.HttpRequest;
import io.snice.testing.http.action.AcceptHttpRequestAction;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface HttpServerTransaction {

    interface Builder {

        Builder onRequest(BiConsumer<HttpServerTransaction, HttpRequest> f);

        /**
         * If we do not receive a request within a given timeout, this {@link AcceptHttpRequestAction} will
         * eventually give up.
         */
        Builder onTimeout(Consumer<HttpServerTransaction> f);

        HttpServerTransaction start();

    }
}
