package io.snice.testing.http.check;

import io.snice.codecs.codec.http.HttpHeader;
import io.snice.codecs.codec.http.HttpRequest;
import io.snice.codecs.codec.http.HttpResponse;
import io.snice.functional.Either;
import io.snice.testing.core.check.Extractor;

import java.util.Optional;

import static io.snice.preconditions.PreConditions.assertNotEmpty;

public class HttpCheckSupport {

    public static HttpCheckBuilder<HttpResponse, Integer> status() {
        return HttpCheckBuilder.of(HttpResponse.class, new StatusExtractor());
    }

    public static HttpCheckBuilder<HttpRequest, String> header(final String headerName) {
        assertNotEmpty(headerName, "The name of the header cannot be null or the empty string");
        return HttpCheckBuilder.of(HttpRequest.class, new HeaderExtractor(headerName));
    }

    private static record HeaderExtractor(String headerName) implements Extractor<HttpRequest, String> {
        @Override
        public Either<Throwable, Optional<String>> apply(final HttpRequest msg) {
            return Either.right(msg.header(headerName).map(HttpHeader::value).map(Object::toString));
        }
    }

    private static class StatusExtractor implements Extractor<HttpResponse, Integer> {
        @Override
        public Either<Throwable, Optional<Integer>> apply(final HttpResponse response) {
            return Either.right(Optional.of(response.statusCode()));
        }
    }

}
