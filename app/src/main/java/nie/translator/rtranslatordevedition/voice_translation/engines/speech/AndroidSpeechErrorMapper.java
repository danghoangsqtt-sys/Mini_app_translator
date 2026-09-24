package nie.translator.rtranslatordevedition.voice_translation.engines.speech;

import android.speech.SpeechRecognizer;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineError;

/** Sanitizes platform failures; recognizer error messages and content never leave this boundary. */
final class AndroidSpeechErrorMapper {
    private AndroidSpeechErrorMapper() { }

    static EngineError map(int code) {
        final EngineError.Category category;
        final String detail;
        switch (code) {
            case SpeechRecognizer.ERROR_NETWORK:
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
            case SpeechRecognizer.ERROR_SERVER:
                category = EngineError.Category.NETWORK;
                detail = "Speech recognition network failure";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                category = EngineError.Category.PERMISSION;
                detail = "Microphone permission is unavailable";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
            case SpeechRecognizer.ERROR_TOO_MANY_REQUESTS:
                category = EngineError.Category.BUSY;
                detail = "Speech recognizer is busy";
                break;
            case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED:
                category = EngineError.Category.UNSUPPORTED_LANGUAGE;
                detail = "Recognition language is not supported";
                break;
            case SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE:
                category = EngineError.Category.MISSING_DEPENDENCY;
                detail = "Recognition language data is unavailable";
                break;
            case SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT:
            case SpeechRecognizer.ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS:
                category = EngineError.Category.UNSUPPORTED;
                detail = "Recognition support operation is unavailable";
                break;
            default:
                category = EngineError.Category.INTERNAL_FAILURE;
                detail = "Speech recognition failed";
                break;
        }
        return new EngineError(category, new int[] { code }, 0L, detail);
    }

    static EngineError timeout() {
        return new EngineError(EngineError.Category.INTERNAL_FAILURE, new int[] { SpeechRecognizer.ERROR_SPEECH_TIMEOUT },
                0L, "Speech recognition timed out");
    }

    static EngineError emptyResult() {
        return new EngineError(EngineError.Category.INTERNAL_FAILURE, new int[] { SpeechRecognizer.ERROR_NO_MATCH },
                0L, "Speech recognition returned no result");
    }
}
