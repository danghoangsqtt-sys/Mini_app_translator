package nie.translator.rtranslatordevedition.voice_translation.engines;

/** Produces engines for one explicit family. Factories do not choose runtime defaults. */
public interface EngineFactory {
    EngineType getType();
    TextTranslationEngine createTextTranslationEngine();
    SpeechRecognitionEngine createSpeechRecognitionEngine();
    SpeechOutputEngine createSpeechOutputEngine();
}
