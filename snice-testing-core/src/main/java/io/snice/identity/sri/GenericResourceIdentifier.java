package io.snice.identity.sri;

import io.snice.buffer.Buffer;

import static io.snice.preconditions.PreConditions.assertArgument;

public final class GenericResourceIdentifier extends SniceResourceIdentifier.BaseResourceIdentifier implements SniceResourceIdentifier {

    private GenericResourceIdentifier(final String prefix, final Buffer raw) {
        super(prefix, raw);
    }

    public static GenericResourceIdentifier of(final String prefix, final Buffer uuid) {
        assertArgument(prefix != null && prefix.length() == 3, "The SRI prefix must be exactly three characters long");
        SniceResourceIdentifier.validateRaw(uuid);
        return new GenericResourceIdentifier(prefix, uuid);
    }

    public static GenericResourceIdentifier of(final String prefix) {
        return of(prefix, SniceResourceIdentifier.uuid());
    }
}
