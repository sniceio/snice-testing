package io.snice.testing.http.stack;

import io.snice.codecs.codec.http.HttpRequest;
import io.snice.testing.core.action.Action;
import io.snice.testing.http.protocol.HttpServerTransaction;
import io.snice.testing.http.protocol.HttpTransaction;

import java.net.URL;
import java.time.Duration;

public interface HttpStack {

    HttpStackUserConfig config();

    /**
     * Whenever we wish to accept an incoming {@link HttpRequest}, we need to ask the {@link HttpStack}
     * to do so for us.
     */
    HttpServerTransaction.Builder newServerTransaction(Duration timeout);

    HttpTransaction.Builder newTransaction(HttpRequest request);

    /**
     * The address at which external clients can access this {@link HttpStack}. Typically, this is only used
     * for those {@link Action}s that are accepting traffic (e.g., an action that accepts an incoming webhook).
     * <p>
     * This address can then be used by other {@link Action}s to e.g. install a webhook along with an HTTP POST
     * request to the system under test.
     *
     * @return
     */
    URL address();

}
