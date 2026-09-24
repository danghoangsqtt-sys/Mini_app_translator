package nie.translator.rtranslatordevedition.voice_translation.engines;

import java.util.List;
import nie.translator.rtranslatordevedition.tools.CustomLocale;

/** Runtime-neutral text translation contract. */
public interface TextTranslationEngine {
    interface TranslationCallback {
        /** The returned locale is the requested output language; detection has a separate API. */
        void onTranslated(String text, CustomLocale outputLanguage);
        void onFailure(EngineError error);
    }

    interface LanguageDetectionCallback {
        void onDetected(CustomLocale language);
        void onFailure(EngineError error);
    }

    interface SupportedLanguagesCallback {
        void onLanguages(List<CustomLocale> languages);
        void onFailure(EngineError error);
    }

    EngineCapability getCapability();
    EngineOperation translate(String text, CustomLocale outputLanguage, TranslationCallback callback);
    EngineOperation detectLanguage(String text, LanguageDetectionCallback callback);
    EngineOperation getSupportedLanguages(CustomLocale displayLanguage, SupportedLanguagesCallback callback);
    void close();
}
