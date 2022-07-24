package io.snice.testing.http.stack.impl;

import io.snice.identity.sri.ActionResourceIdentifier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.util.Optional;

import static io.snice.testing.http.stack.impl.HttpStackUtils.extractSri;
import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class HttpStackUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = {"/hello/SRI", "http://localhost:1234/apa/SRI/hello?a=b#nisse"})
    public void testExtractSri(final String raw) throws Exception {
        // we'll "inject" the SRI into the string since it's easier than copy/paste that into all
        // @ValueSource above...
        final var sri = ActionResourceIdentifier.of();
        final var uri = new URI(raw.replaceAll("SRI", sri.asString()));

        final var sriMaybe = extractSri(ActionResourceIdentifier.PREFIX, ActionResourceIdentifier::from, uri);
        assertThat(sriMaybe, is(Optional.of(sri)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/hello/nope", "/", "http://localhost:1234/no/sri/here"})
    public void testExtractMissingSri(final String raw) throws Exception {
        final var uri = new URI(raw);
        final var sriMaybe = extractSri(ActionResourceIdentifier.PREFIX, ActionResourceIdentifier::from, uri);
        assertThat(sriMaybe, is(empty()));
    }

}