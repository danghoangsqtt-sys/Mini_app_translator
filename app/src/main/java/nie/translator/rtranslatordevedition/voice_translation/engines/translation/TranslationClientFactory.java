package nie.translator.rtranslatordevedition.voice_translation.engines.translation;

/** Testable factory boundary for per-request ML Kit language identification and translation clients. */
public interface TranslationClientFactory {
    interface LanguageIdentifierClient {
        void identifyLanguage(String text, AsyncResultCallback<String> callback);
        void close();
    }

    interface TranslatorClient {
        void translate(String text, AsyncResultCallback<String> callback);
        void close();
    }

    LanguageIdentifierClient createLanguageIdentifier();
    TranslatorClient createTranslator(String sourceLanguageTag, String targetLanguageTag);
}
