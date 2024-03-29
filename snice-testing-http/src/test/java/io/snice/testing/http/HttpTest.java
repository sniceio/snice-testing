package io.snice.testing.http;

import io.snice.testing.core.common.Expression;
import io.snice.testing.http.protocol.HttpProtocol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URL;
import java.util.Arrays;

import static io.snice.testing.http.HttpDsl.http;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(MockitoExtension.class)
public class HttpTest {

    /**
     * The {@link InitiateHttpRequestBuilder} is immutable, which means as we add/configure the builder,
     * we will continue to get a new instance. As such, once we build it we should actually have
     * different values.
     */
    @Test
    public void testCreateHttpObject(@Mock final HttpProtocol httpProtocol) throws Exception {

        final var baseUrl = "http://localhost:8080";
        // System.out.println(wm.getHttpBaseUrl());
        // System.out.println(wm.getHttpPort());
        // WireMock.stubFor(WireMock.get("/hello").willReturn(ok()));
        final var http1 = http("Fetch User Data").get()
                .baseUrl(baseUrl).header("Hello", "World");
        final var http3 = http1.asJson();

        final var def1 = http1.build();
        final var def3 = http3.build();
        final var def4 = http1.baseUrl("http://192.168.100.100").build();

        final var expectedBaseUrl = new URL(baseUrl);
        ensureUrl(def1, expectedBaseUrl);
        ensureUrl(def3, expectedBaseUrl);
        ensureUrl(def4, new URL("http://192.168.100.100"));

        // all have the same header Hello: World but "http3" as the "asJson"
        // added to it. Also note that the "asJson" was added after we changed
        // http1.baseUrl, which produced our "def4" but since the "asJson" would
        // have produced a new builder, those headers added by "asJson" will not be
        // on our def 4
        ensureHeaderCount(def1, 1);
        ensureHeaderCount(def3, 3);
        ensureHeaderCount(def4, 1);

        ensureHeader(def1, "Hello", "World");
        ensureHeader(def3, "Hello", "World");
        ensureHeader(def4, "Hello", "World");

        ensureHeaderDoesNotExist(def1, "Accept", "Content-Type");
        ensureHeaderDoesNotExist(def4, "Accept", "Content-Type");

        // but they do exist for the definition built by http3
        ensureHeader(def3, "Accept", "application/json");
        ensureHeader(def3, "Content-Type", "application/json");
    }

    private void ensureHeader(final InitiateHttpRequestDef def, final String name, final String expectedValue) {
        final var headerExpression = def.headers().get(name);
        assertThat(headerExpression.isStatic(), is(true));
        assertThat(headerExpression, is(Expression.of(expectedValue)));
    }

    private void ensureHeaderDoesNotExist(final InitiateHttpRequestDef def, final String... names) {
        Arrays.asList(names).forEach(name -> assertThat(def.headers().get(name), is(nullValue())));
    }

    private void ensureHeaderCount(final InitiateHttpRequestDef def, final int expected) {
        assertThat(def.headers().size(), is(expected));
    }

    private void ensureUrl(final InitiateHttpRequestDef def, final URL expected) {
        // for our tests, all (so far) are static expressions.
        final var expression = def.baseUrl().get();
        assertThat(expression.isStatic(), is(true));
        assertThat(expression, is(Expression.of(expected.toString())));

    }
}
