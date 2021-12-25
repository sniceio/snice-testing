package io.snice.testing.http.action;

import io.snice.testing.core.Session;
import io.snice.testing.core.action.Action;
import io.snice.testing.http.HttpRequestDef;
import io.snice.testing.http.protocol.HttpProtocol;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.MalformedURLException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class HttpRequestActionTest {

    @ParameterizedTest
    @CsvSource({", ", "hello, http://nisse.com"})
    public void testCreateTargetURL(final String protocolBaseUrl, final String httpDefBaseUrl) throws Exception {
        final HttpProtocol protocol = mockHttpProtocol(protocolBaseUrl);
        final HttpRequestDef def = mockRequestDef(httpDefBaseUrl);
        final TestAction next = new TestAction();

        final var action = new HttpRequestAction("Test", protocol, def, next);

        final var session = new Session("Testing");

        action.execute(session);

        assertThat(next.session.isFailed(), is(true));
        assertThat(next.session != session, is(true));
        assertThat(session.isSucceeded(), is(true));
    }

    private static class TestAction implements Action {

        Session session;

        @Override
        public String name() {
            return null;
        }

        @Override
        public void execute(final Session session) {
            this.session = session;
        }
    }

    private static HttpProtocol mockHttpProtocol(final String uri) throws MalformedURLException {
        final HttpProtocol protocol = mock(HttpProtocol.class);
        // final Optional<URL> uriMaybe = uri == null ? Optional.empty() : Optional.of(new
        // when(protocol.baseUrl()).thenReturn(uriMaybe);
        return protocol;
    }

    private static HttpRequestDef mockRequestDef(final String uri) throws MalformedURLException {
        final HttpRequestDef def = mock(HttpRequestDef.class);
        // final Optional<URL> uriMaybe = uri == null ? Optional.empty() : Optional.of(new URL(uri));
        // when(def.baseUrl()).thenReturn(uriMaybe);
        return def;
    }

}