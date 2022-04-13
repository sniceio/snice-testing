package io.snice.testing.http;

import io.snice.codecs.codec.http.HttpRequest;
import io.snice.testing.core.check.Check;

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
