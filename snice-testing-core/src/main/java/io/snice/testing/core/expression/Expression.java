package io.snice.testing.core.expression;

import io.snice.preconditions.PreConditions;
import io.snice.testing.core.Session;

import static io.snice.preconditions.PreConditions.assertNotEmpty;
import static io.snice.preconditions.PreConditions.assertNotNull;

public sealed interface Expression permits Expression.DynamicExpression, Expression.StaticExpression {

    static Expression of(final String expression) {
        assertNotEmpty(expression);

        // TODO: assume all is static.
        return new StaticExpression(expression);
    }

    String apply(Session s);

    final record DynamicExpression(String value) implements Expression {

        @Override
        public String apply(final Session s) {
            assertNotNull(s);
            throw new RuntimeException("Not yet implemented");
        }
    }

    final record StaticExpression(String value) implements Expression {
        @Override
        public String apply(final Session s) {
            return value;
        }
    }
}
