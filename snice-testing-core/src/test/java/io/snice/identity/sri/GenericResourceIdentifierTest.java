package io.snice.identity.sri;

import io.snice.buffer.Buffers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class GenericResourceIdentifierTest {

    @Test
    public void testCreateGenericResourceIdentifier() {
        final var sri = GenericResourceIdentifier.of("AAA", Buffers.uuid());
        assertThat(sri.prefix(), is("AAA"));
        assertThat(sri.toString().substring(0, 3), is("AAA"));
    }

    /**
     * Only exactly three characters are allowed as an SRE prefix.
     */
    @ParameterizedTest
    @ValueSource(strings = {"A", "AA", "AAAA"})
    public void testCreateBadPrefix(final String prefix) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            GenericResourceIdentifier.of(prefix, Buffers.random(16));
        });
    }

}