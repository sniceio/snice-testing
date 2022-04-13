package io.snice.identity.sri;

import io.snice.buffer.Buffer;
import io.snice.buffer.Buffers;

import static io.snice.preconditions.PreConditions.assertArgument;

public sealed interface SniceResourceIdentifier permits GenericResourceIdentifier, ScenarioResourceIdentifier, SessionResourceIdentifier {

    /**
     * The three character prefix.
     */
    String prefix();

    /**
     * The raw SRI but without the prefix.
     */
    Buffer raw();

    default String asString() {
        return prefix() + raw().toHexString(false);
    }

    static void validateRaw(final Buffer raw) {
        assertArgument(raw != null && raw.capacity() == 16, "The SRI consists of a 3 character prefix and " +
                "a 32 hex-digit long UUID (16 bytes). The supplied raw Buffer was not 16 bytes long");
    }

    static Buffer uuid() {
        return Buffers.uuid();
    }

    sealed class BaseResourceIdentifier permits GenericResourceIdentifier, ScenarioResourceIdentifier, SessionResourceIdentifier {

        private final String prefix;
        private final Buffer uuid;

        protected BaseResourceIdentifier(final String prefix, final Buffer uuid) {
            this.prefix = prefix;
            this.uuid = uuid;
        }

        public String prefix() {
            return prefix;
        }

        public Buffer raw() {
            return uuid;
        }

        @Override
        public String toString() {
            return prefix() + uuid.toHexString(false);
        }

    }

}
