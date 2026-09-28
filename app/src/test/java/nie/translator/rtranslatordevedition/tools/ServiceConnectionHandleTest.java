package nie.translator.rtranslatordevedition.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import nie.translator.rtranslatordevedition.tools.services_communication.ServiceCallback;
import nie.translator.rtranslatordevedition.tools.services_communication.ServiceCommunicator;
import nie.translator.rtranslatordevedition.tools.services_communication.ServiceCommunicatorListener;
import org.junit.Test;

public class ServiceConnectionHandleTest {
    @Test public void cancelBeforeStartSuppressesLateFailureAndRunsCleanupOnce() {
        Probe probe = new Probe();
        ServiceConnectionHandle handle = handle(probe);

        handle.cancel();
        handle.cancel();
        handle.fail(new int[]{ErrorCodes.ERROR}, -1L);

        assertTrue(handle.isCancelled());
        assertEquals(1, probe.cancellations);
        assertFalse(probe.startedAtCancellation);
        assertNull(probe.connectionAtCancellation);
        assertEquals(0, probe.failures);
    }

    @Test public void cancelAfterLaunchReportsStartedRequestForOrphanCleanup() {
        Probe probe = new Probe();
        ServiceConnectionHandle handle = handle(probe);

        assertTrue(handle.markServiceStarted());
        handle.cancel();

        assertEquals(1, probe.cancellations);
        assertTrue(probe.startedAtCancellation);
    }

    @Test public void attachedConnectionIsOwnedByExactHandle() {
        Probe probe = new Probe();
        ServiceConnectionHandle handle = handle(probe);
        CustomServiceConnection connection = new CustomServiceConnection(new FakeCommunicator());

        assertTrue(handle.attach(connection));
        handle.cancel();

        assertSame(connection, probe.connectionAtCancellation);
        assertFalse(handle.attach(new CustomServiceConnection(new FakeCommunicator())));
    }

    @Test public void terminalFailureIsDeliveredOnceAndPreventsCancellation() {
        Probe probe = new Probe();
        ServiceConnectionHandle handle = handle(probe);

        handle.fail(new int[]{ErrorCodes.ERROR}, 7L);
        handle.fail(new int[]{ErrorCodes.ERROR}, 8L);
        handle.cancel();

        assertEquals(1, probe.failures);
        assertEquals(7L, probe.failureValue);
        assertEquals(0, probe.cancellations);
    }

    @Test public void cancellationWinningBeforeAttachNeverTransfersCallbacks() {
        Probe probe = new Probe();
        ServiceConnectionHandle handle = handle(probe);
        handle.cancel();

        CustomServiceConnection connection = new CustomServiceConnection(new FakeCommunicator());
        assertFalse(handle.attach(connection));
        connection.reportBindFailure();

        assertEquals(0, probe.failures);
    }

    private static ServiceConnectionHandle handle(Probe probe) {
        ServiceCommunicatorListener response = new ServiceCommunicatorListener() {
            @Override public void onServiceCommunicator(ServiceCommunicator serviceCommunicator) { }

            @Override public void onFailure(int[] reasons, long value) {
                probe.failures++;
                probe.failureValue = value;
            }
        };
        return new ServiceConnectionHandle(new ServiceCallback() { }, response, probe);
    }

    private static final class Probe implements ServiceConnectionHandle.CancellationListener {
        int failures;
        long failureValue;
        int cancellations;
        boolean startedAtCancellation;
        CustomServiceConnection connectionAtCancellation;

        @Override public void onCancelled(ServiceConnectionHandle handle,
                                          CustomServiceConnection connection,
                                          boolean serviceStarted) {
            cancellations++;
            connectionAtCancellation = connection;
            startedAtCancellation = serviceStarted;
        }
    }

    private static final class FakeCommunicator extends ServiceCommunicator {
        FakeCommunicator() { super(1); }
        @Override public void addCallback(ServiceCallback callback) { }
        @Override public int removeCallback(ServiceCallback callback) { return 0; }
    }
}
