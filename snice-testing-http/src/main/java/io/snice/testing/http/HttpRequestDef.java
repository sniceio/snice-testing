package io.snice.testing.http;

import io.snice.testing.core.expression.Expression;

import java.net.URL;
import java.util.Map;
import java.util.Optional;

public sealed interface HttpRequestDef permits HttpDsl.DefaultHttpRequestDef {

    Expression requestName();

    Map<String, String> headers();

    Optional<URL> baseUrl();

    String method();

}
