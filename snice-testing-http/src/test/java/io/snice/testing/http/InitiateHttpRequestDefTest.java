package io.snice.testing.http;

import io.snice.codecs.codec.http.HttpMethod;
import io.snice.testing.core.Session;
import io.snice.testing.core.common.Expression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.snice.codecs.codec.http.HttpMethod.GET;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InitiateHttpRequestDefTest extends TestBase {

    private Session session;

    @BeforeEach
    public void setup() {
        session = new Session("Unit Test");
    }

    @Test
    public void testCreateHttpRequestDefThroughBuilder() {

        final var def = InitiateHttpRequestDef.of("Unit Test Get", HttpMethod.GET, "/nisse")
                .baseUrl("http://example.com")
                .header("hello", "world")
                .build();

        assertThat(def.requestName(), is("Unit Test Get"));
        final var headers = def.headers();
        assertHeaders(def.headers(), "hello", "world");
        assertThat(headers.get("hello").apply(session), is("world"));

        assertThat(def.baseUrl().get().apply(session), is("http://example.com"));
        assertThat(def.uri().get().apply(session), is("/nisse"));
    }

    /**
     * The annoying thing with Java records is that you cannot have a private
     * constructor in order to absolutely ensure that the object is created
     * properly. So even if there is a builder to aid in the construction of the
     * record, you still need to have a bunch of extra annoying code in the
     * canonical constructor to ensure no one is creating the record outside
     * of the builder.
     * <p>
     * Ensure that if you pass in null for the optional values and the
     * headers that optional empty and an empty map will be created.
     */
    @Test
    public void testCreateHttpRequestDef() {
        final var def = new InitiateHttpRequestDef("Test", GET, List.of(), null, null, null);
        assertThat(def.baseUrl(), is(Optional.empty()));
        assertThat(def.uri(), is(Optional.empty()));
        assertThat(def.headers(), is(Map.of()));
    }

    /**
     * The request name and http methods are NOT optional so make sure that we cannot
     * create one without them.
     */
    @ParameterizedTest
    @CsvSource({", ", "Bad Request, ", ", GET"})
    public void testCreateHttpRequestDefBadValues(final String name, final String method) {
        assertThrows(IllegalArgumentException.class, () -> new InitiateHttpRequestDef(name, method == null ? null : HttpMethod.valueOf(method)));
    }

    @Test
    public void testResolveURL() throws Exception {
        ensureResolve("http://hello.com", "http://hello.com", null, null);
        ensureResolve("http://hello.com", null, "http://hello.com", null);
        ensureResolve("http://hello.com", null, null, "http://hello.com");

        ensureResolve("http://hello.com/nisse", "http://hello.com", null, "/nisse");

        // the way that new URL(base, spec) works is that if the spec starts with a '/' (slash) then
        // the spec path is treated as absolute and the spec path is replacing the context path.
        ensureResolve("http://hello.com/nisse", "http://hello.com/will/be/removed", null, "/nisse");
        ensureResolve("http://hello.com/nisse", null, "http://hello.com/will/be/removed", "/nisse");

        // if, however, the spec path do not start with '/' (slash) then it will be appended IF the context
        // path has a '/' itself at the end.
        ensureResolve("http://hello.com/will/not/be/removed/nisse", "http://hello.com/will/not/be/removed/", null, "nisse");
        ensureResolve("http://hello.com/will/not/be/removed/nisse", null, "http://hello.com/will/not/be/removed/", "nisse");

        // otherwise the last part of the context path will be replaced with the spec path
        ensureResolve("http://hello.com/will/not/be/nisse", "http://hello.com/will/not/be/removed", null, "nisse");
        ensureResolve("http://hello.com/will/not/be/nisse", null, "http://hello.com/will/not/be/removed", "nisse");

        // If the HttpRequestDef base is present then it takes precedence over the protocol base url
        ensureResolve("http://example.com/nisse", "http://hello.com", "http://example.com", "/nisse");
    }

    private static void ensureResolve(final String expected, final String protocolBaseUrl, final String httpDefBaseUrl, final String httpDefUri) throws Exception {
        final var session = new Session("Hello");
        final var protocol = someHttpProtocol(protocolBaseUrl);
        final var defBase = Optional.ofNullable(httpDefBaseUrl == null ? null : Expression.of(httpDefBaseUrl));
        final var defUri = Optional.ofNullable(httpDefUri == null ? null : Expression.of(httpDefUri));
        final var def = new InitiateHttpRequestDef("Unit Test", GET, List.of(), defBase, defUri, null);

        assertThat(def.resolveTarget(protocol, session).get(), is(new URL(expected)));
    }


}