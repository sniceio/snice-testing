package io.snice.testing.http;

import io.snice.testing.core.expression.Expression;
import io.snice.testing.http.protocol.HttpProtocol;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestBase {

    public HttpProtocol someHttpProtocol(final String baseUrl) {
        final var protocol = mock(HttpProtocol.class);
        when(protocol.baseUrl()).thenReturn(Optional.ofNullable(baseUrl == null ? null : Expression.of(baseUrl)));
        return protocol;
    }

}
