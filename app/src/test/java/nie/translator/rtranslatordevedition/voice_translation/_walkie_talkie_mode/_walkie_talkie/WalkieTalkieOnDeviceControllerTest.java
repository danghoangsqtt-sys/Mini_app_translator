package nie.translator.rtranslatordevedition.voice_translation._walkie_talkie_mode._walkie_talkie;

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

public class WalkieTalkieOnDeviceControllerTest {
    @Test public void explicitDirectionStartsExactlyOneRecognizerAndTargetsTheOtherLanguage() {
        FakeSpeech speech = new FakeSpeech(); FakeTranslation translation = new FakeTranslation(); Events events = new Events();
        WalkieTalkieOnDeviceController controller = controller(speech, translation, events);
        CustomLocale first = new CustomLocale("en", "US"), second = new CustomLocale("it", "IT");
        controller.setLanguages(first, second); controller.setSourceIsFirst(false); controller.startTurn();
        assertEquals(1, speech.starts); assertEquals("it", speech.request.getLanguage().getLanguage());
        speech.finalResult("ciao", second);
        assertEquals("en", translation.target.getLanguage());
        translation.success("hello", first);
        assertEquals("hello", events.translated); assertTrue(!events.sourceFirst); assertEquals(1, events.ended);
    }
    @Test public void typedThirdLanguageFailsOnceAndLanguageReplacementInvalidatesTurn() {
        FakeSpeech speech = new FakeSpeech(); FakeTranslation translation = new FakeTranslation(); Events events = new Events();
        WalkieTalkieOnDeviceController controller = controller(speech, translation, events);
        CustomLocale first = new CustomLocale("en", "US"), second = new CustomLocale("it", "IT");
        controller.setLanguages(first, second); controller.translateTyped("bonjour");
        translation.detected(new CustomLocale("fr", "FR"));
        assertEquals(EngineError.Category.UNSUPPORTED_LANGUAGE, events.error.getCategory()); assertEquals(1, events.ended);
        controller.startTurn(); controller.setLanguages(second, first);
        speech.finalResult("late", first);
        assertEquals(0, translation.translates);
    }
    @Test public void sameDirectionKeepsTurnButRealDirectionChangeAndDuplicateTerminalAreGated() {
        FakeSpeech speech = new FakeSpeech(); FakeTranslation translation = new FakeTranslation(); Events events = new Events();
        WalkieTalkieOnDeviceController controller = controller(speech, translation, events);
        CustomLocale first = new CustomLocale("en", "US"), second = new CustomLocale("it", "IT");
        controller.setLanguages(first, second); controller.startTurn(); controller.setSourceIsFirst(true);
        speech.finalResult("one", first); assertEquals(1, translation.translates); speech.finalResult("duplicate", first);
        assertEquals(1, translation.translates); translation.success("uno", second); translation.success("late", second);
        assertEquals(1, events.ended); controller.startTurn(); controller.setSourceIsFirst(false); speech.finalResult("stale", first);
        assertEquals(1, translation.translates);
    }
    @Test public void closedControllerRejectsNewWorkAndLateCallbacks() {
        FakeSpeech speech = new FakeSpeech(); FakeTranslation translation = new FakeTranslation(); Events events = new Events();
        WalkieTalkieOnDeviceController controller = controller(speech, translation, events);
        controller.setLanguages(new CustomLocale("en", "US"), new CustomLocale("it", "IT")); controller.close();
        controller.startTurn(); controller.translateTyped("ignored");
        assertEquals(0, speech.starts); assertEquals(0, translation.detections); assertEquals(0, translation.translates); assertEquals(0, events.ended);
    }
    @Test public void restoredLanguagesPreserveTurnAndRealFirstOrSecondChangeInvalidatesIt() {
        FakeSpeech speech = new FakeSpeech(); FakeTranslation translation = new FakeTranslation(); Events events = new Events();
        WalkieTalkieOnDeviceController controller = controller(speech, translation, events);
        CustomLocale first = new CustomLocale("en", "US"), second = new CustomLocale("it", "IT");
        controller.setLanguages(first, second); controller.startTurn(); controller.setLanguages(new CustomLocale("en", "US"), new CustomLocale("it", "IT"));
        speech.finalResult("kept", first); assertEquals(1, translation.translates);
        controller.startTurn(); controller.setLanguages(new CustomLocale("fr", "FR"), second); speech.finalResult("old-first", first); assertEquals(1, translation.translates);
        controller.startTurn(); controller.setLanguages(new CustomLocale("fr", "FR"), new CustomLocale("de", "DE")); speech.finalResult("old-second", first); assertEquals(1, translation.translates);
        assertEquals(0, events.ended);
    }
    private static WalkieTalkieOnDeviceController controller(FakeSpeech speech, FakeTranslation translation, Events events) { return new WalkieTalkieOnDeviceController(speech, translation, new EngineTurnCoordinator(speech, translation, null), events); }
    private static final class Events implements WalkieTalkieOnDeviceController.Listener {
        String translated; boolean sourceFirst; int ended; EngineError error;
        @Override public void onTranslated(String text, CustomLocale language, boolean direction) { translated = text; sourceFirst = direction; }
        @Override public void onError(EngineError value, OperationKind kind) { error = value; }
        @Override public void onTurnEnded() { ended++; }
    }
    private static final class FakeSpeech implements SpeechRecognitionEngine {
        int starts; RecognitionRequest request; RecognitionCallback callback;
        @Override public EngineCapability getCapability() { return capability(); }
        @Override public List<CustomLocale> getSupportedLanguages() { return Collections.emptyList(); }
        @Override public EngineOperation start(RecognitionRequest value, RecognitionCallback result) { starts++; request = value; callback = result; return new EngineOperation(null); }
        void finalResult(String text, CustomLocale language) { callback.onResult(text, language, 1f, true); }
        @Override public boolean submitAudio(EngineOperation operation, byte[] audio, int size) { return false; }
        @Override public boolean finish(EngineOperation operation) { return false; }
        @Override public void close() { }
    }
    private static final class FakeTranslation implements TextTranslationEngine {
        LanguageDetectionCallback detector; TranslationCallback translator; CustomLocale target; int translates; int detections;
        @Override public EngineCapability getCapability() { return capability(); }
        @Override public EngineOperation translate(String text, CustomLocale output, TranslationCallback callback) { translates++; target = output; translator = callback; return new EngineOperation(null); }
        void success(String text, CustomLocale language) { translator.onTranslated(text, language); }
        @Override public EngineOperation detectLanguage(String text, LanguageDetectionCallback callback) { detections++; detector = callback; return new EngineOperation(null); }
        void detected(CustomLocale language) { detector.onDetected(language); }
        @Override public EngineOperation getSupportedLanguages(CustomLocale display, SupportedLanguagesCallback callback) { return new EngineOperation(null); }
        @Override public void close() { }
    }
    private static EngineCapability capability() { return new EngineCapability(nie.translator.rtranslatordevedition.voice_translation.engines.CapabilityState.AVAILABLE, "test"); }
}
