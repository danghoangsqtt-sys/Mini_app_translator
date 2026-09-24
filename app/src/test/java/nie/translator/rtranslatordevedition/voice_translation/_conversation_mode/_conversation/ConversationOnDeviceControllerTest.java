package nie.translator.rtranslatordevedition.voice_translation._conversation_mode._conversation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import nie.translator.rtranslatordevedition.tools.CustomLocale;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineCapability;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineError;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineOperation;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineServiceErrorMapper.OperationKind;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineTurnCoordinator;
import nie.translator.rtranslatordevedition.voice_translation.engines.SpeechRecognitionEngine;
import nie.translator.rtranslatordevedition.voice_translation.engines.TextTranslationEngine;
import org.junit.Test;

public class ConversationOnDeviceControllerTest {
    @Test public void outgoingSpeechDoesNotCancelIncomingTranslationAndFinalIsBounded() {
        FakeSpeech speech = new FakeSpeech(); FakeTranslation translation = new FakeTranslation(); Events events = new Events();
        ConversationOnDeviceController controller = controller(speech, translation, events);
        CustomLocale english = new CustomLocale("en", "US");
        controller.startTurn(english);
        controller.onIncoming("ciaoit-IT5", english);
        assertEquals(1, translation.callbacks.size());
        speech.finalResult("spoken", english);
        assertEquals(Collections.singletonList("out:spoken"), events.events);
        translation.success(0, "hello", english);
        assertEquals("in:hello", events.events.get(1));
        assertEquals(1, events.ended);
    }

    @Test public void incomingFifoAndCloseSuppressLateCallbacksWithoutAccumulatingOperations() {
        FakeSpeech speech = new FakeSpeech(); FakeTranslation translation = new FakeTranslation(); Events events = new Events();
        ConversationOnDeviceController controller = controller(speech, translation, events);
        CustomLocale english = new CustomLocale("en", "US");
        controller.onIncoming("unoit-IT5", english);
        controller.onIncoming("dueit-IT5", english);
        assertEquals(1, translation.callbacks.size());
        translation.success(0, "one", english);
        assertEquals(2, translation.callbacks.size());
        controller.close();
        assertTrue(translation.operations.get(1).isCancelled());
        translation.success(1, "two", english);
        assertEquals(Collections.singletonList("in:one"), events.events);
        assertEquals(1, speech.closes); assertEquals(1, translation.closes);
    }

    @Test public void incomingFailureOrMalformedPayloadDoesNotCancelSpeechAndQueueAdvances() {
        FakeSpeech speech = new FakeSpeech(); FakeTranslation translation = new FakeTranslation(); Events events = new Events();
        ConversationOnDeviceController controller = controller(speech, translation, events); CustomLocale english = new CustomLocale("en", "US");
        controller.startTurn(english); controller.onIncoming("unoit-IT5", english); controller.onIncoming("dueit-IT5", english);
        translation.failure(0); assertEquals(2, translation.callbacks.size());
        controller.onIncoming("badx", english); speech.finalResult("once", english); speech.finalResult("duplicate", english);
        assertEquals(1, Collections.frequency(events.events, "out:once")); assertTrue(!events.events.contains("out:duplicate"));
        translation.success(1, "two", english); assertTrue(events.events.contains("in:two"));
    }

    private static ConversationOnDeviceController controller(FakeSpeech speech, FakeTranslation translation, Events events) {
        return new ConversationOnDeviceController(speech, translation, new EngineTurnCoordinator(speech, translation, null), events);
    }
    private static final class Events implements ConversationOnDeviceController.Listener {
        final List<String> events = new ArrayList<>(); int ended;
        @Override public void onPartial(String text) { events.add("partial:" + text); }
        @Override public void onOutboundFinal(String text, CustomLocale language) { events.add("out:" + text); }
        @Override public void onIncomingText(String text, CustomLocale language) { events.add("in:" + text); }
        @Override public void onError(EngineError error, OperationKind kind) { events.add("error:" + kind); }
        @Override public void onTurnEnded() { ended++; }
    }
    private static final class FakeSpeech implements SpeechRecognitionEngine {
        RecognitionCallback callback; int closes;
        @Override public EngineCapability getCapability() { return capability(); }
        @Override public List<CustomLocale> getSupportedLanguages() { return Collections.emptyList(); }
        @Override public EngineOperation start(RecognitionRequest request, RecognitionCallback value) { callback = value; return new EngineOperation(null); }
        void finalResult(String text, CustomLocale language) { callback.onResult(text, language, 1f, true); }
        @Override public boolean submitAudio(EngineOperation operation, byte[] audio, int size) { return false; }
        @Override public boolean finish(EngineOperation operation) { return false; }
        @Override public void close() { closes++; }
    }
    private static final class FakeTranslation implements TextTranslationEngine {
        final List<TranslationCallback> callbacks = new ArrayList<>(); final List<EngineOperation> operations = new ArrayList<>(); int closes;
        @Override public EngineCapability getCapability() { return capability(); }
        @Override public EngineOperation translate(String text, CustomLocale output, TranslationCallback callback) { callbacks.add(callback); EngineOperation operation = new EngineOperation(null); operations.add(operation); return operation; }
        void success(int index, String text, CustomLocale language) { callbacks.get(index).onTranslated(text, language); }
        void failure(int index) { callbacks.get(index).onFailure(new EngineError(EngineError.Category.NETWORK, new int[0], 0L, "test")); }
        @Override public EngineOperation detectLanguage(String text, LanguageDetectionCallback callback) { return new EngineOperation(null); }
        @Override public EngineOperation getSupportedLanguages(CustomLocale display, SupportedLanguagesCallback callback) { return new EngineOperation(null); }
        @Override public void close() { closes++; }
    }
    private static EngineCapability capability() { return new EngineCapability(nie.translator.rtranslatordevedition.voice_translation.engines.CapabilityState.AVAILABLE, "test"); }
}
