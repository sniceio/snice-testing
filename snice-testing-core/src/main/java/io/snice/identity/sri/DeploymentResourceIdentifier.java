package io.snice.identity.sri;

import io.snice.buffer.Buffer;

public final class DeploymentResourceIdentifier extends SniceResourceIdentifier.BaseResourceIdentifier implements SniceResourceIdentifier {

    public static final String PREFIX = "DPL";

    private DeploymentResourceIdentifier(final Buffer raw) {
        super(PREFIX, raw);
    }

    public static DeploymentResourceIdentifier from(final String sri) {
        return from(PREFIX, sri, DeploymentResourceIdentifier::new);
    }

    public static DeploymentResourceIdentifier fromString(final String sri) {
        return from(sri);
    }

    public static DeploymentResourceIdentifier of() {
        return new DeploymentResourceIdentifier(SniceResourceIdentifier.uuid());
    }
}
