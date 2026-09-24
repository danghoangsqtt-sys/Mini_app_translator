package nie.translator.rtranslatordevedition.voice_translation.engines.translation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.google.mlkit.common.MlKitException;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineError;
import org.junit.Test;

public class MlKitErrorMapperTest {
    @Test public void mapsStableMlKitCategories() {
        assertCategory(MlKitException.NOT_FOUND, EngineError.Category.MISSING_DEPENDENCY);
        assertCategory(MlKitException.NETWORK_ISSUE, EngineError.Category.NETWORK);
        assertCategory(MlKitException.UNAVAILABLE, EngineError.Category.NETWORK);
        assertCategory(MlKitException.INVALID_ARGUMENT, EngineError.Category.UNSUPPORTED_LANGUAGE);
        assertCategory(MlKitException.UNSUPPORTED, EngineError.Category.UNSUPPORTED_LANGUAGE);
        assertCategory(MlKitException.CANCELLED, EngineError.Category.CANCELLED);
        assertCategory(MlKitException.RESOURCE_EXHAUSTED, EngineError.Category.QUOTA);
        assertCategory(MlKitException.NOT_ENOUGH_SPACE, EngineError.Category.QUOTA);
        assertCategory(MlKitException.ABORTED, EngineError.Category.BUSY);
        assertCategory(MlKitException.PERMISSION_DENIED, EngineError.Category.PERMISSION);
        assertCategory(MlKitException.UNKNOWN, EngineError.Category.INTERNAL_FAILURE);
    }

    @Test public void detailNeverContainsRawExceptionMessage() {
        EngineError error = MlKitErrorMapper.map(new IllegalStateException("private source text"));
        assertEquals(EngineError.Category.INTERNAL_FAILURE, error.getCategory());
        assertFalse(error.getDetail().contains("private source text"));
    }

    private static void assertCategory(int code, EngineError.Category expected) {
        assertEquals(expected, MlKitErrorMapper.fromErrorCode(code).getCategory());
    }
}
