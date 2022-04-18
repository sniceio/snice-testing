package io.snice.identity.sri;

import io.hektor.core.Actor;
import io.snice.buffer.Buffer;

/**
 * An {@link io.snice.testing.core.action.Action} is executed as its own FSM within an {@link Actor}
 * and has a unique SRI.
 */
public final class ActionResourceIdentifier extends SniceResourceIdentifier.BaseResourceIdentifier implements SniceResourceIdentifier {

    public static final String PREFIX = "ACN";

    private ActionResourceIdentifier(final Buffer raw) {
        super(PREFIX, raw);
    }

    public static ActionResourceIdentifier of() {
        return new ActionResourceIdentifier(SniceResourceIdentifier.uuid());
    }
}
