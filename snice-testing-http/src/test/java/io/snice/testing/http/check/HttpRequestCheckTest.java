package io.snice.testing.http.check;

import io.snice.codecs.codec.http.HttpRequest;
import io.snice.testing.core.Session;
import io.snice.testing.core.check.CheckResult;
import io.snice.testing.http.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static io.snice.testing.http.check.HttpCheckSupport.header;
import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class HttpRequestCheckTest extends TestBase {

    private Session session;
    private HttpRequest request;

    @BeforeEach
    public void setup() {
        session = new Session("Unit Test");
        request = someHttpRequest("Hello", "World", "Apa", "Nisse").build();
    }

    @Test
    public void testCheckHttpRequestHeaderOperandIsSuccess() {
        final var check = header("Hello").is("World").saveAs("hello");
        final var result = check.check(request, session);
        checkCheckResult(result, Optional.of("World"), true);
    }

    /**
     * The "is" check is case-sensitive for Strings.
     */
    @Test
    public void testCheckHttpRequestHeaderOperandIsFailure() {
        final var result = header("Hello").is("world").check(request, session);
        checkCheckResult(result, empty(), false);
    }

    @Test
    public void testSaveAs() {
        var result = header("Hello").is("world").saveAs("hello").check(request, session);
        assertThat(result.saveAs(), is(Optional.of("hello")));

        result = header("Hello").is("world").check(request, session);
        assertThat(result.saveAs(), is(empty()));
    }

    private static void checkCheckResult(final CheckResult<?, ?> result, final Optional<?> extractedValue, final boolean isSuccess) {
        assertThat(result.extractedValue(), is(extractedValue));
        assertThat(result.isSuccess(), is(isSuccess));
        assertThat(result.isFailure(), is(!isSuccess));
    }
}
