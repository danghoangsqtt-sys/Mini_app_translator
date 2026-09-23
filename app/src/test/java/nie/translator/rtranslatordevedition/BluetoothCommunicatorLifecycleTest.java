package nie.translator.rtranslatordevedition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

public class BluetoothCommunicatorLifecycleTest {
    @Test
    public void freshInstallAndDeniedPermissionNeverCreateCommunicator() {
        AtomicBoolean granted = new AtomicBoolean(false);
        AtomicInteger creations = new AtomicInteger();
        BluetoothCommunicatorLifecycle<Object> lifecycle = newLifecycle(granted, creations,
                new RecordingDestroyer<Object>());

        assertNull(lifecycle.getIfPermitted());
        assertNull(lifecycle.initializeIfPermitted());
        assertNull(lifecycle.initializeIfPermitted());
        assertEquals(0, creations.get());
    }

    @Test
    public void permissionGrantCreatesExactlyOneCommunicatorAndRevokeDoesNotExposeIt() {
        AtomicBoolean granted = new AtomicBoolean(false);
        AtomicInteger creations = new AtomicInteger();
        BluetoothCommunicatorLifecycle<Object> lifecycle = newLifecycle(granted, creations,
                new RecordingDestroyer<Object>());

        granted.set(true);
        Object created = lifecycle.initializeIfPermitted();
        assertSame(created, lifecycle.initializeIfPermitted());
        assertEquals(1, creations.get());

        granted.set(false);
        assertNull(lifecycle.getIfPermitted());
        assertNull(lifecycle.initializeIfPermitted());

        granted.set(true);
        assertSame(created, lifecycle.initializeIfPermitted());
        assertEquals(1, creations.get());
    }

    @Test
    public void resetWaitsForCleanupBeforeAllowingRetry() {
        AtomicBoolean granted = new AtomicBoolean(true);
        AtomicInteger creations = new AtomicInteger();
        RecordingDestroyer<Object> destroyer = new RecordingDestroyer<>();
        BluetoothCommunicatorLifecycle<Object> lifecycle = newLifecycle(granted, creations, destroyer);
        Object first = lifecycle.initializeIfPermitted();

        lifecycle.resetIfPermitted();
        lifecycle.resetIfPermitted();
        assertEquals(1, destroyer.destroyCalls);
        assertNull(lifecycle.getIfPermitted());
        assertNull(lifecycle.initializeIfPermitted());

        destroyer.complete();
        Object replacement = lifecycle.getIfPermitted();
        assertEquals(2, creations.get());
        assertSame(replacement, lifecycle.getIfPermitted());
    }

    @Test
    public void failedConstructionDoesNotExposePartialCommunicatorAndCanRetry() {
        final AtomicBoolean granted = new AtomicBoolean(true);
        final AtomicInteger attempts = new AtomicInteger();
        BluetoothCommunicatorLifecycle<Object> lifecycle = new BluetoothCommunicatorLifecycle<>(
                new BluetoothCommunicatorLifecycle.PermissionGate() {
                    @Override
                    public boolean isGranted() {
                        return granted.get();
                    }
                }, new BluetoothCommunicatorLifecycle.Factory<Object>() {
                    @Override
                    public Object create() {
                        if (attempts.incrementAndGet() == 1) {
                            throw new IllegalStateException("construction failed");
                        }
                        return new Object();
                    }
                }, new RecordingDestroyer<Object>());

        try {
            lifecycle.initializeIfPermitted();
        } catch (IllegalStateException expected) {
            // The platform exception is intentionally not hidden; the lifecycle remains retryable.
        }
        assertNull(lifecycle.getIfPermitted());
        assertNotNull(lifecycle.initializeIfPermitted());
        assertEquals(2, attempts.get());
    }

    private BluetoothCommunicatorLifecycle<Object> newLifecycle(final AtomicBoolean granted,
                                                                 final AtomicInteger creations,
                                                                 RecordingDestroyer<Object> destroyer) {
        return new BluetoothCommunicatorLifecycle<>(new BluetoothCommunicatorLifecycle.PermissionGate() {
            @Override
            public boolean isGranted() {
                return granted.get();
            }
        }, new BluetoothCommunicatorLifecycle.Factory<Object>() {
            @Override
            public Object create() {
                creations.incrementAndGet();
                return new Object();
            }
        }, destroyer);
    }

    private static final class RecordingDestroyer<T> implements BluetoothCommunicatorLifecycle.Destroyer<T> {
        private int destroyCalls;
        private Runnable onDestroyed;

        @Override
        public void destroy(T value, Runnable onDestroyed) {
            destroyCalls++;
            this.onDestroyed = onDestroyed;
        }

        private void complete() {
            onDestroyed.run();
        }
    }
}
