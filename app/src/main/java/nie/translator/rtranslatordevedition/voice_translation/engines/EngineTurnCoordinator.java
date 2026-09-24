package nie.translator.rtranslatordevedition.voice_translation.engines;

import java.util.ArrayList;
import java.util.List;

/** Owns service generations and operation cancellation without invoking client code under a lock. */
public final class EngineTurnCoordinator {
    private final Object lock = new Object();
    private final SpeechRecognitionEngine speech;
    private final TextTranslationEngine translation;
    private final SpeechOutputEngine output;
    private final List<EngineOperation> operations = new ArrayList<>();
    private long generation;
    private boolean closed;

    public EngineTurnCoordinator(SpeechRecognitionEngine speech, TextTranslationEngine translation, SpeechOutputEngine output) {
        this.speech = speech; this.translation = translation; this.output = output;
    }
    public long replace() {
        List<EngineOperation> old;
        synchronized (lock) { if (closed) { return -1L; } generation++; old = drainLocked(); }
        cancel(old);
        return generation;
    }
    public void track(long expectedGeneration, EngineOperation operation) {
        boolean cancel;
        synchronized (lock) { cancel = closed || generation != expectedGeneration; if (!cancel) { operations.add(operation); } }
        if (cancel) { operation.cancel(); }
    }
    public boolean isCurrent(long expectedGeneration) {
        synchronized (lock) { return !closed && generation == expectedGeneration; }
    }
    public void cancelCurrent() { replace(); }
    public void close() {
        List<EngineOperation> old;
        synchronized (lock) { if (closed) { return; } closed = true; generation++; old = drainLocked(); }
        cancel(old); speech.close(); translation.close(); if (output != null) { output.close(); }
    }
    private List<EngineOperation> drainLocked() { List<EngineOperation> copy = new ArrayList<>(operations); operations.clear(); return copy; }
    private static void cancel(List<EngineOperation> operations) { for (EngineOperation operation : operations) { operation.cancel(); } }
}
