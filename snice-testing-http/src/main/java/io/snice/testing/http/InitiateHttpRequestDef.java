package io.snice.testing.http;

import io.snice.codecs.codec.http.HttpMethod;
import io.snice.codecs.codec.http.HttpResponse;
import io.snice.functional.Either;
import io.snice.testing.core.Session;
import io.snice.testing.core.action.ActionBuilder;
import io.snice.testing.core.check.Check;
import io.snice.testing.core.common.Expression;
import io.snice.testing.http.action.InitiateHttpRequestActionBuilder;
import io.snice.testing.http.protocol.HttpProtocol;
import io.snice.testing.http.stack.HttpStackUserConfig;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.snice.functional.Optionals.isAllEmpty;
import static io.snice.preconditions.PreConditions.assertArgument;
import static io.snice.preconditions.PreConditions.assertNotEmpty;
import static io.snice.preconditions.PreConditions.assertNotNull;

/**
 *
 */
public record InitiateHttpRequestDef(String requestName,
                                     HttpMethod method,
                                     Optional<Auth> auth,
                                     List<Check<HttpResponse>> checks,
                                     Optional<Expression> baseUrl,
                                     Optional<Expression> uri,
                                     Map<String, Expression> headers,
                                     Optional<Content<?>> content,
                                     HttpStackUserConfig config) {

    public InitiateHttpRequestDef {
        assertNotEmpty(requestName, "The HTTP request must have a name");
        assertNotNull(method, "You must specify the HTTP Method");
        auth = auth == null ? Optional.empty() : auth;
        baseUrl = baseUrl == null ? Optional.empty() : baseUrl;
        uri = uri == null ? Optional.empty() : uri;
        content = content == null ? Optional.empty() : content;
        headers = headers == null ? Map.of() : headers;
    }

    public InitiateHttpRequestDef(final String requestName, final HttpMethod method) {
        this(requestName, method, null, List.of(), null, null, null, null, null);
    }

    public static InitiateHttpRequestBuilder of(final String requestName, final HttpMethod method, final String uri) {
        assertNotEmpty(requestName);
        assertNotNull(method);
        return new DefaultInitiateHttpRequestBuilder(requestName, method, uri);
    }

    /**
     * The {@link InitiateHttpRequestDef} can optionally contain information about
     * the target URL but if not specified, the {@link HttpProtocol#baseUrl()}
     * will be used as the target. Also, any of the components may contain a
     * dynamic {@link Expression} which will be resolved by looking up the
     * corresponding expression within the given {@link Session}.
     * <p>
     * TODO: return a proper Error object instead. We can then accumulate errors along the way if
     * TODO: the scenario builder wants to make use of them somehow. Or do we actually bail out?
     *
     * @return
     */
    public Either<String, URL> resolveTarget(final HttpProtocol protocol, final Session session) {
        assertNotNull(protocol, "The given HTTP Protocol cannot be null");
        assertNotNull(session, "The given Session cannot be null");
        if (isAllEmpty(protocol.baseUrl(), baseUrl, uri)) {
            return Either.left("No URL was ever specified");
        }

        return createUrl(session, baseUrl.or(() -> protocol.baseUrl()));
    }

    private Either<String, URL> createUrl(final Session session, final Optional<Expression> base) {
        final var resolvedPath = uri.map(e -> e.apply(session));
        final var resolvedBase = base.map(exp -> exp.apply(session));

        return resolvedBase.map(b -> safeCreateUrl(b, resolvedPath)).orElseGet(() -> safeCreateUrl(resolvedPath.get()));
    }

    private static Either<String, URL> safeCreateUrl(final String base, final Optional<String> spec) {
        try {
            final var url = new URL(base);
            if (spec.isPresent()) {
                return Either.right(new URL(url, spec.get()));
            }
            return Either.right(url);
        } catch (final MalformedURLException e) {
            return Either.left("Invalid URL");
        }
    }

    private static Either<String, URL> safeCreateUrl(final String url) {
        try {
            return Either.right(new URL(url));
        } catch (final MalformedURLException e) {
            return Either.left("Invalid URL \"" + url + "\"");
        }
    }

    private static class DefaultInitiateHttpRequestBuilder implements InitiateHttpRequestBuilder {

        private static final String REQUEST_NAME_KEY = "REQUEST_NAME";

        private static final String HEADERS_KEY = "HEADERS";

        private static final String BASE_URL_KEY = "BASE_URL";

        private static final String PATH_KEY = "PATH";

        private static final String METHOD_KEY = "METHOD";

        private static final String CHECKS_KEY = "CHECKS";

        private static final String CONTENT = "CONTENT";

        private static final String AUTH = "AUTH";

        private static final String STACK_USER_CONFIG_KEY = "STACK_USER_CONFIG";

        /**
         * There is no good way for incremental building up an immutable object in java that doesn't
         * involve calling a constructor over and over with a massive argument list. Scala has
         * better support for this so let's just stick it all in a map and that way we can
         * generalize it.
         */
        private final Map<String, Object> values;

        private DefaultInitiateHttpRequestBuilder(final String requestName,
                                                  final HttpMethod method,
                                                  final String uri) {
            this(createDefaultValues(requestName, method, uri));
        }

        private static Map<String, Object> createDefaultValues(final String requestName, final HttpMethod method, final String uri) {
            final var values = new HashMap<String, Object>();
            values.put(REQUEST_NAME_KEY, requestName);
            values.put(HEADERS_KEY, Collections.unmodifiableMap(new HashMap<String, Expression>()));
            values.put(METHOD_KEY, method);
            values.put(CHECKS_KEY, List.of());

            if (uri != null && !uri.isEmpty()) {
                values.put(PATH_KEY, Expression.of(uri));
            }
            return values;
        }

        private DefaultInitiateHttpRequestBuilder(final Map<String, Object> values) {
            this.values = Collections.unmodifiableMap(values);
        }

        @Override
        public InitiateHttpRequestBuilder baseUrl(final String url) {
            assertNotEmpty(url);
            try {
                final var expression = Expression.of(url);

                // if this is a static express then the base URL must be a valid URL
                // so check right away since that makes it easier for the user to blow
                // up early as opposed to later when the eventual action is executing.
                if (expression.isStatic()) {
                    new URL(url);
                }

                return extend(BASE_URL_KEY, Expression.of(url));
            } catch (final MalformedURLException e) {
                throw new IllegalArgumentException("The given base URL is malformed", e);
            }
        }

        private DefaultInitiateHttpRequestBuilder extendHeaders(final String name, final String value) {
            final var headers = (Map<String, Expression>) values.get(HEADERS_KEY);
            return extend(HEADERS_KEY, extendMap(headers, name, value));
        }

        private DefaultInitiateHttpRequestBuilder extendChecks(final Check<HttpResponse> check) {
            final var checks = (List<Check<HttpResponse>>) values.get(CHECKS_KEY);
            final var newChecks = new ArrayList<>(checks);
            newChecks.add(check);
            return extend(CHECKS_KEY, Collections.unmodifiableList(newChecks));
        }

        private DefaultInitiateHttpRequestBuilder extend(final String key, final Object value) {
            final var newValues = new HashMap<>(values);
            newValues.put(key, value);
            return new DefaultInitiateHttpRequestBuilder(newValues);
        }

        private Map<String, Expression> extendMap(final Map<String, Expression> map, final String key, final String value) {
            final var extended = new HashMap<>(map);
            extended.put(key, Expression.of(value));
            return Collections.unmodifiableMap(extended);
        }

        @Override
        public InitiateHttpRequestBuilder header(final String name, final String value) {
            assertNotEmpty(name);
            assertNotEmpty(value);
            return extendHeaders(name, value);
        }

        @Override
        public InitiateHttpRequestBuilder auth(final String username, final String password) {
            final var auth = Auth.basicAuth(Expression.of(username), Expression.of(password));
            return extend(AUTH, auth);
        }

        @Override
        public InitiateHttpRequestBuilder content(final Map<String, Object> content) {
            assertNotNull(content);
            assertArgument(!content.isEmpty());
            return extend(CONTENT, Content.of(content));
        }

        @Override
        public InitiateHttpRequestBuilder check(final Check<HttpResponse> check) {
            assertNotNull(check);
            return extendChecks(check);
        }

        @Override
        public InitiateHttpRequestDef build() {
            final var requestName = (String) values.get(REQUEST_NAME_KEY);
            final var baseUrl = (Expression) values.get(BASE_URL_KEY);
            final var headers = (Map<String, Expression>) values.get(HEADERS_KEY);
            final var target = (Expression) values.get(PATH_KEY);
            final var method = (HttpMethod) values.get(METHOD_KEY);
            final var checks = (List<Check<HttpResponse>>) values.get(CHECKS_KEY);
            final Optional<Content<?>> content = Optional.ofNullable((Content<?>) values.get(CONTENT));
            final Optional<Auth> auth = Optional.ofNullable((Auth) values.get(AUTH));

            assertNotNull(method, "You must specify the method of the request");

            // TODO
            final var config = new HttpStackUserConfig();
            return new InitiateHttpRequestDef(requestName, method, auth, checks, Optional.ofNullable(baseUrl),
                    Optional.ofNullable(target), headers, content, config);
        }

        @Override
        public ActionBuilder toActionBuilder() {
            return new InitiateHttpRequestActionBuilder(this);
        }
    }
}
