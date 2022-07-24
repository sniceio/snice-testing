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
        final var session = new Session("Unit Test");
        final var expression = Expression.of("hello");
        assertThat(expression.apply(session), is("hello"));
    }

}