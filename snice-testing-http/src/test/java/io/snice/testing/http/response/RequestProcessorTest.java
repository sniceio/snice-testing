package io.snice.testing.http.response;

import io.snice.testing.core.Execution;
import io.snice.testing.core.Session;
import io.snice.testing.core.action.Action;
import io.snice.testing.http.TestBase;
import io.snice.testing.http.protocol.HttpServerTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        session = new Session("Request Processor Test");
    }


    /**
     * Ensure that if a check fails that we also fail the {@link Execution} and the {@link Session}
     * should reflect this too
     */
    @Test
    public void testRequestProcessorFailedChecks() {
        final var def = someAcceptHttpRequestDef()
                .check(header("Hello").is("Wrong")) // will fail
                .check(header("Foo").is("Boo")) // will succeed but combined, it is still a fail
                .build();

        final var processor = new RequestProcessor(def.requestName(), def.checks(), session, List.of(), next);
        final var incomingHttpRequest = someHttpRequest("Foo", "Boo", "Hello", "World").build();
        processor.onRequest(transaction, incomingHttpRequest);

        verify(next).execute(executionsCaptor.capture(), sessionCaptor.capture());

        final var execution = executionsCaptor.getValue().get(0);
        assertThat(execution.success(), is(false));
        assertThat(sessionCaptor.getValue().isFailed(), is(true));
    }

}