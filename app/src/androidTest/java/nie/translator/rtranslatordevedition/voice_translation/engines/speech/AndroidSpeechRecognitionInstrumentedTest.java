package nie.translator.rtranslatordevedition.voice_translation.engines.speech;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.os.Bundle;
import android.os.Looper;
import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import nie.translator.rtranslatordevedition.tools.CustomLocale;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineCapability;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineOperation;
import nie.translator.rtranslatordevedition.voice_translation.engines.SpeechRecognitionEngine;
import org.junit.Test;
import org.junit.Assume;
import org.junit.runner.RunWith;

/** Lifecycle/intent smoke only; it does not assert transcription quality or offline recognition. */
@RunWith(AndroidJUnit4.class)
public class AndroidSpeechRecognitionInstrumentedTest {
    @Test public void capabilityProbeAndCloseDoNotCrash() throws Throwable {
        final Context context = InstrumentationRegistry.getTargetContext();
        final AtomicReference<EngineCapability> capability = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override public void run() {
                AndroidSpeechRecognitionEngine engine = new AndroidSpeechRecognitionEngine(context);
                capability.set(engine.getCapability());
                engine.close();
            }
        });
        assertNotNull(capability.get());
    }

    @Test public void intentRequestsLanguagePartialResultsAndOfflinePreference() {
        SpeechRecognitionEngine.RecognitionRequest request = new SpeechRecognitionEngine.RecognitionRequest(
                new CustomLocale("it", "IT"), 0, SpeechRecognitionEngine.AudioInputMode.ENGINE_CAPTURE);
        Intent intent = AndroidSpeechRecognizerClient.buildRecognizerIntent(request);
        assertEquals("it-IT", intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE));
        assertEquals(RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL));
        assertEquals(true, intent.getBooleanExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false));
        assertEquals(true, intent.getBooleanExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false));
        assertEquals(1, intent.getIntExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 0));
    }

    @Test public void platformClientCanBeCreatedAndDestroyedOnMainWhenAvailable() throws Throwable {
        final Context context = InstrumentationRegistry.getTargetContext();
        final AtomicReference<Boolean> available = new AtomicReference<>(false);
        final AtomicReference<Boolean> constructed = new AtomicReference<>(false);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override public void run() {
                AndroidSpeechRecognizerClientFactory factory = new AndroidSpeechRecognizerClientFactory(context);
                available.set(factory.isSystemRecognitionAvailable());
                if (!available.get()) { return; }
                SpeechRecognizerClient client = factory.createSystem();
                client.destroy();
                constructed.set(true);
            }
        });
        Assume.assumeTrue("No system recognizer is installed on this target", available.get());
        assertTrue(constructed.get());
    }

    @Test public void schedulerMarshalsWorkerExecuteAndWaitToMainLooper() throws Exception {
        final AndroidMainThreadScheduler scheduler = new AndroidMainThreadScheduler();
        final AtomicReference<Boolean> executeMain = new AtomicReference<>(false);
        final AtomicReference<Boolean> waitMain = new AtomicReference<>(false);
        Thread worker = new Thread(new Runnable() {
            @Override public void run() {
                scheduler.execute(new Runnable() { @Override public void run() { executeMain.set(Looper.myLooper() == Looper.getMainLooper()); } });
                scheduler.executeAndWait(new Runnable() { @Override public void run() { waitMain.set(Looper.myLooper() == Looper.getMainLooper()); } });
            }
        });
        worker.start();
        worker.join();
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertTrue(executeMain.get());
        assertTrue(waitMain.get());
    }

    @Test public void parsingDeliversPartialFinalConfidenceAndEmptyFinalError() {
        ParsingListener listener = new ParsingListener();
        Bundle partial = new Bundle();
        partial.putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION,
                new java.util.ArrayList<>(java.util.Collections.singletonList("partial")));
        AndroidSpeechRecognizerClient.deliver(partial, listener, false);
        assertEquals("partial", listener.text);
        assertEquals(-1f, listener.confidence, 0f);
        Bundle finalResult = new Bundle(partial);
        finalResult.putFloatArray(SpeechRecognizer.CONFIDENCE_SCORES, new float[] { .6f });
        AndroidSpeechRecognizerClient.deliver(finalResult, listener, true);
        assertEquals(.6f, listener.confidence, 0f);
        AndroidSpeechRecognizerClient.deliver(new Bundle(), listener, true);
        assertEquals(SpeechRecognizer.ERROR_NO_MATCH, listener.error);
    }

    @Test public void immediateStartCancelAndCloseHasNoLateLifecycleCallbackWhenRecognizerExists() throws Throwable {
        final Context context = InstrumentationRegistry.getTargetContext();
        final AtomicReference<Boolean> available = new AtomicReference<>(false);
        final AtomicInteger callbacks = new AtomicInteger();
        final AtomicInteger callbacksAtClose = new AtomicInteger();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override public void run() {
                AndroidSpeechRecognizerClientFactory factory = new AndroidSpeechRecognizerClientFactory(context);
                available.set(factory.isSystemRecognitionAvailable() || factory.isOnDeviceRecognitionAvailable());
                if (!available.get()) { return; }
                AndroidSpeechRecognitionEngine engine = new AndroidSpeechRecognitionEngine(context);
                EngineOperation operation = engine.start(new SpeechRecognitionEngine.RecognitionRequest(
                        new CustomLocale("en", "US"), 0, SpeechRecognitionEngine.AudioInputMode.ENGINE_CAPTURE),
                        new SpeechRecognitionEngine.RecognitionCallback() {
                            @Override public void onResult(String text, CustomLocale language, float confidence, boolean isFinal) {
                                callbacks.incrementAndGet();
                            }
                            @Override public void onFailure(nie.translator.rtranslatordevedition.voice_translation.engines.EngineError error) {
                                callbacks.incrementAndGet();
                            }
                        });
                operation.cancel();
                engine.close();
                callbacksAtClose.set(callbacks.get());
            }
        });
        Assume.assumeTrue("No speech recognizer is installed on this target", available.get());
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertEquals(callbacksAtClose.get(), callbacks.get());
    }

    private static final class ParsingListener implements SpeechRecognizerClient.Listener {
        private String text;
        private float confidence;
        private int error = -1;
        @Override public void onPartialResult(String text, float confidence) { this.text = text; this.confidence = confidence; }
        @Override public void onFinalResult(String text, float confidence) { this.text = text; this.confidence = confidence; }
        @Override public void onError(int errorCode) { error = errorCode; }
    }
}
