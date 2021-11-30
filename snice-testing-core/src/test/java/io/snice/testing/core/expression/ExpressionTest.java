package io.snice.testing.core.expression;

import io.snice.testing.core.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(MockitoExtension.class)
class ExpressionTest {

    @Test
    public void testCreateStaticExpression(@Mock final Session session) {
        final var expression = Expression.of("hello");
        assertThat(expression.apply(session), is("hello"));
    }

}