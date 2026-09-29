package nie.translator.rtranslatordevedition.diagnostics;

import java.io.IOException;

/** A diagnostics event with no free-form payload. */
public final class DiagnosticEvent {
    public enum Mode {
        UNKNOWN("U"), PAIRING("P"), CONVERSATION("C"), WALKIE_TALKIE("W"),
        SETTINGS("S"), LEGACY_CLOUD("L");

        private final String code;

        Mode(String code) {
            this.code = code;
        }

        String code() {
            return code;
        }

        static Mode fromCode(String code) {
            for (Mode value : values()) {
                if (value.code.equals(code)) {
                    return value;
                }
            }
            return null;
        }

        public static Mode fromPersistedFragment(int fragment) {
            switch (fragment) {
                case 0:
                    return PAIRING;
                case 1:
                    return CONVERSATION;
                case 2:
                    return WALKIE_TALKIE;
                default:
                    return UNKNOWN;
            }
        }
    }

    public enum Stage {
        UNKNOWN("U"), PROCESS_CREATED("PC"), PROCESS_EXIT_READ("ER"),
        UI_ACTION("UI"), SERVICE_START_REQUESTED("SR"),
        FOREGROUND_PROMOTION("FP"), FOREGROUND_ACTIVE("FA"), IPC("IP"),
        EXPORT("EX"), SHUTDOWN("SD");

        private final String code;

        Stage(String code) {
            this.code = code;
        }

        String code() {
            return code;
        }

        static Stage fromCode(String code) {
            for (Stage value : values()) {
                if (value.code.equals(code)) {
                    return value;
                }
            }
            return null;
        }
    }

    public enum Operation {
        NONE("N"), APP_START("AS"), READ_EXIT_HISTORY("RH"),
        WRITE_PROCESS_SUMMARY("WS"), START_CONVERSATION("SC"),
        START_WALKIE_TALKIE("SW"), BIND_SERVICE("BS"),
        PROMOTE_SERVICE("PS"), SEND_SERVICE_COMMAND("SS"),
        SEND_CLIENT_CALLBACK("CC"), DISPATCH_LOCAL_UI("DU"),
        EXPORT_DIAGNOSTICS("ED");

        private final String code;

        Operation(String code) {
            this.code = code;
        }

        String code() {
            return code;
        }

        static Operation fromCode(String code) {
            for (Operation value : values()) {
                if (value.code.equals(code)) {
                    return value;
                }
            }
            return null;
        }
    }

    public enum ErrorCategory {
        NONE("N"), REMOTE_IPC("RI"), SECURITY("SE"), INVALID_STATE("IS"),
        IO("IO"), RUNTIME("RT"), PERMISSION("PE"),
        PLATFORM_UNAVAILABLE("PU"), UNKNOWN("U");

        private final String code;

        ErrorCategory(String code) {
            this.code = code;
        }

        String code() {
            return code;
        }

        static ErrorCategory fromCode(String code) {
            for (ErrorCategory value : values()) {
                if (value.code.equals(code)) {
                    return value;
                }
            }
            return null;
        }

        public static ErrorCategory fromThrowable(Throwable error) {
            if (error instanceof SecurityException) {
                return SECURITY;
            }
            if (error instanceof IllegalStateException) {
                return INVALID_STATE;
            }
            if (error instanceof IOException) {
                return IO;
            }
            if (error instanceof RuntimeException) {
                return RUNTIME;
            }
            return UNKNOWN;
        }
    }

    private final long sequence;
    private final Mode mode;
    private final Stage stage;
    private final Operation operation;
    private final ErrorCategory errorCategory;

    DiagnosticEvent(long sequence, Mode mode, Stage stage, Operation operation,
                    ErrorCategory errorCategory) {
        this.sequence = sequence;
        this.mode = mode;
        this.stage = stage;
        this.operation = operation;
        this.errorCategory = errorCategory;
    }

    public long getSequence() {
        return sequence;
    }

    public Mode getMode() {
        return mode;
    }

    public Stage getStage() {
        return stage;
    }

    public Operation getOperation() {
        return operation;
    }

    public ErrorCategory getErrorCategory() {
        return errorCategory;
    }
}
