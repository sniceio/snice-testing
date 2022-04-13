package io.snice.identity.sri;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class SessionResourceIdentifierTest {


    @Test
    public void testCreateSessionResourceIdentifier() {
        final var sri = SessionResourceIdentifier.of();
        assertThat(sri.toString(), is(sri.asString()));
        assertThat(sri.toString().substring(0, 3), is("SES"));
    }

    /**
     * Only exactly a buffer of 16 bytes is allowed/expected. Test that and
     * ensure to test around the boundaries of 16
     */
    /*
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 14, 15, 17, 18})
    public void testCreateBadSessionResourceIdentifier(final int size) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new SessionResourceIdentifier(Buffers.random(size));
        });
    }
     */

}