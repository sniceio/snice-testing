package io.snice.testing.http.protocol;

import io.snice.codecs.codec.http.HttpRequest;
import io.snice.codecs.codec.http.HttpResponse;

import java.util.function.BiConsumer;

public interface HttpTransaction {

    interface Builder {

        /**
         * Called once the {@link HttpTransaction} receives any answer and as such, the
         * transaction completes.
         *
         * @param f
         * @return
         */
        Builder onResponse(BiConsumer<HttpTransaction, HttpResponse> f);

        /**
         * Build and start the actual {@link HttpTransaction}. Once the transaction is started,
         * the actual {@link HttpRequest} will be sent.
         */
        HttpTransaction start();
    }
}
