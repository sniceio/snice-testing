package io.snice.testing.http.impl;

import io.snice.testing.core.common.Expression;
import io.snice.testing.http.Content;

import java.util.Map;

import static io.snice.preconditions.PreConditions.assertArgument;
import static io.snice.preconditions.PreConditions.assertNotNull;

public class ContentFactory {

    public static Content<Map<String, Expression>> of(final Map<String, Expression> formEncodedParams) {
        assertNotNull(formEncodedParams);
        assertArgument(!formEncodedParams.isEmpty(), "You must specify at least one form-encoded param");
        return new FormEncodedContent(formEncodedParams);
    }

    private record FormEncodedContent(Map<String, Expression> content) implements Content<Map<String, Expression>> {

    }

}
