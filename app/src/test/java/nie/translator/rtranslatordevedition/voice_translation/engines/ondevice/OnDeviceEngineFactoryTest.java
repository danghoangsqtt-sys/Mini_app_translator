package nie.translator.rtranslatordevedition.voice_translation.engines.ondevice;

import static org.junit.Assert.assertSame;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineFactory;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineFactoryRegistry;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineType;
import nie.translator.rtranslatordevedition.voice_translation.engines.SpeechOutputEngine;
import nie.translator.rtranslatordevedition.voice_translation.engines.SpeechRecognitionEngine;
import nie.translator.rtranslatordevedition.voice_translation.engines.TextTranslationEngine;
import org.junit.Test;

public class OnDeviceEngineFactoryTest {
    @Test public void registrySelectsOnlyTheExplicitOnDeviceFactoryForTheDefaultLane() {
        EngineFactoryRegistry registry = new EngineFactoryRegistry();
        EngineFactory onDevice = new EngineFactory() {
            @Override public EngineType getType() { return EngineType.ON_DEVICE; }
            @Override public TextTranslationEngine createTextTranslationEngine() { return null; }
            @Override public SpeechRecognitionEngine createSpeechRecognitionEngine() { return null; }
            @Override public SpeechOutputEngine createSpeechOutputEngine() { return null; }
        };
        registry.register(onDevice);
        assertSame(onDevice, registry.get(EngineType.ON_DEVICE));
    }
}
