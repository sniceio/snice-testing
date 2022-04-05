package io.snice.testing.http;

import io.snice.codecs.codec.http.HttpMethod;
import io.snice.testing.core.common.Expression;
import io.snice.testing.http.protocol.HttpProtocol;
import io.snice.testing.http.stack.HttpStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestBase {


    public static HttpProtocol someHttpProtocol(final HttpStack stack) {
        return someHttpProtocol(stack, null);
    }

    public static HttpProtocol someHttpProtocol() {
        return someHttpProtocol((String) null);
    }

    public static HttpProtocol someHttpProtocol(final String baseUrl) {
        return someHttpProtocol(null, baseUrl);
    }

    public static HttpProtocol someHttpProtocol(final HttpStack stack, final String baseUrl) {
        final var protocol = mock(HttpProtocol.class);
        when(protocol.baseUrl()).thenReturn(Optional.ofNullable(baseUrl == null ? null : Expression.of(baseUrl)));
        if (stack != null) {
            when(protocol.stack()).thenReturn(stack);
        }

        return protocol;
    }


    public static InitiateHttpRequestDef someHttpRequest() {
        return someHttpRequest(null);
    }

    public static InitiateHttpRequestDef someHttpRequest(final String baseUrl) {
        return someHttpRequest(baseUrl, new String[0]);
    }

    public static InitiateHttpRequestDef someHttpRequest(final String baseUrl, final String... headers) {
        final Optional<Expression> baseExp = baseUrl == null ? Optional.empty() : Optional.of(Expression.of(baseUrl));
        final var map = new HashMap<String, Expression>();
        for (int i = 0; i < headers.length; i += 2) {
            map.put(headers[i], Expression.of(headers[i + 1]));
        }
        return new InitiateHttpRequestDef("Unit Testing", HttpMethod.GET, List.of(), baseExp, null, map);
    }

    /**
     * Ensure that the given set of headers matches the expected ones.
     *
     * @param headers
     * @param expected pairs of name=value, which will be interpreted as header name and its corresponding value.
     */
    public static void assertHeaders(final Map<String, Expression> headers, final String... expected) {
        // just making sure you don't pass in bad stuff. Has to be an even number of expected headers
        // of we won't be able to make up header_name = value pairs.
        final var expectedCount = expected.length / 2;

        assertThat(expected.length % 2, is(0));
        assertThat(headers.size(), is(expectedCount));
        for (int i = 0; i < expected.length; i += 2) {
            assertThat(headers.get(expected[i]), is(Expression.of(expected[i + 1])));
        }
    }

}
