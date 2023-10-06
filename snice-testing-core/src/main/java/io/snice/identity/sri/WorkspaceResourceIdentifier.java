package io.snice.identity.sri;

import io.snice.buffer.Buffer;

public final class WorkspaceResourceIdentifier extends SniceResourceIdentifier.BaseResourceIdentifier implements SniceResourceIdentifier {

    public static final String PREFIX = "WSP";

    private WorkspaceResourceIdentifier(final Buffer raw) {
        super(PREFIX, raw);
    }

    public static WorkspaceResourceIdentifier from(final String sri) {
        return from(PREFIX, sri, WorkspaceResourceIdentifier::new);
    }

    public static WorkspaceResourceIdentifier fromString(final String sri) {
        return from(sri);
    }

    public static WorkspaceResourceIdentifier of() {
        return new WorkspaceResourceIdentifier(SniceResourceIdentifier.uuid());
    }
}
