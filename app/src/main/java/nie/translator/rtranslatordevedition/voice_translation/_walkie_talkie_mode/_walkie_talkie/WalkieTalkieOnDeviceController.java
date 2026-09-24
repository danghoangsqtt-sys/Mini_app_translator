package nie.translator.rtranslatordevedition.voice_translation._walkie_talkie_mode._walkie_talkie;

import nie.translator.rtranslatordevedition.tools.CustomLocale;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineError;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineOperation;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineServiceErrorMapper.OperationKind;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineTurnCoordinator;
import nie.translator.rtranslatordevedition.voice_translation.engines.SpeechRecognitionEngine;
import nie.translator.rtranslatordevedition.voice_translation.engines.TextTranslationEngine;

/** One-recognizer WalkieTalkie pipeline with an explicitly selected source language. */
public final class WalkieTalkieOnDeviceController {
    public interface Listener {
        void onTranslated(String text, CustomLocale language, boolean sourceIsFirst);
        void onError(EngineError error, OperationKind kind);
        void onTurnEnded();
    }
    private final SpeechRecognitionEngine speech; private final TextTranslationEngine translation;
    private final EngineTurnCoordinator turns; private final Listener listener;
    private CustomLocale first; private CustomLocale second; private boolean sourceIsFirst = true;
    private long activeGeneration = -1L; private boolean recognitionTerminal; private boolean turnTerminal;
    public WalkieTalkieOnDeviceController(SpeechRecognitionEngine speech, TextTranslationEngine translation, EngineTurnCoordinator turns, Listener listener) {
        this.speech = speech; this.translation = translation; this.turns = turns; this.listener = listener;
    }
    public void setLanguages(CustomLocale first, CustomLocale second) {
        if (sameLocale(this.first, first) && sameLocale(this.second, second)) { return; }
        this.first = first; this.second = second; turns.cancelCurrent();
    }
    public void setSourceIsFirst(boolean value) { if (sourceIsFirst != value) { sourceIsFirst = value; turns.cancelCurrent(); } }
    public boolean isSourceFirst() { return sourceIsFirst; }
    public void startTurn() {
        final long generation = turns.replace();
        if (generation < 0) { return; }
        activeGeneration = generation; recognitionTerminal = false; turnTerminal = false;
        final CustomLocale source = sourceIsFirst ? first : second; final CustomLocale target = sourceIsFirst ? second : first;
        if (source == null || target == null) {
            listener.onError(new EngineError(EngineError.Category.UNSUPPORTED_LANGUAGE, new int[0], 0L, "Select both languages"), OperationKind.SPEECH_RECOGNITION);
            listener.onTurnEnded(); return;
        }
        final boolean direction = sourceIsFirst;
        EngineOperation operation = speech.start(new SpeechRecognitionEngine.RecognitionRequest(source, 0, SpeechRecognitionEngine.AudioInputMode.ENGINE_CAPTURE), new SpeechRecognitionEngine.RecognitionCallback() {
            @Override public void onResult(String text, CustomLocale ignored, float confidence, boolean isFinal) {
                if (!isFinal || !markRecognitionTerminal(generation)) { return; }
                translate(generation, text, target, direction);
            }
            @Override public void onFailure(EngineError error) {
                if (markRecognitionTerminal(generation)) { listener.onError(error, OperationKind.SPEECH_RECOGNITION); finishTerminal(generation); }
            }
        });
        turns.track(generation, operation);
    }
    public void translateTyped(final String text) {
        final long generation = turns.replace();
        if (generation < 0) { return; }
        activeGeneration = generation; recognitionTerminal = true; turnTerminal = false;
        EngineOperation operation = translation.detectLanguage(text, new TextTranslationEngine.LanguageDetectionCallback() {
            @Override public void onDetected(CustomLocale language) {
                if (!turns.isCurrent(generation)) { return; }
                if (language == null || (!language.equalsLanguage(first) && !language.equalsLanguage(second))) {
                    listener.onError(new EngineError(EngineError.Category.UNSUPPORTED_LANGUAGE, new int[0], 0L, "Typed language is not selected"), OperationKind.LANGUAGE_DETECTION);
                    finishTerminal(generation); return;
                }
                boolean direction = language.equalsLanguage(first);
                translate(generation, text, direction ? second : first, direction);
            }
            @Override public void onFailure(EngineError error) {
                if (turns.isCurrent(generation)) { listener.onError(error, OperationKind.LANGUAGE_DETECTION); finishTerminal(generation); }
            }
        });
        turns.track(generation, operation);
    }
    public void stopTurn() { turns.cancelCurrent(); }
    public void close() { turns.close(); }
    private void translate(final long generation, String text, CustomLocale target, final boolean direction) {
        if (!isCurrentTurn(generation)) { return; }
        EngineOperation operation = translation.translate(text, target, new TextTranslationEngine.TranslationCallback() {
            @Override public void onTranslated(String translated, CustomLocale language) {
                if (isCurrentTurn(generation) && !turnTerminal) { listener.onTranslated(translated, language, direction); finishTerminal(generation); }
            }
            @Override public void onFailure(EngineError error) {
                if (isCurrentTurn(generation) && !turnTerminal) { listener.onError(error, OperationKind.TRANSLATION); finishTerminal(generation); }
            }
        });
        turns.track(generation, operation);
    }
    private boolean markRecognitionTerminal(long generation) {
        if (!isCurrentTurn(generation) || recognitionTerminal) { return false; }
        recognitionTerminal = true; return true;
    }
    private boolean isCurrentTurn(long generation) { return activeGeneration == generation && turns.isCurrent(generation); }
    private void finishTerminal(long generation) { if (isCurrentTurn(generation) && !turnTerminal) { turnTerminal = true; listener.onTurnEnded(); } }
    private static boolean sameLocale(CustomLocale first, CustomLocale second) {
        return first == second || (first != null && first.equals(second));
    }
}
