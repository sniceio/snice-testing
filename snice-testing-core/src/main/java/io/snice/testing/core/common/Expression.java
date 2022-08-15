package io.snice.testing.core.common;

import io.snice.testing.core.Session;
import io.snice.testing.core.scenario.ScenarioException.NoSuchAttributeException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static io.snice.preconditions.PreConditions.assertNotEmpty;
import static io.snice.preconditions.PreConditions.assertNotNull;

public sealed interface Expression permits Expression.CompoundExpression, Expression.DynamicExpression, Expression.EnvironmentExpression, Expression.StaticExpression {

    String DYNAMIC_EXPRESSION_START = "${";
    String DYNAMIC_EXPRESSION_STOP = "}";
    String ENVIRONMENT_EXPRESSION = "env.";
    String DYNAMIC_ENV_EXPRESSION_START = DYNAMIC_EXPRESSION_START + ENVIRONMENT_EXPRESSION;

    static Expression of(final String expression) {
        assertNotEmpty(expression);

        // for now, super simple:
        if (expression.indexOf(DYNAMIC_EXPRESSION_START) != -1) {
            return new CompoundExpression(expression);
        }

        if (expression.startsWith(DYNAMIC_EXPRESSION_START) && expression.endsWith(DYNAMIC_EXPRESSION_STOP)) {
            if (expression.toLowerCase().startsWith(DYNAMIC_ENV_EXPRESSION_START)) {
                return new EnvironmentExpression(expression);
            }
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

    final record CompoundExpression(String value, List<Expression> expressions) implements Expression {

        public CompoundExpression(final String value) {
            this(value, parseValue(0, value));
        }

        private static List<Expression> parseValue(final int readIndex, final String value) {
            if (readIndex == value.length()) {
                return new ArrayList<>();
            }

            final var index = value.indexOf(DYNAMIC_EXPRESSION_START, readIndex);
            if (index == -1) {
                final var l = new ArrayList<Expression>();
                l.add(new StaticExpression(value.substring(readIndex)));
                return l;
            }

            final var endIndex = value.indexOf(DYNAMIC_EXPRESSION_STOP, index);
            if (endIndex == -1) {
                throw new ExpressionParseException("Missing the end of the dynamic expression", value.length());
            }

            final var dynamicExpressionValue = value.substring(index, endIndex + 1);
            final Expression dynamicExpression;
            if (dynamicExpressionValue.toLowerCase().startsWith(DYNAMIC_ENV_EXPRESSION_START)) {
                dynamicExpression = new EnvironmentExpression(dynamicExpressionValue);
            } else {
                dynamicExpression = new DynamicExpression(dynamicExpressionValue);
            }


            final var theRest = parseValue(endIndex + 1, value);
            theRest.add(0, dynamicExpression);

            if (index > readIndex) {
                theRest.add(0, new StaticExpression(value.substring(readIndex, index)));
            }
            return theRest;
        }

        @Override
        public String apply(final Session s) {
            return expressions.stream().map(expression -> expression.apply(s)).collect(Collectors.joining());
        }
    }

    final record EnvironmentExpression(String value) implements Expression {

        public EnvironmentExpression {
            value = value.substring(DYNAMIC_ENV_EXPRESSION_START.length(), value.length() - DYNAMIC_EXPRESSION_STOP.length());
        }

        @Override
        public String apply(final Session s) {
            assertNotNull(s);
            return s.environment(value).map(Object::toString).orElseThrow(() -> new NoSuchAttributeException("Unable to resolve " +
                    "the environment variable \"" + value + "\""));
        }
    }

    final record DynamicExpression(String value) implements Expression {

        public DynamicExpression {
            value = value.substring(DYNAMIC_EXPRESSION_START.length(), value.length() - DYNAMIC_EXPRESSION_STOP.length());
        }

        @Override
        public String apply(final Session s) {
            assertNotNull(s);
            return s.attributes(value).map(Object::toString).orElseThrow(() -> new NoSuchAttributeException("Unable to resolve key \"" +
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
