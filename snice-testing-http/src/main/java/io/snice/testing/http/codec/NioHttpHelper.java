package io.snice.testing.http.codec;

import io.snice.codecs.codec.http.HttpHeader;

import java.net.http.HttpHeaders;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class NioHttpHelper {

    public static <T> Optional<HttpHeader<T>> header(final HttpHeaders headers, final String name) {
        return headers.firstValue(name).map(v -> new NioHttpHeader(name, v));
    }

    public static <T> List<HttpHeader<T>> headers(final HttpHeaders headers, final String name) {
        return headers.allValues(name).stream().map(v -> new NioHttpHeader<T>(name, (T) v)).collect(Collectors.toUnmodifiableList());
    }

    public static List<HttpHeader<?>> headers(final HttpHeaders headers) {
        final var headersMap = headers.map();
        final var newHeaders = new ArrayList<HttpHeader<?>>();
        headersMap.entrySet().forEach(e -> {
            e.getValue().forEach(v -> newHeaders.add(new NioHttpHeader(e.getKey(), v)));
        });
        return newHeaders;
    }

    private static record NioHttpHeader<T>(String name, T value) implements HttpHeader<T> {

    }
}
