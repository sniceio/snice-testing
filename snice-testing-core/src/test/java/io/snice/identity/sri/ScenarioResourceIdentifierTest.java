package io.snice.identity.sri;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScenarioResourceIdentifierTest {


    @Test
    public void testCreateScenarioResourceIdentifier() {
        var sri = ScenarioResourceIdentifier.of();
        assertThat(sri.toString(), is(sri.asString()));
        assertThat(sri.toString().substring(0, 3), is("SCN"));

        sri = ScenarioResourceIdentifier.from("SCNDEADBEEFDEADBEEFDEADBEEFDEADBEEF");
        assertThat(sri.toString(), is("SCNDEADBEEFDEADBEEFDEADBEEFDEADBEEF"));
    }

    /**
     * Only exactly a buffer of 16 bytes is allowed/expected. Test that and
     * ensure to test around the boundaries of 16
     * <p>
     * Note that the two DEADBEEF at the end are one character too short or one too long.
     */
    @ParameterizedTest
    @ValueSource(strings = {"APADEADBEEF", "SCNDEADBEEF", "SCNDEADBEEFDEADBEEFDEADBEEFDEADBEE", "SCNDEADBEEFDEADBEEFDEADBEEFDEADBEEFF"})
    public void testCreateBadScenarioResourceIdentifier(final String input) {
        assertThrows(IllegalArgumentException.class, () -> ScenarioResourceIdentifier.from(input));
    }

}