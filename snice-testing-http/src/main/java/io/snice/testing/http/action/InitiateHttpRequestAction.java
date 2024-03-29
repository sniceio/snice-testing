package io.snice.testing.http.action;

import io.snice.codecs.codec.http.HttpMessage;
import io.snice.codecs.codec.http.HttpRequest;
import io.snice.identity.sri.ActionResourceIdentifier;
import io.snice.testing.core.Execution;
import io.snice.testing.core.Session;
import io.snice.testing.core.action.Action;
import io.snice.testing.core.common.ListOperations;
import io.snice.testing.core.protocol.ProtocolRegistry;
import io.snice.testing.http.Content;
import io.snice.testing.http.InitiateHttpRequestDef;
import io.snice.testing.http.protocol.HttpProtocol;
import io.snice.testing.http.response.ResponseProcessor;
import io.snice.testing.http.stack.HttpStack;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

public record InitiateHttpRequestAction(String name,
                                        ActionResourceIdentifier sri,
                                        HttpProtocol http,
                                        HttpStack stack,
                                        InitiateHttpRequestDef httpDef,
                                        Action next) implements Action {

    @Override
    public Optional<ProtocolRegistry.Key> protocol() {
        return Optional.of(HttpProtocol.httpProtocolKey);
    }

    @Override
    public void execute(final List<Execution> executions, final Session session) {
        try {
            final var uriMaybe = httpDef.resolveTarget(http, session);
            final var newSession = uriMaybe.fold(t -> processResolveUriError(session, t), uri -> session);
            if (newSession.isSucceeded()) {
                final var request = map(session, httpDef, uriMaybe.get());
                final var transaction = stack.newTransaction(request);
                final var processor = new ResponseProcessor(name, request, httpDef.checks(), session, executions, next);
                transaction.onResponse(processor::process);
                transaction.start();
            } else {
                // TODO: perhaps say something more about the failure here? After all, it failed because
                // we were unable to resolve the URI. Or should we perhaps just allow the exception to bubble
                // out to the Snice execution framework that will wrap the exception in a consistent manner instead?
                // Otherwise all actions will have to do the same thing.
                final var failedExecution = new Execution(name, false);
                next.execute(ListOperations.extendList(executions, failedExecution), newSession);
            }
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static HttpRequest map(final Session session, final InitiateHttpRequestDef def, final URL target) throws URISyntaxException {
        final var builder = HttpRequest.create(def.method(), target.toURI());
        def.auth().ifPresent(auth -> auth.apply(session, builder));
        def.headers().entrySet().forEach(entry -> builder.header(entry.getKey(), entry.getValue().apply(session)));
        def.content().ifPresent(content -> processContent(session, content, builder));
        return builder.build();
    }

    private static void processContent(final Session session, final Content content, final HttpMessage.Builder<HttpRequest> builder) {
        // TODO: right now we assume that the Content is only a map
        content.apply(session, builder);
        //final Map<String, Object> params = (Map<String, Object>) content.content();
        //final Map<String, String> processed = new HashMap<>();
        //params.entrySet().stream().forEach(e -> processed.put(e.getKey(), e.getValue().apply(session)));
        //builder.content(processed);
    }

    private Session processResolveUriError(final Session session, final String errorMsg) {
        return session.markAsFailed();
    }

}
