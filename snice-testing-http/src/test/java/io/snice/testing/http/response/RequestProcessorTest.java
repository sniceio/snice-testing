package io.snice.testing.http.response;

import io.snice.codecs.codec.http.HttpResponse;
import io.snice.testing.core.Execution;
import io.snice.testing.core.Session;
import io.snice.testing.core.action.Action;
import io.snice.testing.http.AcceptHttpRequestBuilder;
import io.snice.testing.http.AcceptHttpRequestDef;
import io.snice.testing.http.TestBase;
import io.snice.testing.http.protocol.HttpServerTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static io.snice.testing.http.check.HttpCheckSupport.header;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestProcessorTest extends TestBase {

    @Captor
    private ArgumentCaptor<List<Execution>> executionsCaptor;

    @Captor
    private ArgumentCaptor<Session> sessionCaptor;

    @Mock
    private Action next;

    @Mock
    private HttpServerTransaction transaction;

    private Session session;

    private AcceptHttpRequestBuilder defBuilder;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        session = new Session("Request Processor Test");
        defBuilder = someAcceptHttpRequestDef();
    }

    private RequestProcessor someRequestProcessor(final AcceptHttpRequestDef def) {
        return new RequestProcessor(def.requestName(), def, session, List.of(), next);
    }

    /**
     * Ensure that if a check fails that we also fail the {@link Execution} and the {@link Session}
     * should reflect this too
     * <p>
     * TODO: this test is wrong now. Need to update it...
     */
    // @Test
    public void testRequestProcessorFailedChecks() {
        final var def = defBuilder
                .check(header("Hello").is("Wrong")) // will fail
                .check(header("Foo").is("Boo")) // will succeed but combined, it is still a fail
                .build();

        when(transaction.createResponse(200)).thenReturn(HttpResponse.create(200));
        final var processor = someRequestProcessor(def);
        final var incomingHttpRequest = someHttpRequest("Foo", "Boo", "Hello", "World").build();
        processor.onRequest(transaction, incomingHttpRequest);

        verify(next).execute(executionsCaptor.capture(), sessionCaptor.capture());

        final var execution = executionsCaptor.getValue().get(0);
        assertThat(execution.success(), is(false));
        assertThat(sessionCaptor.getValue().isFailed(), is(true));
    }

    /**
     * Ensure that we will create and send an HTTP response back.
     */
    // @Test
    public void testRequestProcessorSendResponse() {
        // TODO: not really a unit test just yet. We don't actually test and verify anything
        final var def = defBuilder.respond(418, "Teapot")
                .header("UnitTest", "Hello")
                .build();

        final var processor = someRequestProcessor(def);
        processor.onRequest(transaction, someHttpRequest().build());

    }

}