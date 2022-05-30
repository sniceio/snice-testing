package io.snice.testing.http.impl;

import io.snice.codecs.codec.http.HttpMessage;
import io.snice.codecs.codec.http.HttpMethod;
import io.snice.preconditions.PreConditions;
import io.snice.testing.core.check.Check;
import io.snice.testing.core.common.Expression;
import io.snice.testing.http.AcceptHttpRequestDef;
import io.snice.testing.http.Content;
import io.snice.testing.http.HttpMessageDefBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;

/**
 * @param <T>
 */
public sealed abstract class HttpMessageDefBuilderBase<T, M extends HttpMessage> permits AcceptHttpRequestBuilderImpl {

    protected static final String PARENT_KEY = "PARENT";

    protected static final String REQUEST_NAME_KEY = "REQUEST_NAME";

    protected static final String HEADERS_KEY = "HEADERS";

    protected static final String PATH_KEY = "PATH";

    protected static final String METHOD_KEY = "METHOD";

    protected static final String CHECKS_KEY = "CHECKS";

    protected static final String CONTENT = "CONTENT";

    /**
     * There is no good way for incremental building up an immutable object in java that doesn't
     * involve calling a constructor over and over with a massive argument list. Scala has
     * better support for this so let's just stick it all in a map and that way we can
     * generalize it.
     */
    private final Map<String, Object> values;

    protected HttpMessageDefBuilderBase(final String requestName, final HttpMethod method, final String path) {
        this(createDefaultValues(requestName, method, path));
    }

    protected HttpMessageDefBuilderBase(final HttpMessageDefBuilderBase<T, M> parent,
                                        final String requestName,
                                        final HttpMethod method,
                                        final Expression path) {
        this(createDefaultValues(parent, requestName, method, path));
    }

    protected Object get(final String key) {
        return values.get(key);
    }

    /**
     * NOTE: this constructor assumes that the map given to it is safe to use and has been properly
     * constructed by the sub-classes.
     *
     * @param values
     */
    protected HttpMessageDefBuilderBase(final Map<String, Object> values) {
        this.values = values;
    }

    private static <T, M extends HttpMessage> Map<String, Object> createDefaultValues(final HttpMessageDefBuilderBase<T, M> parent,
                                                                                      final String requestName,
                                                                                      final HttpMethod method,
                                                                                      final Expression uri) {
        final var values = new HashMap<String, Object>();
        if (parent != null) {
            values.put(PARENT_KEY, parent);
        }

        values.put(REQUEST_NAME_KEY, requestName);
        values.put(HEADERS_KEY, Collections.unmodifiableMap(new HashMap<String, Expression>()));
        values.put(METHOD_KEY, method);
        values.put(CHECKS_KEY, List.of());
        if (uri != null) {
            values.put(PATH_KEY, uri);
        }
        return values;
    }

    private static Map<String, Object> createDefaultValues(final String requestName, final HttpMethod method, final String uri) {
        final var expression = PreConditions.checkIfEmpty(uri) ? null : Expression.of(uri);
        return createDefaultValues(null, requestName, method, expression);
    }

    protected <T extends HttpMessageDefBuilder> T extendHeaders(final String name, final String value) {
        final var headers = (Map<String, Expression>) values.get(HEADERS_KEY);
        return extend(HEADERS_KEY, extendMap(headers, name, value));
    }

    protected <T extends HttpMessageDefBuilder> T extendChecks(final Check<M> check) {
        final var checks = (List<Check<M>>) values.get(CHECKS_KEY);
        final var newChecks = new ArrayList<>(checks);
        newChecks.add(check);
        return extend(CHECKS_KEY, Collections.unmodifiableList(newChecks));
    }

    protected <T extends HttpMessageDefBuilder> T extend(final String key, final Object value) {
        final var newValues = new HashMap<>(values);
        newValues.put(key, value);
        return newBuilder(newValues);
    }

    private static Map<String, Expression> extendMap(final Map<String, Expression> map, final String key, final String value) {
        final var extended = new HashMap<>(map);
        extended.put(key, Expression.of(value));
        return Collections.unmodifiableMap(extended);
    }

    public final T build() {
        return build(null);
    }

    protected final T build(final T child) {
        final Optional<HttpMessageDefBuilderBase<T, M>> parent =
                ofNullable((HttpMessageDefBuilderBase<T, M>) values.get(PARENT_KEY));

        final var requestName = (String) values.get(REQUEST_NAME_KEY);
        final var method = (HttpMethod) values.get(METHOD_KEY);
        final var target = (Expression) values.get(PATH_KEY);
        final var headers = (Map<String, Expression>) values.get(HEADERS_KEY);
        final var checks = (List<Check<M>>) values.get(CHECKS_KEY);
        final Optional<Content<?>> content = ofNullable((Content<?>) values.get(CONTENT));
        return internalBuild(values, parent, ofNullable(child), requestName, method, target, headers, content, checks);
    }

    protected HttpMethod method() {
        return (HttpMethod) values.get(METHOD_KEY);
    }

    protected Expression target() {
        return (Expression) values.get(PATH_KEY);
    }

    /**
     * Ask the sub-class to actually build the definition. The common parameters (name, headers, checks) etc
     * will be passed to this function but if the overriding sub-class wants to extract some non-standard
     * value, the raw value-map is included as well.
     *
     * @param values  the raw values map, in case the sub-class has something more beyond the common set of
     *                parameters for its definition.
     * @param parent  the optional parent for those definitions that have been chained. this is (currently)
     *                only for the {@link AcceptHttpRequestDef} where you can accept many incoming HTTP requests
     *                as part of a chain of webhooks.
     * @param child   an optional child definition for those definitions that allows to chain multiple together.
     * @param name    the name of the "definition/action" that is being built. It's mainly for
     *                human beings and will be included in logging, reports etc.
     * @param method  the HTTP method. When we are the ones initiating a request, this will be the method of that
     *                request. When we expect a request, the underlying protocol stack will match incoming HTTP
     *                requests on this method. If not matching, clearly it cannot belong to this "accept" definition.
     * @param target  the target, which when we are initiating the requests is where the request will ultimately be sent
     *                to (so it can be a FQDN and if not, the base URL will be used). If we are accepting an incoming
     *                HTTP request, this MUST only be the path.
     * @param headers The HTTP headers to add to the HTTP message that will ultimately be created. If we are
     *                initiating a request, we will add these headers to the HTTP request. If we are accepting an
     *                incoming request, then these are the headers that will be added to the outgoing HTTP response.
     * @param content an optional {@link Content}, which will be added as the body of the HTTP message.
     * @param checks  the checks to be performed. if we are the ones initiating the HTTP request, then these are checks
     *                that will be performed on the HTTP response. If we are accepting an incoming HTTP request, then
     *                these checks will be performed on that HTTP request.
     * @return
     */
    protected abstract T internalBuild(Map<String, Object> values,
                                       Optional<HttpMessageDefBuilderBase<T, M>> parent,
                                       final Optional<T> child,
                                       String name,
                                       HttpMethod method,
                                       Expression target,
                                       Map<String, Expression> headers,
                                       Optional<Content<?>> content,
                                       List<Check<M>> checks);

    protected abstract <T extends HttpMessageDefBuilder> T newBuilder(Map<String, Object> newValues);
}
