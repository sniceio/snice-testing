package io.snice.testing.http.action;

import io.snice.codecs.codec.http.HttpMessage;
import io.snice.codecs.codec.http.HttpMessageFactory;
import io.snice.codecs.codec.http.HttpMethod;
import io.snice.codecs.codec.http.HttpProvider;
import io.snice.codecs.codec.http.HttpRequest;
import io.snice.testing.core.Execution;
import io.snice.testing.core.Session;
import io.snice.testing.core.action.Action;
import io.snice.testing.http.InitiateHttpRequestDef;
import io.snice.testing.http.TestBase;
import io.snice.testing.http.protocol.HttpTransaction;
import io.snice.testing.http.response.ResponseProcessor;
import io.snice.testing.http.stack.HttpStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InitiateHttpRequestActionTest extends TestBase {

    @Captor
    private ArgumentCaptor<Session> sessionCaptor;

    @Captor
    private ArgumentCaptor<List<Execution>> executionsCaptor;

    @Mock
    private HttpMessageFactory messageFactory;

    @Mock
    private HttpMessage.Builder builder;

    @Mock
    private HttpStack stack;

    @Mock
    private HttpTransaction.Builder transactionBuilder;

    @Mock
    private HttpRequest httpRequest;

    @BeforeEach
    public void setup() {
        HttpProvider.setMessageFactory(messageFactory);
    }

    /**
     * If the user specifies a {@link InitiateHttpRequestDef} where the target URL is
     * unresolvable, we need to error out with a failed session. This tests that.
     */
    @Test
    public void testRequestActionButNoTargetUri(@Mock final Action next) throws Exception {
        final var action = new InitiateHttpRequestAction("Test", someHttpProtocol(), someHttpRequestDef(), next);
        final List<Execution> executions = List.of();
        action.execute(executions, new Session("Testing"));

        verify(next).execute(executionsCaptor.capture(), sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().isFailed(), is(true));
    }

    /**
     * Validates the regular flow where we are able to correctly create a new http request, create a new
     * http transaction and then kick off that transaction. At this point, there should be no interactions
     * with the next {@link Action} since that won't happen until we get back a response, which
     * will be processed by the {@link ResponseProcessor}, which we'll test separately.
     */
    @Test
    public void testSendHttpRequest(@Mock final Action next) throws Exception {
        // Note that if there is a bug and we don't end up with a GET and a URI as given below,
        // it won't match and we'll blow up on a NPE.
        when(messageFactory.createRequest(HttpMethod.GET, new URI("http://example.com"))).thenReturn(builder);
        when(builder.build()).thenReturn(httpRequest);
        when(stack.newTransaction(httpRequest)).thenReturn(transactionBuilder);

        final var def = someHttpRequestDef("http://example.com", "hello", "world");
        final var action = new InitiateHttpRequestAction("Test", someHttpProtocol(stack), def, next);
        final List<Execution> executions = List.of();
        action.execute(executions, new Session("Testing"));

        // check so that we build the new request properly
        verify(builder).header("hello", "world");
        verify(builder).build();
        verifyNoMoreInteractions(builder);

        verify(transactionBuilder).onResponse(any());
        verify(transactionBuilder).start();

        // There should be zero interactions with the next Action since that won't
        // happen once the HTTP transaction has completed so make sure that is indeed
        // true!!!
        verifyNoInteractions(next);
    }


}