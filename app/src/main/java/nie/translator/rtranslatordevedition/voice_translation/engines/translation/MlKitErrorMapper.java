package nie.translator.rtranslatordevedition.voice_translation.engines.translation;

import com.google.mlkit.common.MlKitException;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineError;

/** Maps ML Kit failures to stable categories without exposing exception or user-content details. */
public final class MlKitErrorMapper {
    private MlKitErrorMapper() { }

    public static EngineError map(Exception error) {
        if (error instanceof MlKitException) {
            return fromErrorCode(((MlKitException) error).getErrorCode());
        }
        return error(EngineError.Category.INTERNAL_FAILURE, "On-device translation failed");
    }

    static EngineError fromErrorCode(int errorCode) {
        switch (errorCode) {
            case MlKitException.NOT_FOUND:
                return error(EngineError.Category.MISSING_DEPENDENCY, "Translation model is not downloaded");
            case MlKitException.NETWORK_ISSUE:
            case MlKitException.UNAVAILABLE:
            case MlKitException.DEADLINE_EXCEEDED:
                return error(EngineError.Category.NETWORK, "ML Kit network operation failed");
            case MlKitException.INVALID_ARGUMENT:
            case MlKitException.UNIMPLEMENTED:
            case MlKitException.UNSUPPORTED:
                return error(EngineError.Category.UNSUPPORTED_LANGUAGE, "Language is not supported");
            case MlKitException.CANCELLED:
                return error(EngineError.Category.CANCELLED, "Operation was cancelled");
            case MlKitException.RESOURCE_EXHAUSTED:
            case MlKitException.NOT_ENOUGH_SPACE:
                return error(EngineError.Category.QUOTA, "Device resources are unavailable");
            case MlKitException.ABORTED:
            case MlKitException.ALREADY_EXISTS:
                return error(EngineError.Category.BUSY, "ML Kit operation is busy");
            case MlKitException.PERMISSION_DENIED:
                return error(EngineError.Category.PERMISSION, "ML Kit permission was denied");
            default:
                return error(EngineError.Category.INTERNAL_FAILURE, "On-device translation failed");
        }
    }

    static EngineError unsupportedLanguage() {
        return error(EngineError.Category.UNSUPPORTED_LANGUAGE, "Language is not supported");
    }

    static EngineError missingModel() {
        return error(EngineError.Category.MISSING_DEPENDENCY, "Translation model is not downloaded");
    }

    static EngineError closed() {
        return error(EngineError.Category.CLOSED, "Translation component is closed");
    }

    static EngineError busy() {
        return error(EngineError.Category.BUSY, "Translation operation is already active");
    }

    private static EngineError error(EngineError.Category category, String detail) {
        return new EngineError(category, new int[0], 0L, detail);
    }
}
