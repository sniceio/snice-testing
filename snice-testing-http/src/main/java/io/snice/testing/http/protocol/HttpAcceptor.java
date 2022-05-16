package io.snice.testing.http.protocol;

import io.snice.codecs.codec.http.HttpRequest;
import io.snice.codecs.codec.http.HttpResponse;
import io.snice.testing.core.scenario.Scenario;
import io.snice.testing.http.action.AcceptHttpRequestAction;

import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * TODO: cleanup the text below. Just dumping some thoughts for now...
 * <p>
 * The {@link HttpAcceptor} represents the ability to receive incoming HTTP traffic, typically used
 * in a {@link Scenario} for webhooks etc. Since webhooks often can receive multiple events before
 * being "done", the {@link HttpAcceptor} represents the "listening point" and for every new
 * incoming {@link HttpRequest} a new {@link HttpServerTransaction} is created and depending on how
 * the {@link HttpAcceptor} is configured, once the response is sent, either the {@link HttpAcceptor} also
 * terminates or it is still "alive" and waits for yet another request to arrive.
 * <p>
 * By default, the {@link HttpAcceptor} will terminate after a single HTTP transaction but you can configure it
 * to expect x no of incoming requests etc. However, sometimes it is hard to know exactly how many events
 * there are to show up so in order to make it a bit more flexible, a "terminateWhen" function can be passed
 * upon the {@link HttpAcceptor} creation.
 * <p>
 * Also, perhaps there should be a "send terminate event to acceptor" since e.g. if you issue another
 * request to the system under test, you may know that the next webhook should be the last one, or
 * something like that...
 */
public interface HttpAcceptor {

    interface Builder {

        Builder onRequest(BiFunction<HttpServerTransaction, HttpRequest, HttpResponse> f);

        /**
         * If we do not receive a request within a given timeout, this {@link AcceptHttpRequestAction} will
         * eventually give up.
         */
        Builder onTimeout(Consumer<HttpAcceptor> f);

        HttpAcceptor start();

    }
}
