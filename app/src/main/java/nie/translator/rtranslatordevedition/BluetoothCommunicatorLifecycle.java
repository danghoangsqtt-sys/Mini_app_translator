package nie.translator.rtranslatordevedition;

import androidx.annotation.Nullable;

/**
 * Owns the deferred lifetime of the Bluetooth communicator.  In particular, a failed or denied
 * permission request must never leave a partially constructed communicator in the application.
 */
public final class BluetoothCommunicatorLifecycle<T> {
    public interface PermissionGate {
        boolean isGranted();
    }

    public interface Factory<T> {
        T create();
    }

    public interface Destroyer<T> {
        void destroy(T value, Runnable onDestroyed);
    }

    private final PermissionGate permissionGate;
    private final Factory<T> factory;
    private final Destroyer<T> destroyer;
    @Nullable
    private T value;
    private boolean destroying;

    public BluetoothCommunicatorLifecycle(PermissionGate permissionGate, Factory<T> factory,
                                          Destroyer<T> destroyer) {
        this.permissionGate = permissionGate;
        this.factory = factory;
        this.destroyer = destroyer;
    }

    @Nullable
    public synchronized T getIfPermitted() {
        return permissionGate.isGranted() && !destroying ? value : null;
    }

    /** Creates the value only after the platform permission gate has succeeded. */
    @Nullable
    public synchronized T initializeIfPermitted() {
        if (!permissionGate.isGranted() || destroying) {
            return null;
        }
        if (value == null) {
            // Assignment happens only after construction succeeds, so no half-created value leaks.
            value = factory.create();
        }
        return value;
    }

    /**
     * A reset is serialized with initialization. If permission is revoked while cleanup is pending,
     * the callback leaves the value absent; a later grant can initialize it normally.
     */
    public void resetIfPermitted() {
        final T valueToDestroy;
        synchronized (this) {
            if (!permissionGate.isGranted() || value == null || destroying) {
                return;
            }
            destroying = true;
            valueToDestroy = value;
        }
        destroyer.destroy(valueToDestroy, new Runnable() {
            @Override
            public void run() {
                synchronized (BluetoothCommunicatorLifecycle.this) {
                    if (value == valueToDestroy) {
                        value = null;
                    }
                    destroying = false;
                }
                // Preserve the old reset contract, but only recreate after permission is still valid.
                initializeIfPermitted();
            }
        });
    }
}
