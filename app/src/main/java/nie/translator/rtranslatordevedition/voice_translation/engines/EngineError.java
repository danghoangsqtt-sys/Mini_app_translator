package nie.translator.rtranslatordevedition.voice_translation.engines;

import java.util.Arrays;
import java.util.Objects;

/** Immutable normalized error with an optional, defensively copied legacy diagnostic. */
public final class EngineError {
    public enum Category {
        CANCELLED,
        NETWORK,
        AUTHENTICATION,
        QUOTA,
        UNSUPPORTED_LANGUAGE,
        MISSING_DEPENDENCY,
        PERMISSION,
        BUSY,
        UNSUPPORTED,
        CLOSED,
        INTERNAL_FAILURE
    }

    private final Category category;
    private final int[] legacyReasons;
    private final long legacyValue;
    private final String detail;

    public EngineError(Category category, int[] legacyReasons, long legacyValue, String detail) {
        this.category = Objects.requireNonNull(category, "category");
        this.legacyReasons = legacyReasons == null ? new int[0] : Arrays.copyOf(legacyReasons, legacyReasons.length);
        this.legacyValue = legacyValue;
        this.detail = Objects.requireNonNull(detail, "detail");
    }

    public Category getCategory() {
        return category;
    }

    public int[] getLegacyReasons() {
        return Arrays.copyOf(legacyReasons, legacyReasons.length);
    }

    public long getLegacyValue() {
        return legacyValue;
    }

    public String getDetail() {
        return detail;
    }
}
