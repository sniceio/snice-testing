package io.snice.testing.core.protocol;

import io.snice.testing.core.Session;
import io.snice.testing.core.scenario.Scenario;

/**
 * {@link Protocol} is the base interface for all protocols. Simply provides
 * the entry point into managing the stack (start/stop etc) as well
 * as life-cycle management operations for a {@link Session}.
 */
public interface Protocol {

    /**
     * The {@link ProtocolRegistry.Key} under which this {@link Protocol} is registered.
     */
    ProtocolRegistry.Key key();

    /**
     * Called when a particular "protocol" is asked to start.
     * <p>
     * Starting the protocol is typically only happening once when Snice
     * starts since it will configure the underlying stack and start to listen
     * to the configured ports etc. However, when a new {@link Scenario} starts we
     * will also call {@link #startSession(Session)}, which then may configure the underlying
     * stack with additional listening points just for that session.
     */
    default void start() {
    }

    /**
     * Whenever a new {@link Scenario} starts, it will call this method to potentially
     * configure the underlying protocol stack with additional listening points, certificates
     * etc. The {@link Session} will also be populated with information about the underlying protocol
     * stack, such as the ip:port of the listening point that will be used for the {@link Scenario} and
     * those will be made available through the {@link Session#}
     *
     * @param session
     * @return
     */
    default Session startSession(final Session session) {
        return session;
    }

    default void stop() {
    }

    default void stopSession(final Session session) {
    }

    static ProtocolRegistry.Key createKey(final String name, final Class<?> clazz) {
        return new ProtocolRegistry.Key() {

            @Override
            public String name() {
                return name;
            }

            @Override
            public int hashCode() {
                return clazz.hashCode();
            }

            @Override
            public boolean equals(final Object obj) {
                return clazz.equals(obj);
            }

            @Override
            public String toString() {
                return name();
            }
        };
    }

    interface Builder<T extends Protocol> {
        T build();
    }
}
