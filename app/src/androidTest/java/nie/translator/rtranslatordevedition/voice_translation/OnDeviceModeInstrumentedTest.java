package nie.translator.rtranslatordevedition.voice_translation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertSame;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.util.SparseArray;
import android.view.ContextThemeWrapper;
import androidx.test.InstrumentationRegistry;
import java.util.Collections;
import java.util.List;
import nie.translator.rtranslatordevedition.R;
import nie.translator.rtranslatordevedition.tools.CustomLocale;
import nie.translator.rtranslatordevedition.voice_translation._walkie_talkie_mode._walkie_talkie.WalkieTalkieOnDeviceController;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineCapability;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineError;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineOperation;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineServiceErrorMapper.OperationKind;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineTurnCoordinator;
import nie.translator.rtranslatordevedition.voice_translation.engines.SpeechRecognitionEngine;
import nie.translator.rtranslatordevedition.voice_translation.engines.TextTranslationEngine;
import nie.translator.rtranslatordevedition.voice_translation.engines.SpeechOutputEngine;
import nie.translator.rtranslatordevedition.voice_translation.engines.ondevice.OnDeviceEngineFactory;
import nie.translator.rtranslatordevedition.voice_translation.engines.speech.AndroidSpeechRecognitionEngine;
import nie.translator.rtranslatordevedition.voice_translation.engines.translation.MlKitTextTranslationEngine;
import org.junit.Test;

/** API 36 UI/lifecycle smoke only; it makes no transcription, offline-quality, or Bluetooth claim. */
public class OnDeviceModeInstrumentedTest {
    @Test public void directionControlIsSingleSelectedAndItsCheckedStateSurvivesReinflate() {
        final RadioGroup[] group = new RadioGroup[1]; final SparseArray<android.os.Parcelable> state = new SparseArray<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override public void run() {
                ContextThemeWrapper themed = new ContextThemeWrapper(InstrumentationRegistry.getTargetContext(), R.style.Theme_Speech);
                View view = LayoutInflater.from(themed).inflate(R.layout.fragment_walkie_talkie, null, false);
                group[0] = view.findViewById(R.id.sourceDirectionGroup);
                group[0].check(R.id.sourceSecond); group[0].saveHierarchyState(state);
                View restored = LayoutInflater.from(themed).inflate(R.layout.fragment_walkie_talkie, null, false);
                group[0] = restored.findViewById(R.id.sourceDirectionGroup); group[0].restoreHierarchyState(state);
                assertEquals(R.id.sourceSecond, group[0].getCheckedRadioButtonId());
            }
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertFalse(((RadioButton) group[0].getChildAt(0)).isChecked() && ((RadioButton) group[0].getChildAt(1)).isChecked());
    }

    @Test public void modeFacingTurnCanStartCancelAndCloseWithoutALateCallback() {
        FakeSpeech speech = new FakeSpeech(); FakeTranslation translation = new FakeTranslation(); final int[] callbacks = { 0 };
        WalkieTalkieOnDeviceController controller = new WalkieTalkieOnDeviceController(speech, translation,
                new EngineTurnCoordinator(speech, translation, null), new WalkieTalkieOnDeviceController.Listener() {
                    @Override public void onTranslated(String text, CustomLocale language, boolean sourceIsFirst) { callbacks[0]++; }
                    @Override public void onError(EngineError error, OperationKind kind) { callbacks[0]++; }
                    @Override public void onTurnEnded() { callbacks[0]++; }
                });
        controller.setLanguages(new CustomLocale("en", "US"), new CustomLocale("it", "IT"));
        controller.startTurn(); controller.stopTurn(); controller.close(); speech.lateFinal();
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertEquals(1, speech.starts); assertEquals(0, callbacks[0]);
    }

    @Test public void productionFactoryUsesOnDeviceEnginesAndKeepsSharedOutputIdentity() {
        SpeechOutputEngine output = new FakeOutput();
        OnDeviceEngineFactory factory = new OnDeviceEngineFactory(InstrumentationRegistry.getTargetContext(), output);
        assertEquals(nie.translator.rtranslatordevedition.voice_translation.engines.EngineType.ON_DEVICE, factory.getType());
        assertTrue(factory.createSpeechRecognitionEngine() instanceof AndroidSpeechRecognitionEngine);
        assertTrue(factory.createTextTranslationEngine() instanceof MlKitTextTranslationEngine);
        assertSame(output, factory.createSpeechOutputEngine());
    }

    private static final class FakeSpeech implements SpeechRecognitionEngine {
        int starts; RecognitionCallback callback;
        @Override public EngineCapability getCapability() { return capability(); }
        @Override public List<CustomLocale> getSupportedLanguages() { return Collections.emptyList(); }
        @Override public EngineOperation start(RecognitionRequest request, RecognitionCallback value) { starts++; callback = value; return new EngineOperation(null); }
        void lateFinal() { callback.onResult("late", new CustomLocale("en", "US"), 1f, true); }
        @Override public boolean submitAudio(EngineOperation operation, byte[] audio, int size) { return false; }
        @Override public boolean finish(EngineOperation operation) { return false; }
        @Override public void close() { }
    }
    private static final class FakeTranslation implements TextTranslationEngine {
        @Override public EngineCapability getCapability() { return capability(); }
        @Override public EngineOperation translate(String text, CustomLocale output, TranslationCallback callback) { return new EngineOperation(null); }
        @Override public EngineOperation detectLanguage(String text, LanguageDetectionCallback callback) { return new EngineOperation(null); }
        @Override public EngineOperation getSupportedLanguages(CustomLocale display, SupportedLanguagesCallback callback) { return new EngineOperation(null); }
        @Override public void close() { }
    }
    private static final class FakeOutput implements SpeechOutputEngine {
        @Override public EngineCapability getCapability() { return capability(); }
        @Override public EngineOperation setLanguage(CustomLocale language, ResultCallback callback) { return new EngineOperation(null); }
        @Override public EngineOperation speak(CharSequence text, QueueMode mode, ResultCallback callback) { return new EngineOperation(null); }
        @Override public void stop() { }
        @Override public void close() { }
    }
    private static EngineCapability capability() { return new EngineCapability(nie.translator.rtranslatordevedition.voice_translation.engines.CapabilityState.AVAILABLE, "test"); }
}
