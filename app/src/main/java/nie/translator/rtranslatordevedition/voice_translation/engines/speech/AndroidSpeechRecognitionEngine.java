package nie.translator.rtranslatordevedition.voice_translation.engines.speech;

import android.content.Context;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import nie.translator.rtranslatordevedition.tools.CustomLocale;
import nie.translator.rtranslatordevedition.voice_translation.engines.CapabilityState;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineCapability;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineError;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineOperation;
import nie.translator.rtranslatordevedition.voice_translation.engines.SpeechRecognitionEngine;

/** One bounded, engine-owned speech utterance at a time; never restarts recognition automatically. */
public final class AndroidSpeechRecognitionEngine implements SpeechRecognitionEngine {
    private static final long WATCHDOG_MILLIS = 30_000L;
    private final Object lock = new Object();
    private final MainThreadScheduler scheduler;
    private final SpeechRecognizerClientFactory factory;
    private boolean closed;
    private long nextGeneration;
    private Session activeSession;

    public AndroidSpeechRecognitionEngine(Context context) {
        this(new AndroidMainThreadScheduler(), new AndroidSpeechRecognizerClientFactory(context));
    }

    AndroidSpeechRecognitionEngine(MainThreadScheduler scheduler, SpeechRecognizerClientFactory factory) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.factory = Objects.requireNonNull(factory, "factory");
    }

    @Override public EngineCapability getCapability() {
        synchronized (lock) {
            if (closed) { return new EngineCapability(CapabilityState.CLOSED, "Speech recognizer is closed"); }
        }
        final boolean[] availability = new boolean[2];
        try {
            scheduler.executeAndWait(new Runnable() {
                @Override public void run() {
                    availability[0] = factory.isOnDeviceRecognitionAvailable();
                    availability[1] = factory.isSystemRecognitionAvailable();
                }
            });
        } catch (RuntimeException unavailable) {
            return new EngineCapability(CapabilityState.TEMPORARILY_UNAVAILABLE,
                    "Speech recognition availability could not be checked");
        }
        synchronized (lock) {
            if (closed) { return new EngineCapability(CapabilityState.CLOSED, "Speech recognizer is closed"); }
        }
        if (availability[0] && availability[1]) {
            return new EngineCapability(CapabilityState.AVAILABLE,
                    "On-device speech recognition is preferred; system fallback may use network and offline preference is not guaranteed");
        }
        if (availability[0]) {
            return new EngineCapability(CapabilityState.AVAILABLE, "On-device speech recognition is available");
        }
        if (availability[1]) {
            return new EngineCapability(CapabilityState.AVAILABLE,
                    "System speech recognition is available and may use network access");
        }
        return new EngineCapability(CapabilityState.UNSUPPORTED, "No speech recognition service is available");
    }

    @Override public List<CustomLocale> getSupportedLanguages() {
        return Collections.emptyList();
    }

    @Override public EngineOperation start(RecognitionRequest request, RecognitionCallback callback) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(callback, "callback");
        if (request.getAudioInputMode() != AudioInputMode.ENGINE_CAPTURE) {
            return rejected(callback, unsupportedCaptureError());
        }
        final Session session;
        final EngineError rejection;
        synchronized (lock) {
            if (closed) {
                session = null;
                rejection = closedError();
            } else if (activeSession != null && activeSession.operation.isActive()) {
                session = null;
                rejection = busyError();
            } else {
                final Session[] reference = new Session[1];
                EngineOperation operation = new EngineOperation(new EngineOperation.CancelAction() {
                    @Override public void onCancel() { cancelSession(reference[0]); }
                });
                session = new Session(++nextGeneration, request, callback, operation);
                reference[0] = session;
                activeSession = session;
                rejection = null;
            }
        }
        if (rejection != null) { return rejected(callback, rejection); }
        scheduler.execute(new Runnable() {
            @Override public void run() { begin(session); }
        });
        return session.operation;
    }

    @Override public boolean submitAudio(EngineOperation operation, byte[] audio, int size) {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(audio, "audio");
        if (size < 0 || size > audio.length) { throw new IllegalArgumentException("size"); }
        return false;
    }

    @Override public boolean finish(EngineOperation operation) {
        Objects.requireNonNull(operation, "operation");
        final Session session = currentFor(operation);
        if (session == null) { return false; }
        scheduler.execute(new Runnable() {
            @Override public void run() { stopOnce(session); }
        });
        return true;
    }

    @Override public void close() {
        final Session session;
        synchronized (lock) {
            if (closed) { return; }
            closed = true;
            session = activeSession;
            activeSession = null;
        }
        if (session != null) { session.operation.cancel(); }
    }

    private void begin(final Session session) {
        if (!isCurrent(session)) { return; }
        final boolean systemAvailable;
        final boolean onDeviceAvailable;
        try {
            systemAvailable = factory.isSystemRecognitionAvailable();
            onDeviceAvailable = factory.isOnDeviceRecognitionAvailable();
        } catch (RuntimeException failure) {
            fail(session, unavailableError());
            return;
        }
        if (!systemAvailable && !onDeviceAvailable) {
            fail(session, unavailableError());
            return;
        }
        SpeechRecognizerClient client = null;
        if (onDeviceAvailable) {
            try {
                client = factory.createOnDevice();
            } catch (SecurityException denied) {
                fail(session, permissionError());
                return;
            } catch (RuntimeException ignored) {
                // The only permitted fallback boundary is before listening starts.
            }
        }
        if (client == null && systemAvailable) {
            try {
                client = factory.createSystem();
            } catch (SecurityException denied) {
                fail(session, permissionError());
                return;
            } catch (RuntimeException failure) {
                fail(session, unavailableError());
                return;
            }
        }
        if (client == null) {
            fail(session, unavailableError());
            return;
        }
        if (!attachClient(session, client)) {
            destroyUnattached(client);
            return;
        }
        try {
            if (!armAndMarkListening(session)) { return; }
            client.start(session.request, new ClientListener(session));
        } catch (SecurityException denied) {
            fail(session, permissionError());
        } catch (RuntimeException failure) {
            // A started session never falls back or retries.
            fail(session, internalError());
        }
    }

    private boolean armAndMarkListening(Session session) {
        synchronized (lock) {
            if (!isCurrentLocked(session)) { return false; }
            session.listening = true;
        }
        scheduler.schedule(session.watchdog, WATCHDOG_MILLIS);
        return true;
    }

    private boolean attachClient(Session session, SpeechRecognizerClient client) {
        synchronized (lock) {
            if (!isCurrentLocked(session)) { return false; }
            session.client = client;
            return true;
        }
    }

    private void stopOnce(Session session) {
        final SpeechRecognizerClient client;
        synchronized (lock) {
            if (!isCurrentLocked(session) || session.stopRequested || session.client == null) { return; }
            session.stopRequested = true;
            client = session.client;
        }
        try {
            client.stop();
        } catch (RuntimeException failure) {
            fail(session, internalError());
        }
    }

    private void cancelSession(final Session session) {
        if (session == null) { return; }
        scheduler.execute(new Runnable() {
            @Override public void run() {
                detach(session);
                cleanup(session, true);
            }
        });
    }

    private void timeout(Session session) {
        if (!isCurrent(session)) { return; }
        fail(session, AndroidSpeechErrorMapper.timeout());
    }

    private void partial(Session session, final String text, final float confidence) {
        if (!isCurrent(session)) { return; }
        session.operation.dispatch(new Runnable() {
            @Override public void run() {
                session.callback.onResult(text, session.request.getLanguage(), confidence, false);
            }
        });
    }

    private void finalResult(Session session, final String text, final float confidence) {
        if (!detach(session)) { return; }
        cleanup(session, false);
        session.operation.complete(new Runnable() {
            @Override public void run() {
                session.callback.onResult(text, session.request.getLanguage(), confidence, true);
            }
        });
    }

    private void fail(Session session, final EngineError error) {
        if (!detach(session)) { return; }
        cleanup(session, true);
        session.operation.complete(new Runnable() {
            @Override public void run() { session.callback.onFailure(error); }
        });
    }

    private boolean detach(Session session) {
        synchronized (lock) {
            if (activeSession != session) { return false; }
            activeSession = null;
            return true;
        }
    }

    private void cleanup(Session session, boolean cancel) {
        final SpeechRecognizerClient client;
        synchronized (lock) {
            if (session.cleaned) { return; }
            session.cleaned = true;
            client = session.client;
            session.client = null;
        }
        scheduler.cancel(session.watchdog);
        if (client != null) {
            try {
                if (cancel) {
                    try {
                        client.cancel();
                    } catch (RuntimeException failure) {
                        session.cleanupFailed = true;
                    }
                }
            } finally {
                try {
                    client.destroy();
                } catch (RuntimeException failure) {
                    session.cleanupFailed = true;
                }
            }
        }
    }

    private void destroyUnattached(SpeechRecognizerClient client) {
        try {
            client.destroy();
        } catch (RuntimeException failure) {
            // No callback has been admitted for an unattached client; disposal remains best effort.
            return;
        }
    }

    private Session currentFor(EngineOperation operation) {
        synchronized (lock) {
            return !closed && activeSession != null && activeSession.operation == operation && operation.isActive()
                    ? activeSession : null;
        }
    }

    private boolean isCurrent(Session session) {
        synchronized (lock) { return isCurrentLocked(session); }
    }

    private boolean isCurrentLocked(Session session) {
        return !closed && activeSession == session && session.operation.isActive();
    }

    private final class ClientListener implements SpeechRecognizerClient.Listener {
        private final Session session;
        private ClientListener(Session session) { this.session = session; }
        @Override public void onPartialResult(final String text, final float confidence) {
            scheduler.execute(new Runnable() { @Override public void run() { partial(session, text, confidence); } });
        }
        @Override public void onFinalResult(final String text, final float confidence) {
            scheduler.execute(new Runnable() { @Override public void run() { finalResult(session, text, confidence); } });
        }
        @Override public void onError(final int errorCode) {
            scheduler.execute(new Runnable() { @Override public void run() { fail(session, AndroidSpeechErrorMapper.map(errorCode)); } });
        }
    }

    private EngineOperation rejected(final RecognitionCallback callback, final EngineError error) {
        EngineOperation operation = new EngineOperation(null);
        final EngineOperation result = operation;
        scheduler.execute(new Runnable() {
            @Override public void run() {
                result.complete(new Runnable() { @Override public void run() { callback.onFailure(error); } });
            }
        });
        return operation;
    }

    private static EngineError closedError() { return new EngineError(EngineError.Category.CLOSED, new int[0], 0L, "Speech recognizer is closed"); }
    private static EngineError busyError() { return new EngineError(EngineError.Category.BUSY, new int[0], 0L, "Speech recognition is already active"); }
    private static EngineError unsupportedCaptureError() { return new EngineError(EngineError.Category.UNSUPPORTED, new int[0], 0L, "Android speech recognizer owns microphone capture"); }
    private static EngineError unavailableError() { return new EngineError(EngineError.Category.MISSING_DEPENDENCY, new int[0], 0L, "No speech recognition service is available"); }
    private static EngineError permissionError() { return new EngineError(EngineError.Category.PERMISSION, new int[0], 0L, "Microphone permission is unavailable"); }
    private static EngineError internalError() { return new EngineError(EngineError.Category.INTERNAL_FAILURE, new int[0], 0L, "Speech recognition failed"); }

    private final class Session {
        private final long generation;
        private final RecognitionRequest request;
        private final RecognitionCallback callback;
        private final EngineOperation operation;
        private final Runnable watchdog;
        private SpeechRecognizerClient client;
        private boolean listening;
        private boolean stopRequested;
        private boolean cleaned;
        private boolean cleanupFailed;

        private Session(long generation, RecognitionRequest request, RecognitionCallback callback, EngineOperation operation) {
            this.generation = generation;
            this.request = request;
            this.callback = callback;
            this.operation = operation;
            this.watchdog = new Runnable() { @Override public void run() { timeout(Session.this); } };
        }
    }
}
