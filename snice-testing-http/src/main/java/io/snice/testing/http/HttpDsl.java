package io.snice.testing.http;


import io.snice.testing.core.SniceConfig;
import io.snice.testing.core.action.ActionBuilder;
import io.snice.testing.http.action.HttpRequestActionBuilder;
import io.snice.testing.http.protocol.HttpProtocol;
import io.snice.testing.http.protocol.HttpProtocol.HttpProtocolBuilder;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.snice.preconditions.PreConditions.assertNotEmpty;
import static io.snice.preconditions.PreConditions.ensureNotEmpty;

/**
 * Functions as a simple DSL to "kick-start" the creation of various HTTP
 * related objects, such as requests, configure the HTTP stack etc.
 */
public class HttpDsl {

    private HttpDsl() {
        // No instantiation of this class
    }

    public static HttpProtocolBuilder http(final SniceConfig configuration) {
        // TODO: how do you turn the SniceConfig into a http config?
        final var httpConfig = new HttpConfig();
        return HttpProtocol.of(httpConfig);
    }

    public static HttpRequestBuilder http(final String requestName) {
        ensureNotEmpty(requestName, "The name of the HTTP request cannot be empty");
        return new DefaultHttpRequestBuilder(requestName);
    }

    static record DefaultHttpRequestDef(String requestName,
                                        String method,
                                        Optional<URL> baseUrl,
                                        Map<String, String> headers) implements HttpRequestDef {
    }

    private static class DefaultHttpRequestBuilder implements HttpRequestBuilder {

        private static final String REQUEST_NAME_KEY = "REQUEST_NAME";

        private static final String HEADERS_KEY = "HEADERS";

        private static final String BASE_URL_KEY = "BASE_URL";

        private static final String PATH_KEY = "PATH";

        private static final String METHOD_KEY = "METHOD";

        /**
         * There is no good way for incremental building up an immutable object in java that doesn't
         * involve calling a constructor over and over with a massive argument list. Scala has
         * better support for this so let's just stick it all in a map and that way we can
         * generalize it.
         */
        private final Map<String, Object> values;

        private DefaultHttpRequestBuilder(final String requestName) {
            this(createDefaultValues(requestName));
        }

        private static Map<String, Object> createDefaultValues(final String requestName) {
            final var values = new HashMap<String, Object>();
            values.put(REQUEST_NAME_KEY, requestName);
            values.put(HEADERS_KEY, Collections.unmodifiableMap(new HashMap<String, String>()));
            values.put(BASE_URL_KEY, Optional.empty());
            values.put(METHOD_KEY, "GET");
            return values;
        }

        private DefaultHttpRequestBuilder(final Map<String, Object> values) {
            this.values = Collections.unmodifiableMap(values);
        }

        @Override
        public HttpRequestBuilder baseUrl(final String url) {
            assertNotEmpty(url);
            try {
                return extend(BASE_URL_KEY, Optional.of(new URL(url)));
            } catch (final MalformedURLException e) {
                throw new IllegalArgumentException("The given base URL is malformed", e);
            }
        }

        private DefaultHttpRequestBuilder extendHeaders(final String name, final String value) {
            final var headers = (Map<String, String>) values.get(HEADERS_KEY);
            return extend(HEADERS_KEY, extendMap(headers, name, value));
        }

        private DefaultHttpRequestBuilder extend(final String key, final Object value) {
            final var newValues = new HashMap<>(values);
            newValues.put(key, value);
            return new DefaultHttpRequestBuilder(newValues);
        }

        private Map<String, String> extendMap(final Map<String, String> map, final String key, final String value) {
            final var extended = new HashMap<>(map);
            extended.put(key, value);
            return Collections.unmodifiableMap(extended);
        }

        @Override
        public HttpRequestBuilder header(final String name, final String value) {
            assertNotEmpty(name);
            assertNotEmpty(value);
            return extendHeaders(name, value);
        }

        @Override
        public HttpRequestBuilder get(final String uri) throws IllegalArgumentException {
            ensurePath(uri);
            return extend(METHOD_KEY, "GET").extend(PATH_KEY, uri);
        }

        @Override
        public HttpRequestBuilder get() {
            return extend(METHOD_KEY, "GET");
        }

        @Override
        public HttpRequestBuilder post(final String uri) {
            ensurePath(uri);
            return extend(METHOD_KEY, "POST").extendHeaders(PATH_KEY, uri);
        }

        @Override
        public HttpRequestBuilder post() {
            return extend(METHOD_KEY, "POST");
        }

        @Override
        public HttpRequestDef build(final HttpProtocol protocol) {
            final var requestName = (String) values.get(REQUEST_NAME_KEY);
            final var baseUrl = (Optional<URL>) values.get(BASE_URL_KEY);
            final var headers = (Map<String, String>) values.get(HEADERS_KEY);
            final var method = (String) values.get(METHOD_KEY);

            return new DefaultHttpRequestDef(requestName, method, baseUrl, headers);
        }

        /**
         * Make sure that the given path is correct. Since this will also be dependent on the
         * {@link #baseUrl(String)}, we will use that base URL (if present) to construct a FQDN
         * but if not present, then we will build up a fake correct FQDN just to validate the
         * path itself.
         *
         * @param uri
         * @throws IllegalArgumentException
         */
        private void ensurePath(final String uri) throws IllegalArgumentException {
            // TODO
        }

        @Override
        public ActionBuilder toActionBuilder() {
            return new HttpRequestActionBuilder(this);
        }
    }
}
