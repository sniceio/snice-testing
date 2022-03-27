package io.snice.testing.core;

import io.snice.testing.core.action.ActionBuilder;

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

    ActionBuilder toActionBuilder();
}
