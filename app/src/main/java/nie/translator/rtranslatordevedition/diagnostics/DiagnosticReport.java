package nie.translator.rtranslatordevedition.diagnostics;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Fixed-schema formatter for the user-triggered diagnostics export. */
public final class DiagnosticReport {
    public static final int MAX_REPORT_BYTES = 8 * 1024;
    private static final Pattern SAFE_VERSION = Pattern.compile("[A-Za-z0-9._+\\-]{1,32}");
    private static final String[] FORBIDDEN_MARKERS = {
            "recognized_text", "translated_text", "peer_name", "peer_address",
            "credential", "audio_data", "model_content", "exception_message",
            "stack_trace", "android_id", "device_serial"
    };

    public enum DeviceClass {
        UNKNOWN, PHONE, TABLET, LARGE_TABLET, LOW_RAM_PHONE, LOW_RAM_TABLET
    }

    public enum PermissionState {
        GRANTED, DENIED, NOT_REQUIRED, UNKNOWN
    }

    private DiagnosticReport() {
    }

    public static byte[] toBytes(Data data) {
        byte[] bytes = format(data).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_REPORT_BYTES) {
            throw new IllegalStateException("Diagnostics report exceeds fixed byte budget");
        }
        return bytes;
    }

    public static void write(Data data, OutputStream output) throws IOException {
        Objects.requireNonNull(output, "output");
        output.write(toBytes(data));
        output.flush();
    }

    public static String format(Data data) {
        Objects.requireNonNull(data, "data");
        StringBuilder report = new StringBuilder(2048);
        append(report, "schema", "mini_conversation_diagnostics_v1");
        append(report, "app_version", sanitizeVersion(data.versionName));
        append(report, "version_code", data.versionCode);
        append(report, "api_level", data.apiLevel);
        append(report, "device_class", data.deviceClass.name());
        append(report, "permission_microphone", data.microphone.name());
        append(report, "permission_bluetooth_scan", data.bluetoothScan.name());
        append(report, "permission_bluetooth_connect", data.bluetoothConnect.name());
        append(report, "permission_bluetooth_advertise", data.bluetoothAdvertise.name());
        append(report, "selected_mode", data.selectedMode.name());
        append(report, "last_exit_reason", data.processExit.getReason().name());
        append(report, "last_exit_status", data.processExit.getStatus());
        append(report, "last_exit_importance", data.processExit.getImportance());
        append(report, "last_exit_timestamp_ms", data.processExit.getTimestampMillis());

        DiagnosticState.CompactState previous = data.processExit.getPreviousState();
        append(report, "previous_state_present", previous != null);
        if (previous != null) {
            append(report, "previous_state_mode", previous.getMode().name());
            append(report, "previous_state_stage", previous.getStage().name());
            append(report, "previous_state_operation", previous.getOperation().name());
            append(report, "previous_state_error", previous.getErrorCategory().name());
            append(report, "previous_state_error_count", previous.getErrorCount());
            append(report, "previous_state_dropped_events", previous.getDroppedEventCount());
        }

        DiagnosticState.Snapshot snapshot = data.snapshot;
        append(report, "diagnostic_events_total", snapshot.getTotalEventCount());
        append(report, "diagnostic_errors_total", snapshot.getErrorCount());
        append(report, "diagnostic_events_dropped", snapshot.getDroppedEventCount());
        append(report, "diagnostic_events_retained", snapshot.getEvents().size());
        int index = 0;
        for (DiagnosticEvent event : snapshot.getEvents()) {
            append(report, "event_" + index,
                    event.getSequence() + "," + event.getMode().name() + ","
                            + event.getStage().name() + "," + event.getOperation().name()
                            + "," + event.getErrorCategory().name());
            index++;
        }

        String result = report.toString();
        if (!isPrivacySafe(result)) {
            throw new IllegalStateException("Diagnostics report failed privacy guard");
        }
        return result;
    }

    public static boolean isPrivacySafe(String report) {
        if (report == null) {
            return false;
        }
        String normalized = report.toLowerCase(Locale.US);
        for (String forbidden : FORBIDDEN_MARKERS) {
            if (normalized.contains(forbidden)) {
                return false;
            }
        }
        return true;
    }

    private static String sanitizeVersion(String value) {
        return value != null && SAFE_VERSION.matcher(value).matches() ? value : "unknown";
    }

    private static void append(StringBuilder output, String key, Object value) {
        output.append(key).append('=').append(value).append('\n');
    }

    public static final class Data {
        private final String versionName;
        private final long versionCode;
        private final int apiLevel;
        private final DeviceClass deviceClass;
        private final PermissionState microphone;
        private final PermissionState bluetoothScan;
        private final PermissionState bluetoothConnect;
        private final PermissionState bluetoothAdvertise;
        private final DiagnosticEvent.Mode selectedMode;
        private final DiagnosticState.Snapshot snapshot;
        private final ProcessExitRecord processExit;

        public Data(String versionName, long versionCode, int apiLevel,
                    DeviceClass deviceClass, PermissionState microphone,
                    PermissionState bluetoothScan, PermissionState bluetoothConnect,
                    PermissionState bluetoothAdvertise,
                    DiagnosticEvent.Mode selectedMode,
                    DiagnosticState.Snapshot snapshot,
                    ProcessExitRecord processExit) {
            this.versionName = versionName;
            this.versionCode = versionCode;
            this.apiLevel = apiLevel;
            this.deviceClass = Objects.requireNonNull(deviceClass, "deviceClass");
            this.microphone = Objects.requireNonNull(microphone, "microphone");
            this.bluetoothScan = Objects.requireNonNull(bluetoothScan, "bluetoothScan");
            this.bluetoothConnect = Objects.requireNonNull(bluetoothConnect, "bluetoothConnect");
            this.bluetoothAdvertise = Objects.requireNonNull(
                    bluetoothAdvertise, "bluetoothAdvertise");
            this.selectedMode = Objects.requireNonNull(selectedMode, "selectedMode");
            this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
            this.processExit = Objects.requireNonNull(processExit, "processExit");
        }
    }
}
