package nie.translator.rtranslatordevedition.diagnostics;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Thread-safe bounded state used by the in-process diagnostics boundary. */
public final class DiagnosticState {
    public static final int MAX_EVENTS = 16;
    public static final int MAX_PROCESS_SUMMARY_BYTES = 128;
    private static final String SUMMARY_VERSION = "D1";

    private final ArrayDeque<DiagnosticEvent> events = new ArrayDeque<>(MAX_EVENTS);
    private long sequence;
    private long errorCount;
    private long droppedEventCount;

    public synchronized void record(DiagnosticEvent.Mode mode, DiagnosticEvent.Stage stage,
                                    DiagnosticEvent.Operation operation,
                                    DiagnosticEvent.ErrorCategory errorCategory) {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(errorCategory, "errorCategory");
        sequence = incrementSaturated(sequence);
        if (errorCategory != DiagnosticEvent.ErrorCategory.NONE) {
            errorCount = incrementSaturated(errorCount);
        }
        if (events.size() == MAX_EVENTS) {
            events.removeFirst();
            droppedEventCount = incrementSaturated(droppedEventCount);
        }
        events.addLast(new DiagnosticEvent(sequence, mode, stage, operation, errorCategory));
    }

    public synchronized Snapshot snapshot() {
        return new Snapshot(new ArrayList<>(events), sequence, errorCount, droppedEventCount);
    }

    public synchronized byte[] encodeProcessSummary() {
        DiagnosticEvent latest = events.peekLast();
        DiagnosticEvent.Mode mode = latest == null ? DiagnosticEvent.Mode.UNKNOWN : latest.getMode();
        DiagnosticEvent.Stage stage = latest == null ? DiagnosticEvent.Stage.UNKNOWN : latest.getStage();
        DiagnosticEvent.Operation operation = latest == null
                ? DiagnosticEvent.Operation.NONE : latest.getOperation();
        DiagnosticEvent.ErrorCategory error = latest == null
                ? DiagnosticEvent.ErrorCategory.NONE : latest.getErrorCategory();
        String compact = SUMMARY_VERSION + ";" + mode.code() + ";" + stage.code() + ";"
                + operation.code() + ";" + error.code() + ";" + errorCount + ";"
                + droppedEventCount;
        byte[] encoded = compact.getBytes(StandardCharsets.UTF_8);
        if (encoded.length > MAX_PROCESS_SUMMARY_BYTES) {
            throw new IllegalStateException("Process summary exceeds fixed byte budget");
        }
        return encoded;
    }

    public static CompactState decodeProcessSummary(byte[] encoded) {
        if (encoded == null || encoded.length == 0
                || encoded.length > MAX_PROCESS_SUMMARY_BYTES) {
            return null;
        }
        for (byte value : encoded) {
            if (value < 0x20 || value > 0x7e) {
                return null;
            }
        }
        String[] fields = new String(encoded, StandardCharsets.UTF_8).split(";", -1);
        if (fields.length != 7 || !SUMMARY_VERSION.equals(fields[0])) {
            return null;
        }
        DiagnosticEvent.Mode mode = DiagnosticEvent.Mode.fromCode(fields[1]);
        DiagnosticEvent.Stage stage = DiagnosticEvent.Stage.fromCode(fields[2]);
        DiagnosticEvent.Operation operation = DiagnosticEvent.Operation.fromCode(fields[3]);
        DiagnosticEvent.ErrorCategory error = DiagnosticEvent.ErrorCategory.fromCode(fields[4]);
        if (mode == null || stage == null || operation == null || error == null) {
            return null;
        }
        try {
            long errors = parseNonNegative(fields[5]);
            long dropped = parseNonNegative(fields[6]);
            return new CompactState(mode, stage, operation, error, errors, dropped);
        } catch (NumberFormatException invalid) {
            return null;
        }
    }

    private static long parseNonNegative(String value) {
        long parsed = Long.parseLong(value);
        if (parsed < 0) {
            throw new NumberFormatException("negative");
        }
        return parsed;
    }

    private static long incrementSaturated(long value) {
        return value == Long.MAX_VALUE ? Long.MAX_VALUE : value + 1;
    }

    public static final class Snapshot {
        private final List<DiagnosticEvent> events;
        private final long totalEventCount;
        private final long errorCount;
        private final long droppedEventCount;

        private Snapshot(List<DiagnosticEvent> events, long totalEventCount,
                         long errorCount, long droppedEventCount) {
            this.events = Collections.unmodifiableList(events);
            this.totalEventCount = totalEventCount;
            this.errorCount = errorCount;
            this.droppedEventCount = droppedEventCount;
        }

        public List<DiagnosticEvent> getEvents() {
            return events;
        }

        public long getTotalEventCount() {
            return totalEventCount;
        }

        public long getErrorCount() {
            return errorCount;
        }

        public long getDroppedEventCount() {
            return droppedEventCount;
        }
    }

    public static final class CompactState {
        private final DiagnosticEvent.Mode mode;
        private final DiagnosticEvent.Stage stage;
        private final DiagnosticEvent.Operation operation;
        private final DiagnosticEvent.ErrorCategory errorCategory;
        private final long errorCount;
        private final long droppedEventCount;

        private CompactState(DiagnosticEvent.Mode mode, DiagnosticEvent.Stage stage,
                             DiagnosticEvent.Operation operation,
                             DiagnosticEvent.ErrorCategory errorCategory,
                             long errorCount, long droppedEventCount) {
            this.mode = mode;
            this.stage = stage;
            this.operation = operation;
            this.errorCategory = errorCategory;
            this.errorCount = errorCount;
            this.droppedEventCount = droppedEventCount;
        }

        public DiagnosticEvent.Mode getMode() {
            return mode;
        }

        public DiagnosticEvent.Stage getStage() {
            return stage;
        }

        public DiagnosticEvent.Operation getOperation() {
            return operation;
        }

        public DiagnosticEvent.ErrorCategory getErrorCategory() {
            return errorCategory;
        }

        public long getErrorCount() {
            return errorCount;
        }

        public long getDroppedEventCount() {
            return droppedEventCount;
        }
    }
}
