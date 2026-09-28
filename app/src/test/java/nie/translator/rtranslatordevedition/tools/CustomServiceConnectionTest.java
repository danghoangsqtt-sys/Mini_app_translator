package nie.translator.rtranslatordevedition.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import nie.translator.rtranslatordevedition.tools.services_communication.ServiceCallback;
import nie.translator.rtranslatordevedition.tools.services_communication.ServiceCommunicator;
import nie.translator.rtranslatordevedition.tools.services_communication.ServiceCommunicatorListener;

public class CustomServiceConnectionTest {
    @Test public void successfulRegistrationUnbindsExactlyOnce() {
        CustomServiceConnection connection = connection(null);
        final int[] unbinds = {0};
        assertTrue(connection.markRegistered());
        assertFalse(connection.markRegistered());
        connection.disconnect(value -> unbinds[0]++);
        connection.disconnect(value -> unbinds[0]++);
        assertEquals(1, unbinds[0]);
        assertFalse(connection.isRegistered());
    }

    @Test public void falseBindNeverAttemptsUnbindAndReportsOnce() {
        final int[] failures = {0};
        CustomServiceConnection connection = connection(new ServiceCommunicatorListener() {
            @Override public void onServiceCommunicator(ServiceCommunicator communicator) { }
            @Override public void onFailure(int[] reasons, long value) {
                failures[0]++;
                assertEquals(ErrorCodes.ERROR, reasons[0]);
            }
        });
        final int[] unbinds = {0};
        connection.reportBindFailure();
        connection.reportBindFailure();
        connection.disconnect(value -> unbinds[0]++);
        assertEquals(1, failures[0]);
        assertEquals(0, unbinds[0]);
    }

    @Test public void platformAlreadyUnregisteredRaceIsContained() {
        CustomServiceConnection connection = connection(null);
        assertTrue(connection.markRegistered());
        connection.disconnect(value -> { throw new IllegalArgumentException("not registered"); });
        assertFalse(connection.isRegistered());
    }

    @Test public void terminalBindingFailureReleasesRegisteredConnectionOnce() {
        CustomServiceConnection connection = connection(null);
        final int[] terminals = {0};
        final int[] unbinds = {0};
        assertTrue(connection.markRegistered());
        connection.setTerminalListener(() -> {
            terminals[0]++;
            connection.disconnect(value -> unbinds[0]++);
        });
        connection.onNullBinding(null);
        connection.onBindingDied(null);
        assertEquals(1, terminals[0]);
        assertEquals(1, unbinds[0]);
        assertFalse(connection.isRegistered());
    }

    @Test public void releaseBeforeLateTerminalCallbackSuppressesOwnerFailure() {
        final int[] failures = {0};
        CustomServiceConnection connection = connection(new ServiceCommunicatorListener() {
            @Override public void onServiceCommunicator(ServiceCommunicator communicator) { }
            @Override public void onFailure(int[] reasons, long value) { failures[0]++; }
        });

        connection.disconnect(value -> { });
        connection.reportBindFailure();

        assertEquals(0, failures[0]);
        assertFalse(connection.hasEverConnected());
    }

    private static CustomServiceConnection connection(ServiceCommunicatorListener listener) {
        CustomServiceConnection connection = new CustomServiceConnection(new FakeCommunicator());
        connection.addCallbacks(new ServiceCallback() { }, listener);
        return connection;
    }

    private static final class FakeCommunicator extends ServiceCommunicator {
        FakeCommunicator() { super(1); }
        @Override public void addCallback(ServiceCallback callback) { }
        @Override public int removeCallback(ServiceCallback callback) { return 0; }
    }
}
