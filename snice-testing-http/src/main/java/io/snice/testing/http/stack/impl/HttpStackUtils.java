package io.snice.testing.http.stack.impl;

import io.snice.identity.sri.SniceResourceIdentifier;

import java.net.URI;
import java.util.Optional;
import java.util.function.Function;

import static io.snice.preconditions.PreConditions.assertNotNull;

public class HttpStackUtils {

    /**
     * The incoming HTTP traffic will have a SRI baked in somewhere and this function finds and extracts that, or
     * returns an empty optional.
     */
    public static <T extends SniceResourceIdentifier> Optional<T> extractSri(final String prefix, final Function<String, T> creator, final URI uri) {
        assertNotNull(uri);
        final var path = uri.getPath().toUpperCase();
        final var index = path.indexOf(prefix);
        if (index == -1) {
            return Optional.empty();
        }

        return Optional.of(path.substring(index, index + prefix.length() + SniceResourceIdentifier.LENGTH)).map(creator::apply);
    }
}
