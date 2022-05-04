package io.snice.identity.sri;

import io.snice.buffer.Buffer;
import io.snice.buffer.Buffers;

import java.util.Objects;
import java.util.function.Function;

import static io.snice.preconditions.PreConditions.assertArgument;

public sealed interface SniceResourceIdentifier permits ActionResourceIdentifier,
        GenericResourceIdentifier,
        ScenarioResourceIdentifier,
        SessionResourceIdentifier {

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

    sealed class BaseResourceIdentifier permits ActionResourceIdentifier, GenericResourceIdentifier, ScenarioResourceIdentifier, SessionResourceIdentifier {

        private final String prefix;
        private final Buffer uuid;

        protected BaseResourceIdentifier(final String prefix, final Buffer uuid) {
            this.prefix = prefix;
            this.uuid = uuid;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final BaseResourceIdentifier that = (BaseResourceIdentifier) o;
            return uuid.equals(that.uuid);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uuid);
        }

        protected static <T extends SniceResourceIdentifier> T from(final String expectedPrefix, final String raw, final Function<Buffer, T> builder) {
            // TODO: lots of checks needs to be done.
            if (raw == null || raw.length() != 32 + expectedPrefix.length()) {
                throw new IllegalArgumentException("The SRI is not exactly " + (32 + expectedPrefix.length())
                        + " characters long. Cannot be a proper SRI");
            }

            if (raw.startsWith(expectedPrefix)) {
                final var buffer = Buffers.wrapAsHex(raw.substring(expectedPrefix.length(), raw.length()));
                return builder.apply(buffer);
            }

            throw new IllegalArgumentException("The given raw string is not a valid SRI");
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
