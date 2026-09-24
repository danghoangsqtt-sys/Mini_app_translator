package nie.translator.rtranslatordevedition.voice_translation.engines;

/** Stable availability states reported by an engine without exposing platform APIs. */
public enum CapabilityState {
    AVAILABLE,
    TEMPORARILY_UNAVAILABLE,
    SETUP_REQUIRED,
    UNSUPPORTED,
    CLOSED
}
