package io.snice.identity.sri;

import io.snice.buffer.Buffer;

public final class OrganizationResourceIdentifier extends SniceResourceIdentifier.BaseResourceIdentifier implements SniceResourceIdentifier {

    public static final String PREFIX = "ORG";

    private OrganizationResourceIdentifier(final Buffer raw) {
        super(PREFIX, raw);
    }

    public static OrganizationResourceIdentifier from(final String sri) {
        return from(PREFIX, sri, OrganizationResourceIdentifier::new);
    }

    public static OrganizationResourceIdentifier fromString(final String sri) {
        return from(sri);
    }

    public static OrganizationResourceIdentifier of() {
        return new OrganizationResourceIdentifier(SniceResourceIdentifier.uuid());
    }
}
