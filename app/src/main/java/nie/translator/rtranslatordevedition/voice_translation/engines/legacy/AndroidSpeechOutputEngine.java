package nie.translator.rtranslatordevedition.voice_translation.engines.legacy;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import nie.translator.rtranslatordevedition.tools.CustomLocale;
import nie.translator.rtranslatordevedition.tools.TTS;
import nie.translator.rtranslatordevedition.voice_translation.engines.CapabilityState;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineCapability;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineError;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineOperation;
import nie.translator.rtranslatordevedition.voice_translation.engines.SpeechOutputEngine;

/** Adapter over the existing Android TTS wrapper. Android constants stay within this legacy class. */
public final class AndroidSpeechOutputEngine implements SpeechOutputEngine {
    private final Object lock = new Object();
    private final TTS tts;
    private final Context context;
    private final AtomicLong utteranceIds = new AtomicLong();
    private boolean closed;

    public AndroidSpeechOutputEngine(TTS tts, Context context) {
        this.tts = Objects.requireNonNull(tts, "tts");
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override public EngineCapability getCapability() {
        if (isClosed()) { return new EngineCapability(CapabilityState.CLOSED, "Engine is closed"); }
        return tts.isActive() ? new EngineCapability(CapabilityState.AVAILABLE, "Android speech output available")
                : new EngineCapability(CapabilityState.SETUP_REQUIRED, "Speech output is not initialized");
    }

    @Override public EngineOperation setLanguage(CustomLocale language, final ResultCallback callback) {
        Objects.requireNonNull(language, "language");
        Objects.requireNonNull(callback, "callback");
        final EngineOperation operation = new EngineOperation(null);
        if (isClosed()) { failClosed(operation, callback); return operation; }
        try {
            if (tts.setLanguage(language, context) == TextToSpeech.SUCCESS) { succeed(operation, callback); }
            else { failInternal(operation, callback); }
        } catch (RuntimeException error) { failInternal(operation, callback); }
        return operation;
    }

    @Override public EngineOperation speak(CharSequence text, QueueMode queueMode, final ResultCallback callback) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(queueMode, "queueMode");
        Objects.requireNonNull(callback, "callback");
        final EngineOperation operation = new EngineOperation(new EngineOperation.CancelAction() { @Override public void onCancel() { stop(); } });
        if (isClosed()) { failClosed(operation, callback); return operation; }
        int queue = queueMode == QueueMode.FLUSH ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD;
        try {
            int status = tts.speak(text, queue, new Bundle(), "engine-" + utteranceIds.incrementAndGet());
            if (status == TextToSpeech.SUCCESS) { succeed(operation, callback); }
            else { failInternal(operation, callback); }
        } catch (RuntimeException error) { failInternal(operation, callback); }
        return operation;
    }

    @Override public void stop() {
        if (!isClosed()) { tts.stop(); }
    }

    @Override public void close() {
        synchronized (lock) {
            if (closed) { return; }
            closed = true;
        }
        tts.stop();
        tts.shutdown();
    }

    private boolean isClosed() {
        synchronized (lock) { return closed; }
    }

    private static void succeed(EngineOperation operation, final ResultCallback callback) { operation.complete(new Runnable() { @Override public void run() { callback.onSuccess(); } }); }
    private static void failClosed(EngineOperation operation, final ResultCallback callback) { operation.complete(new Runnable() { @Override public void run() { callback.onFailure(new EngineError(EngineError.Category.CLOSED, new int[0], 0L, "Engine is closed")); } }); }
    private static void failInternal(EngineOperation operation, final ResultCallback callback) { operation.complete(new Runnable() { @Override public void run() { callback.onFailure(new EngineError(EngineError.Category.INTERNAL_FAILURE, new int[0], 0L, "Speech output request failed")); } }); }
}
