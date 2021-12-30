package io.snice.testing.core.check;

import java.util.Optional;

public record CheckResult<T>(Optional<T> extractedValue, Optional<String> saveAs) {
}
