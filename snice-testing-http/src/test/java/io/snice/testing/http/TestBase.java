package io.snice.testing.http;

import io.snice.codecs.codec.http.HttpMessage;
import io.snice.codecs.codec.http.HttpMethod;
import io.snice.codecs.codec.http.HttpProvider;
import io.snice.codecs.codec.http.HttpRequest;
import io.snice.networking.http.impl.NettyHttpMessageFactory;
import io.snice.testing.core.common.Expression;
import io.snice.testing.http.protocol.HttpProtocol;
import io.snice.testing.http.stack.HttpStack;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestBase {

    @BeforeAll
    public static void setupEnvironment() {
        HttpProvider.setMessageFactory(new NettyHttpMessageFactory());
    }

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

    public static HttpMessage.Builder<HttpRequest> someHttpRequest(final String... headers) {
        assertThat(headers.length % 2, is(0));
        final var builder = someHttpRequest(HttpMethod.POST, "/hello");
        for (int i = 0; i < headers.length; i += 2) {
            builder.header(headers[i], headers[i + 1]);
        }
        return builder;
    }

    public static HttpMessage.Builder<HttpRequest> someHttpRequest(final HttpMethod method, final String uri) {
        try {
            return HttpRequest.create(method, new URI(uri));
        } catch (final URISyntaxException e) {
            Assertions.fail("Please pass in a proper URI. This test is not about testing the URI class!!!");
            throw new RuntimeException(e);
        }
    }

    public static InitiateHttpRequestDef someHttpRequestDef() {
        return someHttpRequestDef(null);
    }

    public static InitiateHttpRequestDef someHttpRequestDef(final String baseUrl) {
        return someHttpRequestDef(baseUrl, new String[0]);
    }

    public static InitiateHttpRequestDef someHttpRequestDef(final String baseUrl, final String... headers) {
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

    public static AcceptHttpRequestBuilder someAccept() {
        return someAccept("Unit Test", HttpMethod.GET);
    }

    public static AcceptHttpRequestBuilder someAccept(final String name, final HttpMethod method) {
        return AcceptHttpRequestDef.of(name, method, "/whatever", "hello");
    }


}
