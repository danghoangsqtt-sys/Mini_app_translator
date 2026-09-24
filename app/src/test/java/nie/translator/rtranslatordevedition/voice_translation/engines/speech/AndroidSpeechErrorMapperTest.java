package nie.translator.rtranslatordevedition.voice_translation.engines.speech;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import android.speech.SpeechRecognizer;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineError;
import org.junit.Test;

public class AndroidSpeechErrorMapperTest {
    @Test public void mapsDocumentedCodesToStableSanitizedCategories() {
        assertCategory(SpeechRecognizer.ERROR_NETWORK, EngineError.Category.NETWORK);
        assertCategory(SpeechRecognizer.ERROR_NETWORK_TIMEOUT, EngineError.Category.NETWORK);
        assertCategory(SpeechRecognizer.ERROR_SERVER, EngineError.Category.NETWORK);
        assertCategory(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS, EngineError.Category.PERMISSION);
        assertCategory(SpeechRecognizer.ERROR_RECOGNIZER_BUSY, EngineError.Category.BUSY);
        assertCategory(SpeechRecognizer.ERROR_TOO_MANY_REQUESTS, EngineError.Category.BUSY);
        assertCategory(SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, EngineError.Category.UNSUPPORTED_LANGUAGE);
        assertCategory(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE, EngineError.Category.MISSING_DEPENDENCY);
        assertCategory(SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT, EngineError.Category.UNSUPPORTED);
        assertCategory(SpeechRecognizer.ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS, EngineError.Category.UNSUPPORTED);
        assertCategory(SpeechRecognizer.ERROR_AUDIO, EngineError.Category.INTERNAL_FAILURE);
        assertCategory(SpeechRecognizer.ERROR_CLIENT, EngineError.Category.INTERNAL_FAILURE);
        assertCategory(SpeechRecognizer.ERROR_NO_MATCH, EngineError.Category.INTERNAL_FAILURE);
        assertCategory(SpeechRecognizer.ERROR_SPEECH_TIMEOUT, EngineError.Category.INTERNAL_FAILURE);
        assertCategory(SpeechRecognizer.ERROR_SERVER_DISCONNECTED, EngineError.Category.INTERNAL_FAILURE);
        assertCategory(999, EngineError.Category.INTERNAL_FAILURE);
    }

    private static void assertCategory(int code, EngineError.Category expected) {
        EngineError error = AndroidSpeechErrorMapper.map(code);
        assertEquals(expected, error.getCategory());
        assertArrayEquals(new int[] { code }, error.getLegacyReasons());
    }
}
