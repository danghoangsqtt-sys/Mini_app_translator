package nie.translator.rtranslatordevedition.voice_translation.engines.legacy;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import nie.translator.rtranslatordevedition.tools.ErrorCodes;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineError;
import org.junit.Test;

public class LegacyErrorMapperTest {
    @Test public void mapsEveryKnownLegacyCodeAndPreservesDiagnostics() {
        assertCategory(ErrorCodes.MISSED_CONNECTION, EngineError.Category.NETWORK);
        assertCategory(ErrorCodes.MISSED_CREDENTIALS, EngineError.Category.AUTHENTICATION);
        assertCategory(ErrorCodes.SAFETY_NET_EXCEPTION, EngineError.Category.AUTHENTICATION);
        assertCategory(ErrorCodes.MISSING_API_KEY, EngineError.Category.AUTHENTICATION);
        assertCategory(ErrorCodes.WRONG_API_KEY, EngineError.Category.AUTHENTICATION);
        assertCategory(ErrorCodes.MAX_CREDIT_OFFET_REACHED, EngineError.Category.QUOTA);
        assertCategory(ErrorCodes.MISSING_PLAY_SERVICES, EngineError.Category.MISSING_DEPENDENCY);
        assertCategory(ErrorCodes.MISSING_GOOGLE_TTS, EngineError.Category.MISSING_DEPENDENCY);
        assertCategory(ErrorCodes.ERROR, EngineError.Category.INTERNAL_FAILURE);
        assertCategory(ErrorCodes.MISSED_ARGUMENT, EngineError.Category.INTERNAL_FAILURE);
        assertCategory(ErrorCodes.GOOGLE_TTS_ERROR, EngineError.Category.INTERNAL_FAILURE);
    }

    @Test public void unknownAndEmptyReasonsBecomeInternalFailure() {
        assertEquals(EngineError.Category.INTERNAL_FAILURE, LegacyErrorMapper.map(new int[] { 9999 }, 3L).getCategory());
        assertEquals(EngineError.Category.INTERNAL_FAILURE, LegacyErrorMapper.map(new int[0], 3L).getCategory());
    }

    private static void assertCategory(int reason, EngineError.Category expected) {
        EngineError error = LegacyErrorMapper.map(new int[] { reason }, 42L);
        assertEquals(expected, error.getCategory());
        assertArrayEquals(new int[] { reason }, error.getLegacyReasons());
        assertEquals(42L, error.getLegacyValue());
    }
}
