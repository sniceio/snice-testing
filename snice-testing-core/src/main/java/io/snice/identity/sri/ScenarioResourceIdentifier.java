package io.snice.identity.sri;

import io.snice.buffer.Buffer;

public final class ScenarioResourceIdentifier extends SniceResourceIdentifier.BaseResourceIdentifier implements SniceResourceIdentifier {

    public static final String PREFIX = "SCN";

    private ScenarioResourceIdentifier(final Buffer raw) {
        super(PREFIX, raw);
    }

    public static ScenarioResourceIdentifier from(final String sri) {
        return from(PREFIX, sri, ScenarioResourceIdentifier::new);
    }

    public static ScenarioResourceIdentifier fromString(final String sri) {
        return from(sri);
    }

    public static ScenarioResourceIdentifier of() {
        return new ScenarioResourceIdentifier(SniceResourceIdentifier.uuid());
    }
}
