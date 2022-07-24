package io.snice.testing.http;

import io.snice.codecs.codec.http.HttpMethod;
import org.junit.jupiter.api.Test;

import static io.snice.codecs.codec.http.HttpMethod.POST;
import static io.snice.testing.http.check.HttpCheckSupport.header;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AcceptHttpRequestDefTest extends TestBase {

    @Test
    public void testCreateDefThroughBuilder() {
        final var def = AcceptHttpRequestDef.of("Unit Test", POST, null, "hello")
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
        final var def = HttpDsl.http("Unit Test Accept").accept(POST, "/whatever")
                .saveAs("unit_test")
                .header("apa", "monkey")
                .header("nisse", "hello")
                .check(header("Connection").is("Close").saveAs("connection_header"))
                .check(header("Hello").is("World").saveAs("hello_world"))
                .build();

        // status 200 is default if you don't specify it.
        assertDef(def, "unit_test", 200, 2, "apa", "monkey", "nisse", "hello");
    }

    @Test
    public void testMultipleAccepts() {

        final var def = HttpDsl.http("Multiple Accepts").accept(POST)
                .saveAs("multiple")
                .respond(555)
                .acceptNextRequest("Step Two")
                .check(header("Blah").not("Foo"))
                .respond(123)
                .acceptNextRequest("Final Step")
                .respond(100)
                .header("Child", "Two")
                .build();

        assertDef(def, "multiple", 555, 0);

        assertThat(countChildren(def), is(2));

        final var firstChild = getChild(1, def);
        assertDef(firstChild, "multiple", 123, 1);

        final var secondChild = getChild(2, def);
        assertDef(secondChild, "multiple", 100, 0, "Child", "Two");
    }

    private static void assertDef(final AcceptHttpRequestDef def, final String expectedSaveAs, final int expectedStatus, final int expectedChecksCount, final String... expectedHeaders) {
        assertThat(def.saveAs(), is(expectedSaveAs));
        assertThat(def.statusCode(), is(expectedStatus));
        assertHeaders(def.headers(), expectedHeaders);
        assertThat(def.checks().size(), is(expectedChecksCount));
    }

    private static AcceptHttpRequestDef getChild(int childNo, final AcceptHttpRequestDef parent) {
        if (childNo == 0) {
            return parent;
        }

        return getChild(--childNo, parent.child().get());
    }

    private static int countChildren(final AcceptHttpRequestDef def) {
        return countChildren(0, def);
    }

    private static int countChildren(int count, final AcceptHttpRequestDef def) {
        if (def.child().isPresent()) {
            return countChildren(++count, def.child().get());
        }
        return count;
    }


}
