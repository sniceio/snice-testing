package io.snice.testing.http;

/**
 * A builder for specifying how to construct an HTTP request. The builder itself is immutable, which
 * means everytime a new value is added to the builder, a new instance is created. This allows for
 * creating "template" builders, which otherwise wouldn't be possible if, by adding new values, changed
 * the "template".
 * <p>
 * Once a HTTP request has been fully been specified, this builder, when asked, will produce a
 * {@link HttpRequestDef}. The main difference between the {@link HttpRequestDef} and this {@link HttpRequestBuilder}
 * is just that the request definition is GUARANTEED to be complete and accurate, whereas the builder does not.
 * <p>
 * TODO: add examples
 */
public interface HttpRequestBuilder {

    /**
     * Specify the base URL for built off of this {@link HttpRequestBuilder}.
     * <p>
     * If you do not specify the base URL, then the base URL configured on the {@link HttpProtocolBuilder}
     * will be used. If neither have been specified and a FQDN has not been given when specifying which
     * HTTP method to use (so e.g. {@link #get(String)} or {@link #post(String)}, then an exception will
     * be thrown when the test is about to execute and it will fail.
     *
     * @param uri
     * @return
     */
    HttpRequestBuilder baseUrl(String uri);

    /**
     * Add a new HTTP header to this builder.
     *
     * @param name  the name of the header
     * @param value the value of the header. The value can be a variable expression.
     * @return
     */
    HttpRequestBuilder header(String name, String value);

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
    default HttpRequestBuilder asJson() {
        return header("Accept", "application/json").
                header("Content-Type", "application/json");
    }


    /**
     * Specify that this HTTP request is a GET and the URI to fetch.
     * <p>
     * You can specify a FQDN, in which case the {@link #baseUrl(String)}
     * will have no effect on this request. If you just specify a path, the
     * {@link #baseUrl(String)} will be pre-pended to form the FQDN.
     * <p>
     * Also, the URI is allowed to contain variable expression, which will be expanded
     * when the test executes. This also means that the validity of the resulting URI
     * cannot be verified (if it is a variable expression) until the execution starts.
     *
     * @param uri the URI to fetch, which is either a FQDN or a path that will be appended to the base URL.
     * @return a new instance of the {@link HttpRequestBuilder} with the new URI for the GET.
     * @throws IllegalArgumentException in case the URI is null or malformed.
     */
    HttpRequestBuilder get(String uri) throws IllegalArgumentException;

    /**
     * Same as {@link #get(String)} but allows you to not specify any further information in the URI,
     * in which case the {@link #baseUrl(String)} MUST have been specified when the HTTP request
     * eventually is constructed.
     *
     * @return a new instance of the {@link HttpRequestBuilder} with the URI for the
     * GET set to that of the {@link #baseUrl(String)}.
     */
    HttpRequestBuilder get();

    HttpRequestBuilder post(String uri);

    HttpRequestBuilder post();

    /**
     * Build a {@link HttpRequestDef}. You can call this method several times
     * and we will keep building new instances of {@link HttpRequestDef}. Of course, since
     * a {@link HttpRequestBuilder} is mutable, you will be getting the exact same info in
     * the definition.
     *
     * @return a new instance of a {@link HttpRequestDef}
     */
    HttpRequestDef build(HttpProtocol protocol);

}
