package io.snice.identity.sri;

import io.snice.buffer.Buffer;

public final class SessionResourceIdentifier extends SniceResourceIdentifier.BaseResourceIdentifier implements SniceResourceIdentifier {

    public static final String PREFIX = "SES";

    private SessionResourceIdentifier(final Buffer raw) {
        super(PREFIX, raw);
    }

    public static SessionResourceIdentifier from(final String sri) {
        return from(PREFIX, sri, SessionResourceIdentifier::new);
    }

    public static SessionResourceIdentifier of() {
        return new SessionResourceIdentifier(SniceResourceIdentifier.uuid());
    }
}
