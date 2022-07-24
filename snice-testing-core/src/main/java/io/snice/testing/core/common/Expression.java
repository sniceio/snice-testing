package io.snice.testing.core.common;

import io.snice.testing.core.Session;

import static io.snice.preconditions.PreConditions.assertNotEmpty;
import static io.snice.preconditions.PreConditions.assertNotNull;

public sealed interface Expression permits Expression.DynamicExpression, Expression.StaticExpression {

    static Expression of(final String expression) {
        assertNotEmpty(expression);

        // for now, super simple:
        if (expression.startsWith("${") && expression.endsWith("}")) {
            return new DynamicExpression(expression);
        }
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

        public DynamicExpression {
            value = value.substring(2, value.length() - 1);
        }

        @Override
        public String apply(final Session s) {
            assertNotNull(s);
            return s.attributes(value).map(Object::toString).orElseThrow(() -> new IllegalStateException("Unable to resolve key \"" +
                    value + "\" as it is not part of the Session"));
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
