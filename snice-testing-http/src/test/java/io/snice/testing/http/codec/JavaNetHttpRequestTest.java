package io.snice.testing.http.codec;

import io.snice.codecs.codec.http.HttpProvider;
import io.snice.codecs.codec.http.HttpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class JavaNetHttpRequestTest {

    @BeforeEach
    public void setup() {
        HttpProvider.setMessageFactory(new JavaNetHttpMessageFactory());
    }

    @Test
    public void testCreateRequest() {
        final var req = HttpRequest.get("http://example.com")
                .header("Hello", "World")
                .header("Hello", "World Again")
                .header("Foo", "Woo")
                .build();

        assertThat(req.headers().size(), is(3));
        assertThat(req.headers("Hello").size(), is(2));
        assertThat(req.headers("Hello").get(0).value(), is("World"));
        assertThat(req.headers("Hello").get(1).value(), is("World Again"));

        assertThat(req.header("Hello").get().value(), is("World"));

        assertThat(req.header("DoesntExist"), is(Optional.empty()));
        assertThat(req.headers("DoesntExist").isEmpty(), is(true));
    }
}
