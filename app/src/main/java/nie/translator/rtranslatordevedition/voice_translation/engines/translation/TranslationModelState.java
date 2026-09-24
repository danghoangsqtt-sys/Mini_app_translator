package nie.translator.rtranslatordevedition.voice_translation.engines.translation;

/** Honest model-management states. Download progress is intentionally indeterminate. */
public enum TranslationModelState {
    BUILT_IN,
    NOT_DOWNLOADED,
    QUEUED,
    DOWNLOADING,
    DOWNLOADED,
    DELETING,
    FAILED
}
