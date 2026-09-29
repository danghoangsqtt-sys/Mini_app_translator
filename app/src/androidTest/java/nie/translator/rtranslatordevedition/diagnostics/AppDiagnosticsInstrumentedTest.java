package nie.translator.rtranslatordevedition.diagnostics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import androidx.test.InstrumentationRegistry;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

/** Runtime smoke for the local diagnostics boundary; it makes no physical-device claim. */
public class AppDiagnosticsInstrumentedTest {
    @Test
    public void initializedDiagnosticsWriteABoundedSanitizedReport() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();
        AppDiagnostics diagnostics = AppDiagnostics.getOrInitialize(context);
        AppDiagnostics.recordEvent(DiagnosticEvent.Mode.SETTINGS,
                DiagnosticEvent.Stage.EXPORT,
                DiagnosticEvent.Operation.EXPORT_DIAGNOSTICS,
                DiagnosticEvent.ErrorCategory.NONE);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        diagnostics.writeReport(output);
        byte[] bytes = output.toByteArray();
        String report = new String(bytes, StandardCharsets.UTF_8);

        assertTrue(bytes.length <= DiagnosticReport.MAX_REPORT_BYTES);
        assertTrue(report.contains("schema=mini_conversation_diagnostics_v1"));
        assertTrue(report.contains("api_level="));
        assertTrue(report.contains("permission_microphone="));
        assertTrue(DiagnosticReport.isPrivacySafe(report));
        assertFalse(report.contains("android_id"));
    }

    @Test
    public void exportIntentUsesStorageAccessFrameworkOnly() {
        Intent intent = AppDiagnostics.newExportIntent();
        assertEquals(Intent.ACTION_CREATE_DOCUMENT, intent.getAction());
        assertTrue(intent.hasCategory(Intent.CATEGORY_OPENABLE));
        assertEquals("text/plain", intent.getType());
        assertEquals(AppDiagnostics.EXPORT_FILE_NAME,
                intent.getStringExtra(Intent.EXTRA_TITLE));
    }
}
