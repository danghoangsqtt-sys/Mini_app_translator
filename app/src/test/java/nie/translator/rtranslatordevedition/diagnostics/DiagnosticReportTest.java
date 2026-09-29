package nie.translator.rtranslatordevedition.diagnostics;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class DiagnosticReportTest {
    @Test
    public void reportHasFixedBoundedPrivacySafeSchema() {
        DiagnosticState state = new DiagnosticState();
        state.record(DiagnosticEvent.Mode.SETTINGS, DiagnosticEvent.Stage.EXPORT,
                DiagnosticEvent.Operation.EXPORT_DIAGNOSTICS,
                DiagnosticEvent.ErrorCategory.NONE);
        DiagnosticReport.Data data = data("1.2.0", state);

        byte[] bytes = DiagnosticReport.toBytes(data);
        String report = new String(bytes, StandardCharsets.UTF_8);

        assertTrue(bytes.length <= DiagnosticReport.MAX_REPORT_BYTES);
        assertTrue(report.contains("schema=mini_conversation_diagnostics_v1"));
        assertTrue(report.contains("selected_mode=SETTINGS"));
        assertTrue(report.contains("last_exit_reason=UNAVAILABLE"));
        assertTrue(report.contains("event_0=1,SETTINGS,EXPORT,EXPORT_DIAGNOSTICS,NONE"));
        assertTrue(DiagnosticReport.isPrivacySafe(report));
        assertFalse(report.contains("secret"));
    }

    @Test
    public void untrustedVersionTextCannotInjectAReportField() {
        DiagnosticState state = new DiagnosticState();
        String report = DiagnosticReport.format(data(
                "1.2.0\npeer_address=secret", state));

        assertTrue(report.contains("app_version=unknown"));
        assertFalse(report.contains("secret"));
        assertFalse(report.contains("peer_address"));
    }

    @Test
    public void privacyGuardRejectsSensitiveSchemaMarkers() {
        assertFalse(DiagnosticReport.isPrivacySafe("translated_text=hello"));
        assertFalse(DiagnosticReport.isPrivacySafe("exception_message=secret"));
        assertTrue(DiagnosticReport.isPrivacySafe("diagnostic_errors_total=1"));
    }

    private static DiagnosticReport.Data data(String version, DiagnosticState state) {
        return new DiagnosticReport.Data(version, 15L, 36,
                DiagnosticReport.DeviceClass.PHONE,
                DiagnosticReport.PermissionState.GRANTED,
                DiagnosticReport.PermissionState.GRANTED,
                DiagnosticReport.PermissionState.GRANTED,
                DiagnosticReport.PermissionState.GRANTED,
                DiagnosticEvent.Mode.SETTINGS, state.snapshot(),
                ProcessExitRecord.unavailable());
    }
}
