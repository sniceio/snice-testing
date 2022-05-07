package io.snice.testing.http;

import io.snice.codecs.codec.http.HttpMethod;
import io.snice.codecs.codec.http.HttpRequest;
import io.snice.testing.core.check.Check;
import io.snice.testing.core.common.Expression;
import io.snice.testing.http.impl.AcceptHttpRequestBuilderImpl;
import io.snice.testing.http.stack.HttpStackUserConfig;

import java.util.List;
import java.util.Map;

/**
 * The {@link AcceptHttpRequestDef} describes what to do for an incoming request, what checks to perform on that
 * incoming request and the response to generate.
 * <p>
 * Overall, it is very similar to the {@link InitiateHttpRequestDef} but instead of generating a request, it accepts one.
 */
public record AcceptHttpRequestDef(String requestName,
                                   HttpMethod method,
                                   Expression path,
                                   int statusCode,
                                   String reasonPhrase,
                                   Map<String, Expression> headers,
                                   List<Check<HttpRequest>> checks,
                                   String saveAs,
                                   HttpStackUserConfig config) {

    public static AcceptHttpRequestBuilder of(final String requestName, final HttpMethod method, final String uri, final String saveAs) {
        final var builder = AcceptHttpRequestBuilderImpl.of(requestName, method, uri);
        return builder.saveAs(saveAs);
    }


}
