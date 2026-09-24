package nie.translator.rtranslatordevedition.voice_translation.engines;

import java.util.Objects;

/** Immutable engine availability information. Details must never contain text or credentials. */
public final class EngineCapability {
    private final CapabilityState state;
    private final String detail;

    public EngineCapability(CapabilityState state, String detail) {
        this.state = Objects.requireNonNull(state, "state");
        this.detail = Objects.requireNonNull(detail, "detail");
    }

    public CapabilityState getState() {
        return state;
    }

    public String getDetail() {
        return detail;
    }
}
