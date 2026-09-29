package nie.translator.rtranslatordevedition.diagnostics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.app.ApplicationExitInfo;
import org.junit.Test;

public class ProcessExitRecordTest {
    @Test
    public void requiredPlatformReasonsMapToStableCategories() {
        assertEquals(ProcessExitRecord.Reason.CRASH,
                ProcessExitRecord.mapReason(ApplicationExitInfo.REASON_CRASH));
        assertEquals(ProcessExitRecord.Reason.CRASH,
                ProcessExitRecord.mapReason(ApplicationExitInfo.REASON_CRASH_NATIVE));
        assertEquals(ProcessExitRecord.Reason.ANR,
                ProcessExitRecord.mapReason(ApplicationExitInfo.REASON_ANR));
        assertEquals(ProcessExitRecord.Reason.LOW_MEMORY,
                ProcessExitRecord.mapReason(ApplicationExitInfo.REASON_LOW_MEMORY));
        assertEquals(ProcessExitRecord.Reason.PERMISSION_CHANGE,
                ProcessExitRecord.mapReason(ApplicationExitInfo.REASON_PERMISSION_CHANGE));
        assertEquals(ProcessExitRecord.Reason.USER_STOP,
                ProcessExitRecord.mapReason(ApplicationExitInfo.REASON_USER_REQUESTED));
        assertEquals(ProcessExitRecord.Reason.USER_STOP,
                ProcessExitRecord.mapReason(ApplicationExitInfo.REASON_USER_STOPPED));
        assertEquals(ProcessExitRecord.Reason.UNKNOWN,
                ProcessExitRecord.mapReason(ApplicationExitInfo.REASON_UNKNOWN));
        assertEquals(ProcessExitRecord.Reason.UNKNOWN,
                ProcessExitRecord.mapReason(Integer.MAX_VALUE));
    }

    @Test
    public void platformRecordKeepsOnlyStableFieldsAndValidatedState() {
        DiagnosticState state = new DiagnosticState();
        state.record(DiagnosticEvent.Mode.CONVERSATION,
                DiagnosticEvent.Stage.FOREGROUND_ACTIVE,
                DiagnosticEvent.Operation.PROMOTE_SERVICE,
                DiagnosticEvent.ErrorCategory.NONE);

        ProcessExitRecord record = ProcessExitRecord.fromPlatform(
                ApplicationExitInfo.REASON_ANR, 7, 100, 1234L,
                state.encodeProcessSummary());

        assertEquals(ProcessExitRecord.Reason.ANR, record.getReason());
        assertEquals(7, record.getStatus());
        assertEquals(100, record.getImportance());
        assertEquals(1234L, record.getTimestampMillis());
        assertNotNull(record.getPreviousState());
        assertEquals(DiagnosticEvent.Mode.CONVERSATION,
                record.getPreviousState().getMode());
    }
}
