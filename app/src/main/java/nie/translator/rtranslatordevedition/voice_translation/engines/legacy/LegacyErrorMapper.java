package nie.translator.rtranslatordevedition.voice_translation.engines.legacy;

import nie.translator.rtranslatordevedition.tools.ErrorCodes;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineError;

/** Converts legacy reason codes to stable public categories while preserving their diagnostics. */
public final class LegacyErrorMapper {
    private LegacyErrorMapper() { }

    public static EngineError map(int[] reasons, long value) {
        EngineError.Category category = EngineError.Category.INTERNAL_FAILURE;
        if (reasons != null) {
            for (int reason : reasons) {
                category = categoryFor(reason);
                if (category != EngineError.Category.INTERNAL_FAILURE) {
                    break;
                }
            }
        }
        return new EngineError(category, reasons, value, "Legacy engine failure");
    }

    private static EngineError.Category categoryFor(int reason) {
        switch (reason) {
            case ErrorCodes.MISSED_CONNECTION:
                return EngineError.Category.NETWORK;
            case ErrorCodes.MISSED_CREDENTIALS:
            case ErrorCodes.SAFETY_NET_EXCEPTION:
            case ErrorCodes.MISSING_API_KEY:
            case ErrorCodes.WRONG_API_KEY:
                return EngineError.Category.AUTHENTICATION;
            case ErrorCodes.MAX_CREDIT_OFFET_REACHED:
                return EngineError.Category.QUOTA;
            case ErrorCodes.MISSING_PLAY_SERVICES:
            case ErrorCodes.MISSING_GOOGLE_TTS:
                return EngineError.Category.MISSING_DEPENDENCY;
            default:
                return EngineError.Category.INTERNAL_FAILURE;
        }
    }
}
