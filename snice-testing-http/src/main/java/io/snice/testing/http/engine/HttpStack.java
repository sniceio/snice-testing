package io.snice.testing.http.engine;

import static io.snice.preconditions.PreConditions.assertNotNull;

public interface HttpStack {

    static Builder of(final HttpStackConfig config) {
        assertNotNull(config);
        return new Builder(config);
    }

    class Builder {

        private final HttpStackConfig config;

        private Builder(final HttpStackConfig config) {
            this.config = config;
        }

        public HttpStack build() {
            return null;
        }

    }
}
