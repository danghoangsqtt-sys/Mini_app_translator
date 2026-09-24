package nie.translator.rtranslatordevedition.voice_translation.engines.legacy;

import android.app.Service;
import java.util.Objects;
import nie.translator.rtranslatordevedition.tools.TTS;
import nie.translator.rtranslatordevedition.voice_translation.cloud_apis.translation.Translator;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineFactory;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineType;
import nie.translator.rtranslatordevedition.voice_translation.engines.SpeechOutputEngine;
import nie.translator.rtranslatordevedition.voice_translation.engines.SpeechRecognitionEngine;
import nie.translator.rtranslatordevedition.voice_translation.engines.TextTranslationEngine;

/** Explicit factory for adapters over pre-existing legacy dependencies. */
public final class LegacyCloudEngineFactory implements EngineFactory {
    private final Translator translator;
    private final Service recognitionService;
    private final TTS tts;

    public LegacyCloudEngineFactory(Translator translator, Service recognitionService, TTS tts) {
        this.translator = Objects.requireNonNull(translator, "translator");
        this.recognitionService = Objects.requireNonNull(recognitionService, "recognitionService");
        this.tts = Objects.requireNonNull(tts, "tts");
    }

    @Override public EngineType getType() { return EngineType.LEGACY_CLOUD; }
    @Override public TextTranslationEngine createTextTranslationEngine() { return new LegacyCloudTextTranslationEngine(translator); }
    @Override public SpeechRecognitionEngine createSpeechRecognitionEngine() { return new LegacyCloudSpeechRecognitionEngine(recognitionService); }
    @Override public SpeechOutputEngine createSpeechOutputEngine() { return new AndroidSpeechOutputEngine(tts, recognitionService); }
}
