package nie.translator.rtranslatordevedition.voice_translation.engines.legacy;

import android.app.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import nie.translator.rtranslatordevedition.tools.CustomLocale;
import nie.translator.rtranslatordevedition.voice_translation.cloud_apis.voice.Recognizer;
import nie.translator.rtranslatordevedition.voice_translation.cloud_apis.voice.RecognizerListener;
import nie.translator.rtranslatordevedition.voice_translation.engines.CapabilityState;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineCapability;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineError;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineOperation;
import nie.translator.rtranslatordevedition.voice_translation.engines.SpeechRecognitionEngine;

/** Adapter over the legacy streaming recognizer; it owns only adapter lifecycle, never credentials. */
public final class LegacyCloudSpeechRecognitionEngine implements SpeechRecognitionEngine {
    private final Object lock = new Object();
    private final Service service;
    private boolean closed;
    private Session activeSession;

    public LegacyCloudSpeechRecognitionEngine(Service service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    @Override public EngineCapability getCapability() {
        synchronized (lock) {
            return closed ? new EngineCapability(CapabilityState.CLOSED, "Engine is closed")
                    : new EngineCapability(CapabilityState.AVAILABLE, "Legacy cloud recognizer available");
        }
    }

    @Override public List<CustomLocale> getSupportedLanguages() {
        synchronized (lock) {
            if (closed) { return Collections.emptyList(); }
        }
        return Collections.unmodifiableList(new ArrayList<>(Recognizer.getSupportedLanguages(service)));
    }

    @Override public EngineOperation start(RecognitionRequest request, RecognitionCallback callback) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(callback, "callback");
        if (request.getAudioInputMode() != AudioInputMode.PCM_STREAM) {
            return rejected(callback, unsupportedCaptureError());
        }
        final Session[] reference = new Session[1];
        final EngineOperation operation = new EngineOperation(new EngineOperation.CancelAction() {
            @Override public void onCancel() { detachAndDestroy(reference[0]); }
        });
        final Session session = new Session(operation, callback);
        reference[0] = session;
        EngineError rejection = null;
        synchronized (lock) {
            if (closed) {
                rejection = closedError();
            } else if (activeSession != null && activeSession.operation.isActive()) {
                rejection = busyError();
            } else {
                activeSession = session;
            }
        }
        if (rejection != null) {
            fail(operation, callback, rejection);
            return operation;
        }
        try {
            session.recognizer = new Recognizer(service, false, new RecognizerListener() {
                @Override public void onSpeechRecognizedResult(final String text, String languageCode, final float confidence, final boolean isFinal) {
                    deliverResult(session, text, languageCode, confidence, isFinal);
                }
                @Override public void onError(final int[] reasons, final long value) { deliverFailure(session, LegacyErrorMapper.map(reasons, value)); }
            });
            if (!isCurrent(session) || !operation.isActive()) {
                detachAndDestroy(session);
                return operation;
            }
            session.recognizer.startRecognizing(request.getLanguage().getCode(), request.getSampleRateHertz(), true);
        } catch (RuntimeException error) {
            deliverFailure(session, internalError());
        }
        return operation;
    }

    @Override public boolean submitAudio(EngineOperation operation, byte[] audio, int size) {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(audio, "audio");
        if (size < 0 || size > audio.length) { throw new IllegalArgumentException("size"); }
        final Session session = getCurrent(operation);
        if (session == null) { return false; }
        try { session.recognizer.recognize(audio, size); return true; }
        catch (RuntimeException error) { deliverFailure(session, internalError()); return false; }
    }

    @Override public boolean finish(EngineOperation operation) {
        Objects.requireNonNull(operation, "operation");
        final Session session = getCurrent(operation);
        if (session == null) { return false; }
        try { session.recognizer.finishRecognizing(); return true; }
        catch (RuntimeException error) { deliverFailure(session, internalError()); return false; }
    }

    @Override public void close() {
        Session session;
        synchronized (lock) {
            if (closed) { return; }
            closed = true;
            session = activeSession;
            activeSession = null;
        }
        if (session != null) { session.operation.cancel(); }
    }

    private void deliverResult(final Session session, final String text, String languageCode, final float confidence, final boolean isFinal) {
        if (!isCurrent(session)) { return; }
        final CustomLocale language = CustomLocale.getInstance(languageCode);
        if (isFinal) {
            session.operation.complete(new Runnable() {
                @Override public void run() {
                    detachAndDestroy(session);
                    session.callback.onResult(text, language, confidence, true);
                }
            });
        } else {
            session.operation.dispatch(new Runnable() {
                @Override public void run() { session.callback.onResult(text, language, confidence, false); }
            });
        }
    }

    private void deliverFailure(final Session session, final EngineError error) {
        if (!isCurrent(session)) { return; }
        session.operation.complete(new Runnable() {
            @Override public void run() {
                detachAndDestroy(session);
                session.callback.onFailure(error);
            }
        });
    }

    private Session getCurrent(EngineOperation operation) {
        synchronized (lock) {
            if (closed || activeSession == null || activeSession.operation != operation || !operation.isActive()) { return null; }
            return activeSession;
        }
    }

    private boolean isCurrent(Session session) {
        synchronized (lock) { return !closed && activeSession == session && session.operation.isActive(); }
    }

    private void detachAndDestroy(Session session) {
        if (session == null) { return; }
        final Recognizer recognizer;
        synchronized (lock) {
            if (activeSession == session) { activeSession = null; }
            recognizer = session.recognizer;
        }
        if (recognizer != null) {
            try {
                recognizer.finishRecognizing();
            } finally {
                recognizer.destroy();
            }
        }
    }

    private static void fail(EngineOperation operation, final RecognitionCallback callback, final EngineError error) { operation.complete(new Runnable() { @Override public void run() { callback.onFailure(error); } }); }
    private static EngineOperation rejected(RecognitionCallback callback, EngineError error) {
        EngineOperation operation = new EngineOperation(null);
        fail(operation, callback, error);
        return operation;
    }
    private static EngineError closedError() { return new EngineError(EngineError.Category.CLOSED, new int[0], 0L, "Engine is closed"); }
    private static EngineError busyError() { return new EngineError(EngineError.Category.BUSY, new int[0], 0L, "Recognition is already active"); }
    private static EngineError unsupportedCaptureError() { return new EngineError(EngineError.Category.UNSUPPORTED, new int[0], 0L, "Legacy recognizer supports PCM_STREAM only"); }
    private static EngineError internalError() { return new EngineError(EngineError.Category.INTERNAL_FAILURE, new int[0], 0L, "Legacy recognizer failure"); }

    private static final class Session {
        private final EngineOperation operation;
        private final RecognitionCallback callback;
        private Recognizer recognizer;

        private Session(EngineOperation operation, RecognitionCallback callback) {
            this.operation = operation;
            this.callback = callback;
        }
    }
}
