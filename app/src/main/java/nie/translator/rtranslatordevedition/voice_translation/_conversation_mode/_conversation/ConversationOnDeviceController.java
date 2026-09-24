package nie.translator.rtranslatordevedition.voice_translation._conversation_mode._conversation;

import java.util.ArrayDeque;
import java.util.Queue;
import nie.translator.rtranslatordevedition.tools.CustomLocale;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineError;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineOperation;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineServiceErrorMapper.OperationKind;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineTurnCoordinator;
import nie.translator.rtranslatordevedition.voice_translation.engines.SpeechRecognitionEngine;
import nie.translator.rtranslatordevedition.voice_translation.engines.TextTranslationEngine;

/** Conversation has independent recognition and incoming-translation lanes. */
public final class ConversationOnDeviceController {
    public interface Listener {
        void onPartial(String text);
        void onOutboundFinal(String text, CustomLocale language);
        void onIncomingText(String text, CustomLocale language);
        void onError(EngineError error, OperationKind kind);
        void onTurnEnded();
    }
    private final Object lock = new Object();
    private final SpeechRecognitionEngine speech;
    private final TextTranslationEngine translation;
    private final EngineTurnCoordinator recognitionTurns;
    private final Listener listener;
    private final Queue<Incoming> incoming = new ArrayDeque<>();
    private boolean closed;
    private boolean translating;
    private long recognitionGeneration = -1L;
    private boolean recognitionTerminal;
    private long incomingGeneration;
    private EngineOperation activeIncoming;

    public ConversationOnDeviceController(SpeechRecognitionEngine speech, TextTranslationEngine translation,
                                          EngineTurnCoordinator recognitionTurns, Listener listener) {
        this.speech = speech; this.translation = translation; this.recognitionTurns = recognitionTurns; this.listener = listener;
    }

    public void startTurn(final CustomLocale language) {
        final long generation = recognitionTurns.replace();
        if (generation < 0) { return; }
        synchronized (lock) { recognitionGeneration = generation; recognitionTerminal = false; }
        EngineOperation operation = speech.start(new SpeechRecognitionEngine.RecognitionRequest(language, 0,
                SpeechRecognitionEngine.AudioInputMode.ENGINE_CAPTURE), new SpeechRecognitionEngine.RecognitionCallback() {
            @Override public void onResult(String text, CustomLocale resultLanguage, float confidence, boolean isFinal) {
                if (!isRecognitionCurrent(generation)) { return; }
                if (!isFinal) { listener.onPartial(text); return; }
                if (!markRecognitionTerminal(generation)) { return; }
                listener.onOutboundFinal(text, resultLanguage);
                listener.onTurnEnded();
            }
            @Override public void onFailure(EngineError error) {
                if (!markRecognitionTerminal(generation)) { return; }
                listener.onError(error, OperationKind.SPEECH_RECOGNITION);
                listener.onTurnEnded();
            }
        });
        recognitionTurns.track(generation, operation);
    }

    public void stopTurn() { recognitionTurns.cancelCurrent(); }

    public void onIncoming(String payload, CustomLocale localLanguage) {
        final ConversationPayloadCodec.Payload decoded;
        try { decoded = ConversationPayloadCodec.decode(payload); }
        catch (IllegalArgumentException ignored) {
            if (isOpen()) { listener.onError(new EngineError(EngineError.Category.UNSUPPORTED, new int[0], 0L, "Malformed conversation payload"), OperationKind.TRANSLATION); }
            return;
        }
        synchronized (lock) {
            if (closed) { return; }
            incoming.add(new Incoming(decoded, localLanguage));
        }
        processNext();
    }

    public void close() {
        EngineOperation operation;
        synchronized (lock) {
            if (closed) { return; }
            closed = true; incomingGeneration++; incoming.clear(); translating = false;
            operation = activeIncoming; activeIncoming = null;
        }
        if (operation != null) { operation.cancel(); }
        recognitionTurns.close();
    }

    private boolean isOpen() { synchronized (lock) { return !closed; } }

    private boolean isRecognitionCurrent(long generation) {
        synchronized (lock) { return !closed && !recognitionTerminal && recognitionGeneration == generation && recognitionTurns.isCurrent(generation); }
    }

    private boolean markRecognitionTerminal(long generation) {
        synchronized (lock) {
            if (closed || recognitionTerminal || recognitionGeneration != generation || !recognitionTurns.isCurrent(generation)) { return false; }
            recognitionTerminal = true;
            return true;
        }
    }

    private void processNext() {
        final Incoming work; final long generation;
        synchronized (lock) {
            if (closed || translating || incoming.isEmpty()) { return; }
            translating = true; work = incoming.remove(); generation = ++incomingGeneration;
        }
        if (work.payload.getLanguage().equalsLanguage(work.localLanguage)) {
            if (isIncomingCurrent(generation)) { listener.onIncomingText(work.payload.getText(), work.localLanguage); }
            completeIncoming(generation);
            return;
        }
        EngineOperation operation = translation.translate(work.payload.getText(), work.localLanguage,
                new TextTranslationEngine.TranslationCallback() {
                    @Override public void onTranslated(String text, CustomLocale language) {
                        if (isIncomingCurrent(generation)) { listener.onIncomingText(text, language); }
                        completeIncoming(generation);
                    }
                    @Override public void onFailure(EngineError error) {
                        if (isIncomingCurrent(generation)) { listener.onError(error, OperationKind.TRANSLATION); }
                        completeIncoming(generation);
                    }
                });
        boolean cancel;
        synchronized (lock) {
            cancel = closed || incomingGeneration != generation || !translating;
            if (!cancel) { activeIncoming = operation; }
        }
        if (cancel) { operation.cancel(); }
    }

    private boolean isIncomingCurrent(long generation) {
        synchronized (lock) { return !closed && translating && incomingGeneration == generation; }
    }

    private void completeIncoming(long generation) {
        synchronized (lock) {
            if (closed || incomingGeneration != generation) { return; }
            translating = false; activeIncoming = null;
        }
        processNext();
    }

    private static final class Incoming {
        private final ConversationPayloadCodec.Payload payload; private final CustomLocale localLanguage;
        private Incoming(ConversationPayloadCodec.Payload payload, CustomLocale localLanguage) { this.payload = payload; this.localLanguage = localLanguage; }
    }
}
