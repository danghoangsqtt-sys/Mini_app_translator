package nie.translator.rtranslatordevedition.voice_translation.engines.speech;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import nie.translator.rtranslatordevedition.voice_translation.engines.SpeechRecognitionEngine;

/** Adapter for exactly one platform SpeechRecognizer. All entry points are called on main. */
final class AndroidSpeechRecognizerClient implements SpeechRecognizerClient {
    private final SpeechRecognizer recognizer;
    private final AtomicBoolean destroyed = new AtomicBoolean();

    AndroidSpeechRecognizerClient(SpeechRecognizer recognizer) {
        this.recognizer = recognizer;
    }

    @Override public void start(SpeechRecognitionEngine.RecognitionRequest request, final Listener listener) {
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { }
            @Override public void onBeginningOfSpeech() { }
            @Override public void onRmsChanged(float rmsdB) { }
            @Override public void onBufferReceived(byte[] buffer) { }
            @Override public void onEndOfSpeech() { }
            @Override public void onError(int error) { listener.onError(error); }
            @Override public void onResults(Bundle results) { deliver(results, listener, true); }
            @Override public void onPartialResults(Bundle partialResults) { deliver(partialResults, listener, false); }
            @Override public void onEvent(int eventType, Bundle params) { }
        });
        recognizer.startListening(buildRecognizerIntent(request));
    }

    @Override public void stop() { recognizer.stopListening(); }
    @Override public void cancel() { recognizer.cancel(); }
    @Override public void destroy() {
        if (destroyed.compareAndSet(false, true)) { recognizer.destroy(); }
    }

    static Intent buildRecognizerIntent(SpeechRecognitionEngine.RecognitionRequest request) {
        return new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, request.getLanguage().getCode())
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
    }

    static void deliver(Bundle results, Listener listener, boolean isFinal) {
        ArrayList<String> values = results == null ? null
                : results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (values == null || values.isEmpty() || values.get(0) == null || values.get(0).trim().isEmpty()) {
            if (isFinal) { listener.onError(SpeechRecognizer.ERROR_NO_MATCH); }
            return;
        }
        float[] confidences = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
        float confidence = confidences != null && confidences.length > 0 ? confidences[0] : -1f;
        if (isFinal) { listener.onFinalResult(values.get(0), confidence); }
        else { listener.onPartialResult(values.get(0), confidence); }
    }
}
