package io.snice.testing.core.protocol;

import java.util.Optional;

public interface ProtocolRegistry {

    /**
     * Obtain the {@link Protocol} for the given {@link Key}.
     */
    <T extends Protocol> Optional<T> protocol(Key key);

    interface Key {

    }
    

}
