package io.snice.identity.sri;

import io.snice.buffer.Buffer;

public final class RepositoryResourceIdentifier extends SniceResourceIdentifier.BaseResourceIdentifier implements SniceResourceIdentifier {

    public static final String PREFIX = "RPO";

    private RepositoryResourceIdentifier(final Buffer raw) {
        super(PREFIX, raw);
    }

    public static RepositoryResourceIdentifier from(final String sri) {
        return from(PREFIX, sri, RepositoryResourceIdentifier::new);
    }

    public static RepositoryResourceIdentifier fromString(final String sri) {
        return from(sri);
    }

    public static RepositoryResourceIdentifier of() {
        return new RepositoryResourceIdentifier(SniceResourceIdentifier.uuid());
    }
}
