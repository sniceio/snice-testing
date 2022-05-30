package io.snice.testing.http;

import io.snice.buffer.Buffer;
import io.snice.buffer.Buffers;
import io.snice.codecs.codec.http.HttpRequest;
import io.snice.testing.core.check.Check;

import java.util.Map;

public interface AcceptHttpRequestBuilder extends HttpMessageDefBuilder {

    /**
     * Add a new HTTP header to this builder. This header will be used on the response
     * we are to send out as a result of the incoming HTTP request.
     *
     * @param name  the name of the header
     * @param value the value of the header. The value can be a variable expression.
     * @return
     */
    AcceptHttpRequestBuilder header(String name, String value);

    AcceptHttpRequestBuilder respond(int statusCode);

    /**
     * Add form-encoded data to the response. By default,
     * a <code>Content-Type</code> header of <code>application/x-www-form-urlencoded</code>
     * will automatically be added to the request (as will the <code>Content-Length</code> header)
     * <p>
     * TODO: If you do not wish this default behavior to take place, simply call XXXX and then you are in full control yourself.
     * TODO: Should probably add the noDefaults to the HttpProtocolBuilder.
     *
     * @param content
     * @return
     */
    AcceptHttpRequestBuilder content(Map<String, Object> content);

    AcceptHttpRequestBuilder content(Buffer content);

    /**
     * There are webhooks that expect many events to be delivered across it, and we need to
     * be able to define the behavior for each one of those events. E.g., in the Twilio API for
     * tracking the status of a call, the webhook for those "status events" tracks the state of
     * the call (ringing, answered, completed etc) and those are delivered over the same webhook
     * that was registered when the call was created.
     * <p>
     * By calling {@link #acceptNextRequest(String)} you signal that you're done defining the behavior for the
     * "current" request and want to move on to the next.
     *
     * @param name a human-readable name for the "next" request. It will be part of logging and will aid you
     *             when you need to follow the flow of your events. Name with care. But it has nothing to do with
     *             any business logic, so it really is only for you, the human developer (and in particular the person
     *             being on-call at 3am).
     * @return
     */
    AcceptHttpRequestBuilder acceptNextRequest(String name);

    default AcceptHttpRequestBuilder content(final String content) {
        return content(Buffers.wrap(content));
    }

    AcceptHttpRequestBuilder respond(int statusCode, String reasonPhrase);

    /**
     * A check to perform on the incoming {@link HttpRequest}.
     */
    AcceptHttpRequestBuilder check(Check<HttpRequest> check);

    /**
     * Build a {@link AcceptHttpRequestDef}. You can call this method several times
     * and we will keep building new instances of {@link AcceptHttpRequestDef}. Of course, since
     * a {@link AcceptHttpRequestBuilder} is mutable, you will be getting the exact same info in
     * the definition.
     *
     * @return a new instance of a {@link AcceptHttpRequestDef}
     */
    AcceptHttpRequestDef build();
}
