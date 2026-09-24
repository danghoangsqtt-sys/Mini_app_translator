package nie.translator.rtranslatordevedition.voice_translation.engines;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import nie.translator.rtranslatordevedition.tools.ErrorCodes;
import org.junit.Test;

public class EngineServiceErrorMapperTest {
    @Test public void mapsRecoverableOnDeviceFailuresWithoutDetails() {
        assertEquals(ErrorCodes.ON_DEVICE_MODEL_MISSING, EngineServiceErrorMapper.map(error(EngineError.Category.MISSING_DEPENDENCY), EngineServiceErrorMapper.OperationKind.TRANSLATION));
        assertEquals(ErrorCodes.ON_DEVICE_UNAVAILABLE, EngineServiceErrorMapper.map(error(EngineError.Category.MISSING_DEPENDENCY), EngineServiceErrorMapper.OperationKind.SPEECH_RECOGNITION));
        assertEquals(ErrorCodes.ON_DEVICE_UNSUPPORTED_LANGUAGE, EngineServiceErrorMapper.map(error(EngineError.Category.UNSUPPORTED_LANGUAGE)));
        assertEquals(ErrorCodes.ON_DEVICE_BUSY, EngineServiceErrorMapper.map(error(EngineError.Category.BUSY)));
        assertTrue(EngineServiceErrorMapper.isSilent(error(EngineError.Category.CANCELLED)));
    }
    private static EngineError error(EngineError.Category category) { return new EngineError(category, new int[0], 0L, "safe"); }
}
