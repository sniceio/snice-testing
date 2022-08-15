package io.snice.testing.core.expression;

import io.snice.testing.core.Session;
import io.snice.testing.core.common.Expression;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(MockitoExtension.class)
class ExpressionTest {

    @Test
    public void testCreateStaticExpression() {
        assertExpression("hello", "hello");
    }

    @Test
    public void testCreateDynamicExpression() {
        final var session = new Session("Unit Test").attributes("nisse", "kalle");
        assertExpression("${nisse}", "kalle", session);
    }

    @Test
    public void testCreateEnvExpression() {
        final var session = new Session("Unit Test").environment("nisse", "kalle");
        assertExpression("${env.nisse}", "kalle", session);
    }

    @Test
    public void testCreateDynamicExpressionDeepIntoString() {
        final var session = new Session("Unit Test").environment("nisse", "kalle");
        assertExpression("http://example.com/${env.nisse}", "http://example.com/kalle", session);
    }

    @Test
    public void testCreateMultipleDynamicExpression() {
        final var session = new Session("Unit Test").attributes("foo", "woo").environment("nisse", "kalle");
        assertExpression("http://example.com/${env.nisse}/static_again/${foo}/theend", "http://example.com/kalle/static_again/woo/theend", session);
    }

    @Test
    public void testCreateMultipleDynamicExpression2() {
        final var session = new Session("Unit Test")
                .attributes("schema", "https")
                .environment("host", "example.com")
                .environment("port", "2345")
                .environment("root", "api/v1");
        assertExpression("${schema}://${env.host}:${env.port}/${env.root}/users", "https://example.com:2345/api/v1/users", session);
    }

    private static void assertExpression(final String expression, final String expected) {
        assertExpression(expression, expected, new Session("Unit Test"));
    }

    private static void assertExpression(final String expression, final String expected, final Session session) {
        assertThat(Expression.of(expression).apply(session), is(expected));
    }

}