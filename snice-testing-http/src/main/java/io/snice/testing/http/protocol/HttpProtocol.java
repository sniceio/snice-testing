package io.snice.testing.http.protocol;

import io.snice.identity.sri.ActionResourceIdentifier;
import io.snice.identity.sri.ScenarioResourceIdentifier;
import io.snice.testing.core.common.Expression;
import io.snice.testing.core.protocol.Protocol;
import io.snice.testing.core.protocol.ProtocolRegistry;
import io.snice.testing.http.HttpConfig;
import io.snice.testing.http.protocol.impl.SniceHttpProtocol;
import io.snice.testing.http.stack.HttpStack;
import io.snice.testing.http.stack.HttpStackUserConfig;

import java.util.Map;
import java.util.Optional;

import static io.snice.preconditions.PreConditions.assertNotNull;

public interface HttpProtocol extends Protocol {

    ProtocolRegistry.Key httpProtocolKey = Protocol.createKey("HTTP", HttpProtocol.class);

    static HttpProtocolBuilder from(final HttpConfig config) {
        assertNotNull(config);
        return SniceHttpProtocol.newBuilder(config);
    }

    /**
     * Whenever any HTTP Action interacts with the underlying network stack, each action will ask for a "new" stack.
     * Each "stack" gets its own unique URL, should the action require incoming traffic. As the quotation marks
     * are alluding to, you don't actually get a new network stack per se. It's just a thin layer against the
     * real one and mainly serves as a dispatching layer should the action require to handle incoming traffic.
     *
     * @return
     */
    HttpStack newStack(ScenarioResourceIdentifier scenarioSri,
                       ActionResourceIdentifier actionSri,
                       HttpStackUserConfig config);

    HttpConfig config();

    Optional<Expression> baseUrl();

    interface HttpProtocolBuilder extends Protocol.Builder {

        HttpProtocolBuilder baseUrl(final String url);

        /**
         * Basic HTTP Authorization, which will be applied for all outgoing HTTP requests
         * unless explicitly overridden by the request itself (including no-auth);
         *
         * @param username the username to use, which is allowed to be an expression.
         * @param password the password to use, which is allowed to be an expression.
         * @return
         */
        HttpProtocolBuilder auth(String username, String password);

        /**
         * Should probably have a noDefaults here. See comment in
         * {@link io.snice.testing.http.InitiateHttpRequestBuilder#content(Map)} for comments
         * and the reason why we probably need this.
         * @return
         */
        // HttpProtocolBuilder noDefaults();
    }

}
