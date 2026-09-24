package nie.translator.rtranslatordevedition.voice_translation.engines.ondevice;

import android.content.Context;
import java.util.Objects;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineFactory;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineType;
import nie.translator.rtranslatordevedition.voice_translation.engines.SpeechOutputEngine;
import nie.translator.rtranslatordevedition.voice_translation.engines.SpeechRecognitionEngine;
import nie.translator.rtranslatordevedition.voice_translation.engines.TextTranslationEngine;
import nie.translator.rtranslatordevedition.voice_translation.engines.speech.AndroidSpeechRecognitionEngine;
import nie.translator.rtranslatordevedition.voice_translation.engines.translation.MlKitTextTranslationEngine;

/** Explicit service-scoped default composition; it never touches Cloud or credentials. */
public final class OnDeviceEngineFactory implements EngineFactory {
    private final Context context;
    private final SpeechOutputEngine output;

    public OnDeviceEngineFactory(Context context, SpeechOutputEngine output) {
        this.context = Objects.requireNonNull(context, "context").getApplicationContext();
        this.output = Objects.requireNonNull(output, "output");
    }

    @Override public EngineType getType() { return EngineType.ON_DEVICE; }
    @Override public TextTranslationEngine createTextTranslationEngine() { return new MlKitTextTranslationEngine(); }
    @Override public SpeechRecognitionEngine createSpeechRecognitionEngine() { return new AndroidSpeechRecognitionEngine(context); }
    @Override public SpeechOutputEngine createSpeechOutputEngine() { return output; }
}
