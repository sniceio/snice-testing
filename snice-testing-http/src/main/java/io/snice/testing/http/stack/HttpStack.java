package io.snice.testing.http.stack;

import io.snice.codecs.codec.http.HttpRequest;
import io.snice.networking.common.Connection;
import io.snice.networking.common.ConnectionId;
import io.snice.testing.core.action.Action;
import io.snice.testing.http.protocol.HttpAcceptor;
import io.snice.testing.http.protocol.HttpTransaction;

import java.net.URL;
import java.time.Duration;
import java.util.function.BiConsumer;

public interface HttpStack {

    HttpStackUserConfig config();

    /**
     * Whenever we wish to accept an incoming {@link HttpRequest}, we need to ask the {@link HttpStack}
     * to do so for us.
     */
    HttpAcceptor.Builder newHttpAcceptor(Duration timeout);

    HttpTransaction.Builder newTransaction(HttpRequest request);

    /**
     * There are several events that occurs during the lifetime of a {@link Connection}. Often, they may be tied to
     * an {@link HttpTransaction} since that transaction may have established a new connection but e.g. due to
     * connection re-use, some connection events may occur long after the original {@link HttpTransaction} that
     * established the connection in the first place has terminated. Therefore, the connection events are treated
     * separately from the {@link HttpTransaction} and you register a callback to receive them all (or those that
     * were created through this {@link HttpStack}. Connections created through another instance
     * of the {@link HttpStack} will obviously not propagate to this callback.
     * <p>
     * also note that if the usage of a particular {@link HttpStack} is only to create a single {@link HttpTransaction}
     * then yes, the connection events that will occur are very much triggered due to this single transaction.
     *
     * @param f
     */
    void onConnectionEvents(BiConsumer<ConnectionId, Object> f);

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
