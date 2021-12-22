package io.snice.testing.http;

import java.net.URL;
import java.util.Map;
import java.util.Optional;

public sealed interface HttpRequestDef permits HttpDsl.DefaultHttpRequestDef {

    String requestName();

    Map<String, String> headers();

    Optional<URL> baseUrl();

    String method();

}
