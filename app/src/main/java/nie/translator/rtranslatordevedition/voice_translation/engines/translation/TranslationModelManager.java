package nie.translator.rtranslatordevedition.voice_translation.engines.translation;

import com.google.mlkit.nl.translate.TranslateLanguage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineError;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineOperation;

/**
 * Lifecycle-safe state machine for explicitly managed ML Kit translation models. Cancellation
 * suppresses callbacks; the underlying Google Task is not transport-cancellable.
 */
public final class TranslationModelManager {
    public interface ModelsCallback {
        void onModels(List<TranslationModel> models);
        void onFailure(EngineError error);
    }

    public interface OperationCallback {
        void onSuccess(TranslationModel model);
        void onFailure(EngineError error);
    }

    public interface Observer {
        void onModelChanged(TranslationModel model);
    }

    private enum OperationKind { DOWNLOAD, DELETE }

    private final Object lock = new Object();
    private final MlKitLanguageMapper mapper;
    private final RemoteModelClient client;
    private final Map<String, TranslationModel> states = new LinkedHashMap<>();
    private final Map<String, ActiveOperation> activeByLanguage = new HashMap<>();
    private final Set<EngineOperation> pendingOperations = new HashSet<>();
    private long nextIdentity;
    private boolean closed;
    private Observer observer;

    public TranslationModelManager() {
        this(new MlKitLanguageMapper(), new MlKitRemoteModelClient());
    }

