package io.snice.testing.http.stack;

import io.snice.codecs.codec.http.HttpRequest;
import io.snice.testing.core.action.Action;
import io.snice.testing.http.protocol.HttpTransaction;

import java.net.URL;

public interface HttpStack {

    HttpStackUserConfig config();

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
