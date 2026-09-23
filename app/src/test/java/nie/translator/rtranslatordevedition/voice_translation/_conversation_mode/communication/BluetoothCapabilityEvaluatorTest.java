package nie.translator.rtranslatordevedition.voice_translation._conversation_mode.communication;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BluetoothCapabilityEvaluatorTest {
    @Test
    public void permissionMissingWinsBeforeAnyBluetoothDecision() {
        assertEquals(BluetoothCapabilityEvaluator.Capability.PERMISSION_MISSING,
                BluetoothCapabilityEvaluator.evaluate(false, false, false,
                        BluetoothCapabilityEvaluator.SearchResult.FAILURE));
    }

    @Test
    public void unavailableAdapterOrDisabledBluetoothIsRecoverable() {
        assertEquals(BluetoothCapabilityEvaluator.Capability.BLUETOOTH_UNAVAILABLE,
                BluetoothCapabilityEvaluator.evaluate(true, false, false,
                        BluetoothCapabilityEvaluator.SearchResult.NOT_ATTEMPTED));
        assertEquals(BluetoothCapabilityEvaluator.Capability.BLUETOOTH_UNAVAILABLE,
                BluetoothCapabilityEvaluator.evaluate(true, true, false,
                        BluetoothCapabilityEvaluator.SearchResult.NOT_ATTEMPTED));
    }

    @Test
    public void legacyUnsupportedResultMeansDiscoveryLimitationNotNoBle() {
        assertEquals(BluetoothCapabilityEvaluator.Capability.DISCOVERY_UNSUPPORTED,
                BluetoothCapabilityEvaluator.evaluate(true, true, true,
                        BluetoothCapabilityEvaluator.SearchResult.DISCOVERY_UNSUPPORTED));
    }

    @Test
    public void genericLibraryFailureStaysDistinctFromCapabilityFailure() {
        assertEquals(BluetoothCapabilityEvaluator.Capability.LIBRARY_FAILURE,
                BluetoothCapabilityEvaluator.evaluate(true, true, true,
                        BluetoothCapabilityEvaluator.SearchResult.FAILURE));
    }

    @Test
    public void readyAndAlreadyStartedAreBothUsableNearbyStates() {
        assertEquals(BluetoothCapabilityEvaluator.Capability.READY,
                BluetoothCapabilityEvaluator.evaluate(true, true, true,
                        BluetoothCapabilityEvaluator.SearchResult.SUCCESS));
        assertEquals(BluetoothCapabilityEvaluator.Capability.READY,
                BluetoothCapabilityEvaluator.evaluate(true, true, true,
                        BluetoothCapabilityEvaluator.SearchResult.ALREADY_STARTED));
    }
}
