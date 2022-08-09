package io.snice.testing.http.protocol;

import io.snice.codecs.codec.http.HttpRequest;
import io.snice.codecs.codec.http.HttpResponse;
import io.snice.networking.common.event.ConnectionInfoEvent;
import io.snice.networking.common.event.SslInfoEvent;

import java.util.Optional;
import java.util.function.BiConsumer;

public interface HttpTransaction {

    Optional<Object> applicationData();

    interface Builder {

        /**
         * Called once the {@link HttpTransaction} receives any answer and as such, the
         * transaction completes.
         */
        Builder onResponse(BiConsumer<HttpTransaction, HttpResponse> f);

        /**
         * Register a function for any events (other than {@link HttpResponse}) that occurs within the HTTP
         * stack as it relates to this transaction. An event could be an {@link SslInfoEvent} that indicates the
         * success, or failure, of the SSL Handshake, should this {@link HttpRequest} go over
         * HTTPS (and we did not re-use an already existing connection). Or, as the connection closes, we may
         * be interested in the {@link ConnectionInfoEvent}, which tells us exactly how many bytes we sent/received.
         * <p>
         * Note: again, if we re-use an existing connection this will then count all of that traffic, not just this
         * single transaction.
         */
        // Builder onEvent(BiConsumer<HttpTransaction, Object> f);

        /**
         * <p>
         * Any data that the application has associated with this transaction.
         * <p>
         * Note that this data is completely transparent to the HTTP stack.
         */
        Builder applicationData(Object data);

        /**
         * Build and start the actual {@link HttpTransaction}. Once the transaction is started,
         * the actual {@link HttpRequest} will be sent.
         */
        HttpTransaction start();
    }
}
