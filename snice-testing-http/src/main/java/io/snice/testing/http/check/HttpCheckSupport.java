package io.snice.testing.http.check;

import io.snice.codecs.codec.http.HttpResponse;
import io.snice.functional.Either;
import io.snice.testing.core.check.Extractor;

import java.util.Optional;

public class HttpCheckSupport {

    public static HttpCheckBuilder<HttpResponse, Integer> status() {
        return HttpCheckBuilder.of(HttpResponse.class, new StatusExtractor());
    }

    private static class StatusExtractor implements Extractor<HttpResponse, Integer> {
        @Override
        public Either<Throwable, Optional<Integer>> apply(final HttpResponse response) {
            return Either.right(Optional.of(response.statusCode()));
        }
    }

}
