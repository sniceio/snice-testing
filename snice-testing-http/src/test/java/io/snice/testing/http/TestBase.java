package io.snice.testing.http;

import io.snice.codecs.codec.http.HttpMethod;
import io.snice.testing.core.expression.Expression;
import io.snice.testing.http.protocol.HttpProtocol;
import io.snice.testing.http.stack.HttpStack;

import java.util.HashMap;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestBase {


    public HttpProtocol someHttpProtocol(final HttpStack stack) {
        return someHttpProtocol(stack, null);
    }

    public HttpProtocol someHttpProtocol() {
        return someHttpProtocol((String) null);
    }

    public HttpProtocol someHttpProtocol(final String baseUrl) {
        return someHttpProtocol(null, baseUrl);
    }

    public HttpProtocol someHttpProtocol(final HttpStack stack, final String baseUrl) {
        final var protocol = mock(HttpProtocol.class);
        when(protocol.baseUrl()).thenReturn(Optional.ofNullable(baseUrl == null ? null : Expression.of(baseUrl)));
        if (stack != null) {
            when(protocol.stack()).thenReturn(stack);
        }

        return protocol;
    }


    public HttpRequestDef someHttpRequest() {
        return someHttpRequest(null);
    }

    public HttpRequestDef someHttpRequest(final String baseUrl) {
        return someHttpRequest(baseUrl, new String[0]);
    }

    public HttpRequestDef someHttpRequest(final String baseUrl, final String... headers) {
        final Optional<Expression> baseExp = baseUrl == null ? Optional.empty() : Optional.of(Expression.of(baseUrl));
        final var map = new HashMap<String, Expression>();
        for (int i = 0; i < headers.length; i += 2) {
            map.put(headers[i], Expression.of(headers[i + 1]));
        }
        return new HttpRequestDef("Unit Testing", HttpMethod.GET, baseExp, null, map);
    }

}
