package io.snice.identity.sri;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActionResourceIdentifierTest {


    @Test
    public void testCreateActionResourceIdentifier() {
        var sri = ActionResourceIdentifier.of();
        assertThat(sri.toString(), is(sri.asString()));
        assertThat(sri.toString().substring(0, 3), is("ACN"));

        sri = ActionResourceIdentifier.from("ACNDEADBEEFDEADBEEFDEADBEEFDEADBEEF");
        assertThat(sri.toString(), is("ACNDEADBEEFDEADBEEFDEADBEEFDEADBEEF"));
    }

    @Test
    public void testEqual() {
        final var sri1 = ActionResourceIdentifier.from("ACNDEADBEEFDEADBEEFDEADBEEFDEADBEEF");
        final var sri2 = ActionResourceIdentifier.from("ACNDEADBEEFDEADBEEFDEADBEEFDEADBEEF");
        assertThat(sri1, is(sri2));
        assertThat(sri1.hashCode(), is(sri2.hashCode()));

    }

    /**
     * Only exactly a buffer of 16 bytes is allowed/expected. Test that and
     * ensure to test around the boundaries of 16
     * <p>
     * Note that the two DEADBEEF at the end are one character too short or one too long.
     */
    @ParameterizedTest
    @ValueSource(strings = {"APADEADBEEF", "ACNDEADBEEF", "ACNDEADBEEFDEADBEEFDEADBEEFDEADBEE", "ACNDEADBEEFDEADBEEFDEADBEEFDEADBEEFF"})
    public void testCreateBadSessionResourceIdentifier(final String input) {
        assertThrows(IllegalArgumentException.class, () -> ActionResourceIdentifier.from(input));
    }

}