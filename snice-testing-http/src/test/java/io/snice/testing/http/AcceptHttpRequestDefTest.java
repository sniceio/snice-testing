package io.snice.testing.http;

import io.snice.codecs.codec.http.HttpMethod;
import org.junit.jupiter.api.Test;

import static io.snice.testing.http.check.HttpCheckSupport.header;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AcceptHttpRequestDefTest extends TestBase {

    @Test
    public void testCreateDefThroughBuilder() {
        final var def = AcceptHttpRequestDef.of("Unit Test", HttpMethod.POST, null, "hello")
                .header("Hello", "World")
                .header("Foo", "Boo")
                .build();


        assertThat(def.requestName(), is("Unit Test"));
        assertHeaders(def.headers(), "Hello", "World", "Foo", "Boo");
    }

    /**
     * When not specified, the response will be 200 OK.
     */
    @Test
    public void testDefaultResponseStatusCode() {
        var def = someAccept().build();
        assertThat(def.statusCode(), is(200));
        assertThat(def.reasonPhrase(), is("OK"));

        // we can specify just one or the other too
        def = someAccept().respond(400).build();
        assertThat(def.statusCode(), is(400));
        // TODO: this should be the default reason phrase.
        assertThat(def.reasonPhrase(), is("OK"));

    }

    /**
     * Builders are immutable and as such, as you add more "stuff" to the builder,
     * it shouldn't affect a previous builder. Ensure that that is true.
     */
    @Test
    public void testBuildersAreImmutable() {
        final var b1 = AcceptHttpRequestDef.of("Immutable Test", HttpMethod.GET, "/whatever", "hello").header("Apa", "Nisse");
        final var b2 = b1.header("One", "Two");
        final var b3 = b1.header("Three", "Four");

        assertHeaders(b1.build().headers(), "Apa", "Nisse");
        assertHeaders(b2.build().headers(), "Apa", "Nisse", "One", "Two");
        assertHeaders(b3.build().headers(), "Apa", "Nisse", "Three", "Four");

        // all "saveAs" should be the same though
        assertThat(b1.build().saveAs(), is("hello"));
        assertThat(b2.build().saveAs(), is("hello"));
        assertThat(b3.build().saveAs(), is("hello"));
    }

    @Test
    public void testBuilderThroughHttpDsl() {
        final var def = HttpDsl.http("Unit Test Accept").accept(HttpMethod.POST, "/whatever")
                .saveAs("unit_test")
                .header("apa", "monkey")
                .header("nisse", "hello")
                .check(header("Connection").is("Close").saveAs("connection_header"))
                .check(header("Hello").is("World").saveAs("hello_world"))
                .build();

        assertThat(def.saveAs(), is("unit_test"));
        assertHeaders(def.headers(), "apa", "monkey", "nisse", "hello");
        assertThat(def.checks().size(), is(1));
    }

}
