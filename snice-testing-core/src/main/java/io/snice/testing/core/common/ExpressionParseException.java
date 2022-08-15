package io.snice.testing.core.common;

public class ExpressionParseException extends RuntimeException {

    private final int errorOffset;

    public ExpressionParseException(final String s, final int errorOffset) {
        super(s);
        this.errorOffset = errorOffset;
    }

    public int getErrorOffset() {
        return errorOffset;
    }

}
