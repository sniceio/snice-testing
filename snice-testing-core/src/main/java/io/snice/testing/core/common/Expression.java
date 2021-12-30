package io.snice.testing.core.common;

import io.snice.testing.core.Session;

import static io.snice.preconditions.PreConditions.assertNotEmpty;
import static io.snice.preconditions.PreConditions.assertNotNull;

public sealed interface Expression permits Expression.DynamicExpression, Expression.StaticExpression {

    static Expression of(final String expression) {
        assertNotEmpty(expression);

        // TODO: don't assume all is static.
        return new StaticExpression(expression);
    }

    default boolean isStatic() {
        return false;
    }

    default boolean isDynamic() {
        return false;
    }

    String apply(Session s);

    final record DynamicExpression(String value) implements Expression {

        @Override
        public String apply(final Session s) {
            assertNotNull(s);
            throw new RuntimeException("Not yet implemented");
        }

        @Override
        public boolean isDynamic() {
            return true;
        }
    }

    final record StaticExpression(String value) implements Expression {
        @Override
        public String apply(final Session s) {
            return value;
        }

        @Override
        public boolean isStatic() {
            return true;
        }
    }
}
