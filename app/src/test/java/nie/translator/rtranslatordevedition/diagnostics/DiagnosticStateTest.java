package nie.translator.rtranslatordevedition.diagnostics;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class DiagnosticStateTest {
    @Test
    public void stateRetainsOnlyTheNewestBoundedEvents() {
        DiagnosticState state = new DiagnosticState();
        for (int index = 0; index < 20; index++) {
            state.record(DiagnosticEvent.Mode.CONVERSATION,
                    DiagnosticEvent.Stage.IPC,
                    DiagnosticEvent.Operation.SEND_CLIENT_CALLBACK,
                    index % 2 == 0 ? DiagnosticEvent.ErrorCategory.NONE
                            : DiagnosticEvent.ErrorCategory.REMOTE_IPC);
        }

        DiagnosticState.Snapshot snapshot = state.snapshot();
        assertEquals(20L, snapshot.getTotalEventCount());
        assertEquals(10L, snapshot.getErrorCount());
        assertEquals(4L, snapshot.getDroppedEventCount());
        assertEquals(DiagnosticState.MAX_EVENTS, snapshot.getEvents().size());
        assertEquals(5L, snapshot.getEvents().get(0).getSequence());
        assertEquals(20L, snapshot.getEvents().get(15).getSequence());
    }

    @Test
    public void compactSummaryRoundTripsWithin128Bytes() {
        DiagnosticState state = new DiagnosticState();
        state.record(DiagnosticEvent.Mode.WALKIE_TALKIE,
                DiagnosticEvent.Stage.FOREGROUND_PROMOTION,
                DiagnosticEvent.Operation.PROMOTE_SERVICE,
                DiagnosticEvent.ErrorCategory.SECURITY);

        byte[] encoded = state.encodeProcessSummary();
        assertTrue(encoded.length <= DiagnosticState.MAX_PROCESS_SUMMARY_BYTES);
        DiagnosticState.CompactState decoded = DiagnosticState.decodeProcessSummary(encoded);
        assertNotNull(decoded);
        assertEquals(DiagnosticEvent.Mode.WALKIE_TALKIE, decoded.getMode());
        assertEquals(DiagnosticEvent.Stage.FOREGROUND_PROMOTION, decoded.getStage());
        assertEquals(DiagnosticEvent.Operation.PROMOTE_SERVICE, decoded.getOperation());
        assertEquals(DiagnosticEvent.ErrorCategory.SECURITY, decoded.getErrorCategory());
        assertEquals(1L, decoded.getErrorCount());
        assertEquals(0L, decoded.getDroppedEventCount());
        assertArrayEquals(encoded, new String(encoded, StandardCharsets.UTF_8)
                .getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void compactSummaryRejectsOversizedMalformedAndFreeFormData() {
        assertNull(DiagnosticState.decodeProcessSummary(new byte[129]));
        assertNull(DiagnosticState.decodeProcessSummary(
                "D1;C;IP;CC;RI;1;0;unexpected".getBytes(StandardCharsets.UTF_8)));
        assertNull(DiagnosticState.decodeProcessSummary(
                "D1;conversation text;IP;CC;RI;1;0".getBytes(StandardCharsets.UTF_8)));
        assertNull(DiagnosticState.decodeProcessSummary(
                "D1;C;IP;CC;RI;-1;0".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void throwableClassificationNeverNeedsMessageText() {
        assertEquals(DiagnosticEvent.ErrorCategory.SECURITY,
                DiagnosticEvent.ErrorCategory.fromThrowable(new SecurityException("secret")));
        assertEquals(DiagnosticEvent.ErrorCategory.INVALID_STATE,
                DiagnosticEvent.ErrorCategory.fromThrowable(new IllegalStateException("secret")));
        assertEquals(DiagnosticEvent.ErrorCategory.IO,
                DiagnosticEvent.ErrorCategory.fromThrowable(new java.io.IOException("secret")));
        assertEquals(DiagnosticEvent.ErrorCategory.RUNTIME,
                DiagnosticEvent.ErrorCategory.fromThrowable(new RuntimeException("secret")));
    }
}
