package io.snice.identity.sri;

import io.snice.buffer.Buffer;

public final class AccountResourceIdentifier extends SniceResourceIdentifier.BaseResourceIdentifier implements SniceResourceIdentifier {

    public static final String PREFIX = "ACT";

    private AccountResourceIdentifier(final Buffer raw) {
        super(PREFIX, raw);
    }

    public static AccountResourceIdentifier from(final String sri) {
        return from(PREFIX, sri, AccountResourceIdentifier::new);
    }

    public static AccountResourceIdentifier fromString(final String sri) {
        return from(sri);
    }

    public static AccountResourceIdentifier of() {
        return new AccountResourceIdentifier(SniceResourceIdentifier.uuid());
    }
}
