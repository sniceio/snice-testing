package io.snice.testing.core.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Because Java is really annoying and doesn't have true immutable lists.
 */
public interface ListOperations {

    /**
     * Extend the given list with the element.
     *
     * @param list
     * @param element
     * @param <T>
     * @return
     */
    static <T> List<T> extendList(final List<T> list, final T element) {
        final var l = new ArrayList<>(list);
        l.add(element);
        return Collections.unmodifiableList(l);
    }
}
