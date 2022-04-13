package io.snice.testing.http;

import io.snice.codecs.codec.http.HttpResponse;
import io.snice.testing.core.MessageBuilder;
import io.snice.testing.core.check.Check;
import io.snice.testing.http.protocol.HttpProtocol;

import java.util.Map;

/**
 * A builder for specifying how to construct an HTTP request. The builder itself is immutable, which
 * means everytime a new value is added to the builder, a new instance is created. This allows for
 * creating "template" builders, which otherwise wouldn't be possible if, by adding new values, changed
 * the "template".
 * <p>
 * Once a HTTP request has been fully been specified, this builder, when asked, will produce a
 * {@link InitiateHttpRequestDef}. The main difference between the {@link InitiateHttpRequestDef} and this {@link InitiateHttpRequestBuilder}
 * is just that the request definition is GUARANTEED to be complete and accurate, whereas the builder does not.
 * <p>
 * TODO: add examples
 */
public interface InitiateHttpRequestBuilder extends MessageBuilder {

    /**
     * Specify the base URL for built off of this {@link InitiateHttpRequestBuilder}.
     * <p>
     * If you do not specify the base URL, then the base URL configured on the {@link HttpProtocol.HttpProtocolBuilder}
     * will be used. If neither have been specified and a FQDN has not been given when specifying which
     * HTTP method to use (so e.g. {@link #get(String)} or {@link #post(String)}, then an exception will
     * be thrown when the test is about to execute and it will fail.
     *
     * @param uri
     * @return
     */
    InitiateHttpRequestBuilder baseUrl(String uri);

    /**
     * Add a new HTTP header to this builder.
     *
     * @param name  the name of the header
     * @param value the value of the header. The value can be a variable expression.
     * @return
     */
    InitiateHttpRequestBuilder header(String name, String value);

    /**
     * Add form-encoded data with the outgoing request. By default,
     * a <code>Content-Type</code> header of <code>application/x-www-form-urlencoded</code>
     * will automatically be added to the request (as will the <code>Content-Length</code> header)
     * <p>
     * TODO: If you do not wish this default behavior to take place, simply call XXXX and then you are in full control yourself.
     * TODO: Should probably add the noDefaults to the HttpProtocolBuilder.
     *
     * @param content
     * @return
     */
    InitiateHttpRequestBuilder content(Map<String, Object> content);

    /**
     * Convenience method for adding the following two headers:
     *
     * <code>
     * builder.header("Accept", "application/json");
     * builder.header("Content-Type", "application/json");
     * </code>
     *
     * @return
     */
    default InitiateHttpRequestBuilder asJson() {
        return header("Accept", "application/json").
                header("Content-Type", "application/json");
    }

    InitiateHttpRequestBuilder check(Check<HttpResponse> check);

    /**
     * Build a {@link InitiateHttpRequestDef}. You can call this method several times
     * and we will keep building new instances of {@link InitiateHttpRequestDef}. Of course, since
     * a {@link InitiateHttpRequestBuilder} is mutable, you will be getting the exact same info in
     * the definition.
     *
     * @return a new instance of a {@link InitiateHttpRequestDef}
     */
    InitiateHttpRequestDef build();

}
