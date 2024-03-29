package io.snice.testing.http.impl;

import io.snice.buffer.Buffer;
import io.snice.codecs.codec.http.HttpMethod;
import io.snice.codecs.codec.http.HttpRequest;
import io.snice.testing.core.action.ActionBuilder;
import io.snice.testing.core.check.Check;
import io.snice.testing.core.common.Expression;
import io.snice.testing.http.AcceptHttpRequestBuilder;
import io.snice.testing.http.AcceptHttpRequestDef;
import io.snice.testing.http.Content;
import io.snice.testing.http.HttpMessageDefBuilder;
import io.snice.testing.http.action.AcceptHttpRequestActionBuilder;
import io.snice.testing.http.stack.HttpStackUserConfig;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.snice.preconditions.PreConditions.assertArgument;
import static io.snice.preconditions.PreConditions.assertNotEmpty;
import static io.snice.preconditions.PreConditions.assertNotNull;
import static java.util.Optional.ofNullable;

public final class AcceptHttpRequestBuilderImpl extends HttpMessageDefBuilderBase<AcceptHttpRequestDef, HttpRequest>
        implements AcceptHttpRequestBuilder {

    private static final String SAVE_AS_KEY = "SAVE_AS";
    private static final String STATUS_CODE_KEY = "STATUS_CODE";
    private static final String REASON_PHRASE_KEY = "REASON_PHRASE";

    public static AcceptHttpRequestBuilderImpl of(final String requestName,
                                                  final HttpMethod method,
                                                  final String uri) {
        assertNotEmpty(requestName);
        assertNotNull(method);
        return new AcceptHttpRequestBuilderImpl(requestName, method, uri);
    }

    private AcceptHttpRequestBuilderImpl(final String requestName, final HttpMethod method, final String path) {
        super(requestName, method, path);
    }

    private AcceptHttpRequestBuilderImpl(final AcceptHttpRequestBuilderImpl parent,
                                         final String requestName,
                                         final HttpMethod method, final Expression path) {
        super(parent, requestName, method, path);
    }

    private AcceptHttpRequestBuilderImpl(final Map<String, Object> values) {
        super(values);
    }

    public AcceptHttpRequestBuilder saveAs(final String saveAs) {
        assertNotEmpty(saveAs);
        return extend(SAVE_AS_KEY, saveAs);
    }

    @Override
    protected AcceptHttpRequestDef internalBuild(final Map<String, Object> values,
                                                 final Optional<HttpMessageDefBuilderBase<AcceptHttpRequestDef, HttpRequest>> parent,
                                                 final Optional<AcceptHttpRequestDef> child,
                                                 final String name,
                                                 final HttpMethod method,
                                                 final Expression target,
                                                 final Map<String, Expression> headers,
                                                 final Optional<Content<?>> content,
                                                 final List<Check<HttpRequest>> checks) {
        final var saveAs = (String) values.get(SAVE_AS_KEY);
        assertNotEmpty(saveAs, "The \"Save As\" key is empty, which should be impossible. This is an internal bug");

        // TODO: the default reason phrase should be dependent on the status code.
        final var statusCode = ofNullable((Integer) values.get(STATUS_CODE_KEY)).orElse(200);
        final var reasonPhrase = ofNullable((String) values.get(REASON_PHRASE_KEY)).orElse("OK");

        // TODO
        final var config = new HttpStackUserConfig();

        final var def = new AcceptHttpRequestDef(name, method, target, statusCode, reasonPhrase, headers, checks, content, saveAs, child, config);
        return parent.map(p -> p.build(def)).orElse(def);
    }

    @Override
    public ActionBuilder toActionBuilder() {
        return new AcceptHttpRequestActionBuilder(this);
    }


    @Override
    protected <T extends HttpMessageDefBuilder> T newBuilder(final Map<String, Object> newValues) {
        return (T) new AcceptHttpRequestBuilderImpl(newValues);
    }

    @Override
    public AcceptHttpRequestBuilder header(final String name, final String value) {
        return extendHeaders(name, value);
    }

    @Override
    public AcceptHttpRequestBuilder respond(final int statusCode) {
        return extend(STATUS_CODE_KEY, statusCode);
    }

    @Override
    public AcceptHttpRequestBuilder respond(final int statusCode, final String reasonPhrase) {
        extend(STATUS_CODE_KEY, statusCode);
        return extend(REASON_PHRASE_KEY, reasonPhrase);
    }

    @Override
    public AcceptHttpRequestBuilder content(final Map<String, Object> content) {
        assertNotNull(content);
        assertArgument(!content.isEmpty());
        return extend(CONTENT, Content.of(content));
    }

    @Override
    public AcceptHttpRequestBuilder content(final Buffer content) {
        return extend(CONTENT, Content.of(content));
    }

    @Override
    public AcceptHttpRequestBuilder acceptNextRequest(final String name) {
        final var saveAs = (String) get(SAVE_AS_KEY);
        final var next = new AcceptHttpRequestBuilderImpl(this, name, method(), target()).saveAs(saveAs);
        return next;
    }

    @Override
    public AcceptHttpRequestBuilder check(final Check<HttpRequest> check) {
        assertNotNull(check);
        return extendChecks(check);
    }
}
