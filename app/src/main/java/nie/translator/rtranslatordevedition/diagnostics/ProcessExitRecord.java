package nie.translator.rtranslatordevedition.diagnostics;

import android.app.ApplicationExitInfo;

/** Sanitized subset of ApplicationExitInfo. */
public final class ProcessExitRecord {
    public enum Reason {
        UNAVAILABLE,
        NO_HISTORY,
        CRASH,
        ANR,
        LOW_MEMORY,
        PERMISSION_CHANGE,
        USER_STOP,
        SELF_EXIT,
        DEPENDENCY_DIED,
        RESOURCE_LIMIT,
        INITIALIZATION_FAILURE,
        PACKAGE_CHANGE,
        SIGNALLED,
        FROZEN,
        UNKNOWN
    }

    private final Reason reason;
    private final int status;
    private final int importance;
    private final long timestampMillis;
    private final DiagnosticState.CompactState previousState;

    private ProcessExitRecord(Reason reason, int status, int importance,
                              long timestampMillis,
                              DiagnosticState.CompactState previousState) {
        this.reason = reason;
        this.status = status;
        this.importance = importance;
        this.timestampMillis = timestampMillis;
        this.previousState = previousState;
    }

    public static ProcessExitRecord unavailable() {
        return new ProcessExitRecord(Reason.UNAVAILABLE, -1, -1, 0L, null);
    }

    public static ProcessExitRecord noHistory() {
        return new ProcessExitRecord(Reason.NO_HISTORY, -1, -1, 0L, null);
    }

    static ProcessExitRecord fromPlatform(int reason, int status, int importance,
                                          long timestampMillis, byte[] processStateSummary) {
        return new ProcessExitRecord(mapReason(reason), status, importance,
                Math.max(0L, timestampMillis),
                DiagnosticState.decodeProcessSummary(processStateSummary));
    }

    public static Reason mapReason(int reason) {
        switch (reason) {
            case ApplicationExitInfo.REASON_CRASH:
            case ApplicationExitInfo.REASON_CRASH_NATIVE:
                return Reason.CRASH;
            case ApplicationExitInfo.REASON_ANR:
                return Reason.ANR;
            case ApplicationExitInfo.REASON_LOW_MEMORY:
                return Reason.LOW_MEMORY;
            case ApplicationExitInfo.REASON_PERMISSION_CHANGE:
                return Reason.PERMISSION_CHANGE;
            case ApplicationExitInfo.REASON_USER_REQUESTED:
            case ApplicationExitInfo.REASON_USER_STOPPED:
                return Reason.USER_STOP;
            case ApplicationExitInfo.REASON_EXIT_SELF:
                return Reason.SELF_EXIT;
            case ApplicationExitInfo.REASON_DEPENDENCY_DIED:
                return Reason.DEPENDENCY_DIED;
            case ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE:
                return Reason.RESOURCE_LIMIT;
            case ApplicationExitInfo.REASON_INITIALIZATION_FAILURE:
                return Reason.INITIALIZATION_FAILURE;
            case ApplicationExitInfo.REASON_PACKAGE_STATE_CHANGE:
            case ApplicationExitInfo.REASON_PACKAGE_UPDATED:
                return Reason.PACKAGE_CHANGE;
            case ApplicationExitInfo.REASON_SIGNALED:
                return Reason.SIGNALLED;
            case ApplicationExitInfo.REASON_FREEZER:
                return Reason.FROZEN;
            case ApplicationExitInfo.REASON_UNKNOWN:
            case ApplicationExitInfo.REASON_OTHER:
            default:
                return Reason.UNKNOWN;
        }
    }

    public Reason getReason() {
        return reason;
    }

    public int getStatus() {
        return status;
    }

    public int getImportance() {
        return importance;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    public DiagnosticState.CompactState getPreviousState() {
        return previousState;
    }
}
