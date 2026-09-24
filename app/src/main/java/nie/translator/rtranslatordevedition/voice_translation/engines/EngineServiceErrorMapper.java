package nie.translator.rtranslatordevedition.voice_translation.engines;

import nie.translator.rtranslatordevedition.tools.ErrorCodes;
import nie.translator.rtranslatordevedition.voice_translation.VoiceTranslationService;

/** Maps sanitized engine categories to stable, recoverable service error codes. */
public final class EngineServiceErrorMapper {
    public enum OperationKind { SPEECH_RECOGNITION, TRANSLATION, LANGUAGE_DETECTION }
    private EngineServiceErrorMapper() { }
    public static int map(EngineError error) {
        return map(error, OperationKind.SPEECH_RECOGNITION);
    }
    public static int map(EngineError error, OperationKind kind) {
        switch (error.getCategory()) {
            case PERMISSION: return VoiceTranslationService.MISSING_MIC_PERMISSION;
            case MISSING_DEPENDENCY: return kind == OperationKind.TRANSLATION
                    ? ErrorCodes.ON_DEVICE_MODEL_MISSING : ErrorCodes.ON_DEVICE_UNAVAILABLE;
            case UNSUPPORTED_LANGUAGE: return ErrorCodes.ON_DEVICE_UNSUPPORTED_LANGUAGE;
            case BUSY: return ErrorCodes.ON_DEVICE_BUSY;
            case NETWORK: return ErrorCodes.ON_DEVICE_NETWORK;
            case UNSUPPORTED: return ErrorCodes.ON_DEVICE_UNAVAILABLE;
            default: return ErrorCodes.ON_DEVICE_FAILURE;
        }
    }
    public static boolean isSilent(EngineError error) {
        return error.getCategory() == EngineError.Category.CANCELLED || error.getCategory() == EngineError.Category.CLOSED;
    }
}
