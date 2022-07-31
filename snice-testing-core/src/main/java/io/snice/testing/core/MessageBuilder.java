package io.snice.testing.core;

import io.snice.testing.core.action.ActionBuilder;
import io.snice.testing.core.protocol.Protocol;
import io.snice.testing.core.protocol.ProtocolRegistry;

/**
 * All protocols will send requests and those are specified through
 * a <code>builder</code>. The {@link MessageBuilder} is the base class
 * for all message based builders and has two main purposes:
 * <ul>
 *     <li>Being a tag-interface</li>
 *     <li>Ensure that a builder can be converted into an `ActionBuilder`</li>
 * </ul>
 */
public interface MessageBuilder {

    /**
     * The {@link Protocol} needed to execute the message.
     * <p>
     * In plain English, if the resulting
     * message is an HTTP request we will need an HTTP stack available to us or it won't work.
     *
     * @return
     */
    ProtocolRegistry.Key protocol();

    ActionBuilder toActionBuilder();
}
