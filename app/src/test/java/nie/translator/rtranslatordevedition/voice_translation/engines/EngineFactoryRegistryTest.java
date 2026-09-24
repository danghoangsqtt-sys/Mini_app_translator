package nie.translator.rtranslatordevedition.voice_translation.engines;

import static org.junit.Assert.assertSame;

import org.junit.Test;

public class EngineFactoryRegistryTest {
    @Test public void explicitLookupReturnsRegisteredFactory() {
        EngineFactoryRegistry registry = new EngineFactoryRegistry();
        EngineFactory factory = new FakeFactory(EngineType.ON_DEVICE);
        registry.register(factory);
        assertSame(factory, registry.get(EngineType.ON_DEVICE));
    }

    @Test(expected = IllegalArgumentException.class) public void duplicateTypeIsRejected() {
        EngineFactoryRegistry registry = new EngineFactoryRegistry();
        registry.register(new FakeFactory(EngineType.LEGACY_CLOUD));
        registry.register(new FakeFactory(EngineType.LEGACY_CLOUD));
    }

    @Test(expected = IllegalStateException.class) public void missingTypeFailsDeterministically() {
        new EngineFactoryRegistry().get(EngineType.ON_DEVICE);
    }

    private static final class FakeFactory implements EngineFactory {
        private final EngineType type;
        FakeFactory(EngineType type) { this.type = type; }
        @Override public EngineType getType() { return type; }
        @Override public TextTranslationEngine createTextTranslationEngine() { return null; }
        @Override public SpeechRecognitionEngine createSpeechRecognitionEngine() { return null; }
        @Override public SpeechOutputEngine createSpeechOutputEngine() { return null; }
    }
}
