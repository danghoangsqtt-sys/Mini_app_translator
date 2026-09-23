package nie.translator.rtranslatordevedition.voice_translation._conversation_mode.communication;

/**
 * Translates the nearby-device preconditions and the legacy communicator result into a
 * user-actionable state. This class deliberately has no Android or vendor-library dependency so
 * its decision table can be covered by local unit tests.
 */
public final class BluetoothCapabilityEvaluator {
    private BluetoothCapabilityEvaluator() {
    }

    public enum SearchResult {
        NOT_ATTEMPTED,
        SUCCESS,
        ALREADY_STARTED,
        DISCOVERY_UNSUPPORTED,
        FAILURE
    }

    public enum Capability {
        READY,
        PERMISSION_MISSING,
        BLUETOOTH_UNAVAILABLE,
        DISCOVERY_UNSUPPORTED,
        LIBRARY_FAILURE
    }

    public static Capability evaluate(boolean nearbyPermissionsGranted,
                                      boolean bluetoothAdapterAvailable,
                                      boolean bluetoothEnabled,
                                      SearchResult searchResult) {
        if (!nearbyPermissionsGranted) {
            return Capability.PERMISSION_MISSING;
        }
        if (!bluetoothAdapterAvailable || !bluetoothEnabled) {
            return Capability.BLUETOOTH_UNAVAILABLE;
        }
        if (searchResult == SearchResult.DISCOVERY_UNSUPPORTED) {
            return Capability.DISCOVERY_UNSUPPORTED;
        }
        if (searchResult == SearchResult.FAILURE) {
            return Capability.LIBRARY_FAILURE;
        }
        return Capability.READY;
    }
}
