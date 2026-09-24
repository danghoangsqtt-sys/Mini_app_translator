package nie.translator.rtranslatordevedition.voice_translation.engines;

/**
 * Thread-safe lifecycle gate for one engine request. Cancellation and terminal completion are
 * mutually exclusive, callbacks execute outside the internal lock, and cancellation is idempotent.
 * A successful dispatch is admitted before a concurrent successful cancel and may finish; after
 * cancel returns true, no later dispatch or terminal callback can be admitted. Cancellation does
 * not promise to interrupt transport work.
 */
public final class EngineOperation {
    public interface CancelAction {
        void onCancel();
    }

    private final Object lock = new Object();
    private final CancelAction cancelAction;
    private boolean cancelled;
    private boolean completed;

    public EngineOperation(CancelAction cancelAction) {
        this.cancelAction = cancelAction;
    }

    public boolean cancel() {
        synchronized (lock) {
            if (cancelled || completed) {
                return false;
            }
            cancelled = true;
        }
        if (cancelAction != null) {
            cancelAction.onCancel();
        }
        return true;
    }

    /**
     * Delivers a non-terminal callback only while this operation remains active. Admission is
     * linearized under the internal lock; the callback itself is intentionally invoked after it.
     */
    public boolean dispatch(Runnable callback) {
        if (callback == null) {
            throw new NullPointerException("callback");
        }
        synchronized (lock) {
            if (cancelled || completed) {
                return false;
            }
        }
        callback.run();
        return true;
    }

    /** Delivers one terminal callback. Completion is rejected after cancellation or completion. */
    public boolean complete(Runnable callback) {
        if (callback == null) {
            throw new NullPointerException("callback");
        }
        synchronized (lock) {
            if (cancelled || completed) {
                return false;
            }
            completed = true;
        }
        callback.run();
        return true;
    }

    public boolean isCancelled() {
        synchronized (lock) {
            return cancelled;
        }
    }

    public boolean isCompleted() {
        synchronized (lock) {
            return completed;
        }
    }

    public boolean isActive() {
        synchronized (lock) {
            return !cancelled && !completed;
        }
    }
}
