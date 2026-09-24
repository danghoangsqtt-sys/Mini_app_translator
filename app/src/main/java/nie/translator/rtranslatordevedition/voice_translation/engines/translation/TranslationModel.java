package nie.translator.rtranslatordevedition.voice_translation.engines.translation;

import java.util.Objects;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineError;

/** Immutable public snapshot for one ML Kit translation model. */
public final class TranslationModel {
    private final String languageTag;
    private final TranslationModelState state;
    private final EngineError error;

    public TranslationModel(String languageTag, TranslationModelState state, EngineError error) {
        this.languageTag = Objects.requireNonNull(languageTag, "languageTag");
        this.state = Objects.requireNonNull(state, "state");
        this.error = error;
    }

    public String getLanguageTag() {
        return languageTag;
    }

    public TranslationModelState getState() {
        return state;
    }

    public EngineError getError() {
        return error;
    }
}
