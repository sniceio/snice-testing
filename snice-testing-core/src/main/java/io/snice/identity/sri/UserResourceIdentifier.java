package io.snice.identity.sri;

import io.snice.buffer.Buffer;

public final class UserResourceIdentifier extends SniceResourceIdentifier.BaseResourceIdentifier implements SniceResourceIdentifier {

    public static final String PREFIX = "USR";

    private UserResourceIdentifier(final Buffer raw) {
        super(PREFIX, raw);
    }

    public static UserResourceIdentifier from(final String sri) {
        return from(PREFIX, sri, UserResourceIdentifier::new);
    }

    public static UserResourceIdentifier of() {
        return new UserResourceIdentifier(SniceResourceIdentifier.uuid());
    }
}