    TranslationModelManager(MlKitLanguageMapper mapper, RemoteModelClient client) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.client = Objects.requireNonNull(client, "client");
        for (String tag : mapper.getSupportedTags()) {
            states.put(tag, model(tag, initialState(tag), null));
        }
    }

    public void setObserver(Observer observer) {
        synchronized (lock) {
            if (!closed) {
                this.observer = observer;
            }
        }
    }

    public void clearObserver() {
        synchronized (lock) {
            observer = null;
        }
    }

    public List<TranslationModel> getSnapshot() {
        synchronized (lock) {
            return immutableSnapshotLocked();
        }
    }

    public EngineOperation refresh(final ModelsCallback callback) {
        Objects.requireNonNull(callback, "callback");
        final EngineOperation operation = newTrackedOperation();
        if (!registerPending(operation)) {
            fail(operation, callback, MlKitErrorMapper.closed());
            return operation;
        }
        try {
            client.getDownloadedModelTags(new AsyncResultCallback<Set<String>>() {
                @Override public void onSuccess(Set<String> downloadedTags) {
                    final List<TranslationModel> snapshot;
                    synchronized (lock) {
                        if (!canCompletePendingLocked(operation)) { return; }
                        Set<String> normalizedDownloaded = normalizeDownloaded(downloadedTags);
                        for (String tag : mapper.getSupportedTags()) {
                            if (activeByLanguage.containsKey(tag)) { continue; }
                            TranslationModelState state = TranslateLanguage.ENGLISH.equals(tag)
                                    ? TranslationModelState.BUILT_IN
                                    : normalizedDownloaded.contains(tag)
                                    ? TranslationModelState.DOWNLOADED
                                    : TranslationModelState.NOT_DOWNLOADED;
                            states.put(tag, model(tag, state, null));
                        }
                        pendingOperations.remove(operation);
                        snapshot = immutableSnapshotLocked();
                    }
                    operation.complete(new Runnable() {
                        @Override public void run() { callback.onModels(snapshot); }
                    });
                }

                @Override public void onFailure(Exception error) {
                    completeFailure(operation, callback, MlKitErrorMapper.map(error));
                }
            });
        } catch (RuntimeException error) {
            completeFailure(operation, callback, MlKitErrorMapper.map(error));
        }
        return operation;
    }

    public EngineOperation download(String requestedTag, ModelDownloadPolicy policy, final OperationCallback callback) {
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(callback, "callback");
        final String tag;
        try {
            tag = mapper.toSupportedTag(requestedTag);
        } catch (IllegalArgumentException error) {
            return rejected(callback, MlKitErrorMapper.unsupportedLanguage());
        }
        if (TranslateLanguage.ENGLISH.equals(tag)) {
            return rejected(callback, new EngineError(EngineError.Category.UNSUPPORTED, new int[0], 0L,
                    "English is built in and cannot be downloaded"));
        }
        final ActiveOperation active = createActive(tag, OperationKind.DOWNLOAD, callback);
        if (active == null) {
            return rejected(callback, rejectionFor(tag));
        }
        publish(active, TranslationModelState.QUEUED, null);
        publish(active, TranslationModelState.DOWNLOADING, null);
        if (!isCurrent(active)) {
            return active.operation;
        }
        try {
            client.download(tag, policy, new AsyncResultCallback<Void>() {
                @Override public void onSuccess(Void ignored) {
                    completeActiveSuccess(active, TranslationModelState.DOWNLOADED);
                }

                @Override public void onFailure(Exception error) {
                    completeActiveFailure(active, MlKitErrorMapper.map(error));
                }
            });
        } catch (RuntimeException error) {
            completeActiveFailure(active, MlKitErrorMapper.map(error));
        }
        return active.operation;
    }

    public EngineOperation delete(String requestedTag, final OperationCallback callback) {
        Objects.requireNonNull(callback, "callback");
        final String tag;
        try {
            tag = mapper.toSupportedTag(requestedTag);
        } catch (IllegalArgumentException error) {
            return rejected(callback, MlKitErrorMapper.unsupportedLanguage());
        }
        if (TranslateLanguage.ENGLISH.equals(tag)) {
            return rejected(callback, new EngineError(EngineError.Category.UNSUPPORTED, new int[0], 0L,
                    "English is built in and cannot be deleted"));
        }
        final ActiveOperation active = createActive(tag, OperationKind.DELETE, callback);
        if (active == null) {
            return rejected(callback, rejectionFor(tag));
        }
        publish(active, TranslationModelState.DELETING, null);
        if (!isCurrent(active)) {
            return active.operation;
        }
        try {
            client.delete(tag, new AsyncResultCallback<Void>() {
                @Override public void onSuccess(Void ignored) {
                    completeActiveSuccess(active, TranslationModelState.NOT_DOWNLOADED);
                }

                @Override public void onFailure(Exception error) {
                    completeActiveFailure(active, MlKitErrorMapper.map(error));
                }
            });
        } catch (RuntimeException error) {
            completeActiveFailure(active, MlKitErrorMapper.map(error));
        }
        return active.operation;
    }

    /** Cancels the active operation for a model, if there is one. */
    public boolean cancel(String requestedTag) {
        final String tag;
        try {
            tag = mapper.toSupportedTag(requestedTag);
        } catch (IllegalArgumentException error) {
            return false;
        }
        final EngineOperation operation;
        synchronized (lock) {
            ActiveOperation active = activeByLanguage.get(tag);
            operation = active == null ? null : active.operation;
        }
        return operation != null && operation.cancel();
    }

    public void close() {
        final List<EngineOperation> operations;
        synchronized (lock) {
            if (closed) { return; }
            closed = true;
            observer = null;
            operations = new ArrayList<>(pendingOperations);
            pendingOperations.clear();
            for (ActiveOperation active : activeByLanguage.values()) {
                operations.add(active.operation);
            }
            activeByLanguage.clear();
        }
        for (EngineOperation operation : operations) {
            operation.cancel();
        }
    }

    private EngineOperation newTrackedOperation() {
        final EngineOperation[] reference = new EngineOperation[1];
        EngineOperation operation = new EngineOperation(new EngineOperation.CancelAction() {
            @Override public void onCancel() {
                synchronized (lock) {
                    pendingOperations.remove(reference[0]);
                }
            }
        });
        reference[0] = operation;
        return operation;
    }

    private boolean registerPending(EngineOperation operation) {
        synchronized (lock) {
            if (closed) { return false; }
            pendingOperations.add(operation);
            return true;
        }
    }

    private ActiveOperation createActive(String tag, OperationKind kind, OperationCallback callback) {
        synchronized (lock) {
            if (closed || activeByLanguage.containsKey(tag)) { return null; }
            TranslationModel previous = states.get(tag);
            final long identity = ++nextIdentity;
            final ActiveOperation[] reference = new ActiveOperation[1];
            EngineOperation operation = new EngineOperation(new EngineOperation.CancelAction() {
                @Override public void onCancel() { cancelActive(reference[0]); }
            });
            ActiveOperation active = new ActiveOperation(identity, tag, kind, operation, callback, previous);
            reference[0] = active;
            activeByLanguage.put(tag, active);
            return active;
        }
    }

    private EngineError rejectionFor(String tag) {
        synchronized (lock) {
            return closed ? MlKitErrorMapper.closed() : new EngineError(EngineError.Category.BUSY,
                    new int[0], 0L, "A model operation is already active for " + tag);
        }
    }

    private void cancelActive(ActiveOperation active) {
        if (active == null) { return; }
        final TranslationModel restored;
        final Observer currentObserver;
        synchronized (lock) {
            ActiveOperation current = activeByLanguage.get(active.languageTag);
            if (closed || current == null || current.identity != active.identity
                    || current.kind != active.kind) { return; }
            activeByLanguage.remove(active.languageTag);
            if (active.previous != null) {
                states.put(active.languageTag, active.previous);
            }
            restored = states.get(active.languageTag);
            currentObserver = observer;
        }
        if (currentObserver != null && restored != null) {
            notifyObserver(currentObserver, restored);
        }
    }

    private void publish(final ActiveOperation active, TranslationModelState state, EngineError error) {
        final TranslationModel snapshot;
        final Observer currentObserver;
        synchronized (lock) {
            if (!isCurrentLocked(active)) { return; }
            snapshot = model(active.languageTag, state, error);
            states.put(active.languageTag, snapshot);
            currentObserver = observer;
        }
        if (currentObserver != null) {
            active.operation.dispatch(new Runnable() {
                @Override public void run() { notifyObserver(currentObserver, snapshot); }
            });
        }
    }

    private void completeActiveSuccess(final ActiveOperation active, TranslationModelState state) {
        final TranslationModel snapshot;
        final Observer currentObserver;
        synchronized (lock) {
            if (!isCurrentLocked(active)) { return; }
            activeByLanguage.remove(active.languageTag);
            snapshot = model(active.languageTag, state, null);
            states.put(active.languageTag, snapshot);
            currentObserver = observer;
        }
        active.operation.complete(new Runnable() {
            @Override public void run() {
                if (currentObserver != null) { notifyObserver(currentObserver, snapshot); }
                active.callback.onSuccess(snapshot);
            }
        });
    }

    private void completeActiveFailure(final ActiveOperation active, final EngineError error) {
        final TranslationModel snapshot;
        final Observer currentObserver;
        synchronized (lock) {
            if (!isCurrentLocked(active)) { return; }
            activeByLanguage.remove(active.languageTag);
            snapshot = model(active.languageTag, TranslationModelState.FAILED, error);
            states.put(active.languageTag, snapshot);
            currentObserver = observer;
        }
        active.operation.complete(new Runnable() {
            @Override public void run() {
                if (currentObserver != null) { notifyObserver(currentObserver, snapshot); }
                active.callback.onFailure(error);
            }
        });
    }

    private boolean isCurrentLocked(ActiveOperation active) {
        ActiveOperation current = activeByLanguage.get(active.languageTag);
        return !closed && current != null && current.identity == active.identity
                && current.kind == active.kind && active.operation.isActive();
    }

    private boolean isCurrent(ActiveOperation active) {
        synchronized (lock) {
            return isCurrentLocked(active);
        }
    }

    /** Keeps all app callbacks outside the manager lock and ignores a detached observer. */
    private void notifyObserver(final Observer expectedObserver, final TranslationModel snapshot) {
        synchronized (lock) {
            if (closed || observer != expectedObserver) { return; }
        }
        expectedObserver.onModelChanged(snapshot);
    }

    private boolean canCompletePendingLocked(EngineOperation operation) {
        return !closed && pendingOperations.contains(operation) && operation.isActive();
    }

    private Set<String> normalizeDownloaded(Set<String> downloadedTags) {
        Set<String> normalized = new HashSet<>();
        if (downloadedTags != null) {
            for (String tag : downloadedTags) {
                try {
                    normalized.add(mapper.toSupportedTag(tag));
                } catch (IllegalArgumentException ignored) {
                    // Ignore models that are not part of this SDK's advertised language set.
                }
            }
        }
        return normalized;
    }

    private List<TranslationModel> immutableSnapshotLocked() {
        return Collections.unmodifiableList(new ArrayList<>(states.values()));
    }

    private void completeFailure(final EngineOperation operation, final ModelsCallback callback, final EngineError error) {
        synchronized (lock) {
            if (!pendingOperations.remove(operation)) { return; }
        }
        operation.complete(new Runnable() {
            @Override public void run() { callback.onFailure(error); }
        });
    }

    private static EngineOperation rejected(final OperationCallback callback, final EngineError error) {
        EngineOperation operation = new EngineOperation(null);
        operation.complete(new Runnable() {
            @Override public void run() { callback.onFailure(error); }
        });
        return operation;
    }

    private static void fail(EngineOperation operation, final ModelsCallback callback, final EngineError error) {
        operation.complete(new Runnable() {
            @Override public void run() { callback.onFailure(error); }
        });
    }

    private static TranslationModelState initialState(String tag) {
        return TranslateLanguage.ENGLISH.equals(tag)
                ? TranslationModelState.BUILT_IN : TranslationModelState.NOT_DOWNLOADED;
    }

    private static TranslationModel model(String tag, TranslationModelState state, EngineError error) {
        return new TranslationModel(tag, state, error);
    }

    private static final class ActiveOperation {
        private final long identity;
        private final String languageTag;
        private final OperationKind kind;
        private final EngineOperation operation;
        private final OperationCallback callback;
        private final TranslationModel previous;

        private ActiveOperation(long identity, String languageTag, OperationKind kind,
                                EngineOperation operation, OperationCallback callback,
                                TranslationModel previous) {
            this.identity = identity;
            this.languageTag = languageTag;
            this.kind = kind;
            this.operation = operation;
            this.callback = callback;
            this.previous = previous;
        }
    }
}
