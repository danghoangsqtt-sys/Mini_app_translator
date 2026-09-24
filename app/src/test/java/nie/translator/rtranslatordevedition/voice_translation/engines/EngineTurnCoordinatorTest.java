package nie.translator.rtranslatordevedition.voice_translation.engines;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import java.util.Collections;
import java.util.List;
import nie.translator.rtranslatordevedition.tools.CustomLocale;
import org.junit.Test;

public class EngineTurnCoordinatorTest {
    @Test public void replacementCancelsOldOperationAndCloseClosesEachEngineOnce() {
        FakeSpeech speech = new FakeSpeech(); FakeTranslation translation = new FakeTranslation(); FakeOutput output = new FakeOutput();
        EngineTurnCoordinator coordinator = new EngineTurnCoordinator(speech, translation, output);
        long old = coordinator.replace(); EngineOperation operation = new EngineOperation(null); coordinator.track(old, operation);
        long current = coordinator.replace();
        assertFalse(operation.isActive()); assertFalse(coordinator.isCurrent(old));
        coordinator.close(); coordinator.close();
        assertEquals(1, speech.closes); assertEquals(1, translation.closes); assertEquals(1, output.closes); assertFalse(coordinator.isCurrent(current));
    }
    private static final class FakeSpeech implements SpeechRecognitionEngine {
        int closes; @Override public EngineCapability getCapability() { return capability(); } @Override public List<CustomLocale> getSupportedLanguages() { return Collections.emptyList(); }
        @Override public EngineOperation start(RecognitionRequest request, RecognitionCallback callback) { return new EngineOperation(null); } @Override public boolean submitAudio(EngineOperation operation, byte[] audio, int size) { return false; } @Override public boolean finish(EngineOperation operation) { return false; } @Override public void close() { closes++; }
    }
    private static final class FakeTranslation implements TextTranslationEngine {
        int closes; @Override public EngineCapability getCapability() { return capability(); } @Override public EngineOperation translate(String text, CustomLocale output, TranslationCallback callback) { return new EngineOperation(null); } @Override public EngineOperation detectLanguage(String text, LanguageDetectionCallback callback) { return new EngineOperation(null); } @Override public EngineOperation getSupportedLanguages(CustomLocale display, SupportedLanguagesCallback callback) { return new EngineOperation(null); } @Override public void close() { closes++; }
    }
    private static final class FakeOutput implements SpeechOutputEngine {
        int closes; @Override public EngineCapability getCapability() { return capability(); } @Override public EngineOperation setLanguage(CustomLocale language, ResultCallback callback) { return new EngineOperation(null); } @Override public EngineOperation speak(CharSequence text, QueueMode mode, ResultCallback callback) { return new EngineOperation(null); } @Override public void stop() { } @Override public void close() { closes++; }
    }
    private static EngineCapability capability() { return new EngineCapability(CapabilityState.AVAILABLE, "test"); }
}
