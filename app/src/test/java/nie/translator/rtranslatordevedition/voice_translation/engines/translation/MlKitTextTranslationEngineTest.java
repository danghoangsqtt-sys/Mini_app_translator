package nie.translator.rtranslatordevedition.voice_translation.engines.translation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import nie.translator.rtranslatordevedition.tools.CustomLocale;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineError;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineOperation;
import nie.translator.rtranslatordevedition.voice_translation.engines.TextTranslationEngine;
import org.junit.Test;

public class MlKitTextTranslationEngineTest {
    private static MlKitLanguageMapper mapper() {
        return new MlKitLanguageMapper(Arrays.asList("en", "it", "vi"));
    }

    @Test public void detectsChecksRequiredModelTranslatesAndClosesClients() {
        FakeRemoteModelClient models = new FakeRemoteModelClient();
        models.downloaded = true;
        FakeFactory factory = new FakeFactory();
        FakeIdentifier identifier = factory.enqueueIdentifier();
        FakeTranslator translator = factory.enqueueTranslator();
        MlKitTextTranslationEngine engine = new MlKitTextTranslationEngine(mapper(), models, factory);
        TranslationResult result = new TranslationResult();
        engine.translate("hello", new CustomLocale("it", "IT"), result);
        identifier.succeed("en-US");
        assertEquals(Arrays.asList("it"), models.checkedTags);
        assertEquals("en", factory.sourceTag);
        assertEquals("it", factory.targetTag);
        translator.succeed("ciao");
        assertEquals("ciao", result.text);
        assertEquals(1, identifier.closeCalls.get());
        assertEquals(1, translator.closeCalls.get());
    }

    @Test public void sourceEqualsTargetReturnsOriginalWithoutModelOrTranslator() {
        FakeRemoteModelClient models = new FakeRemoteModelClient();
        FakeFactory factory = new FakeFactory();
        FakeIdentifier identifier = factory.enqueueIdentifier();
        MlKitTextTranslationEngine engine = new MlKitTextTranslationEngine(mapper(), models, factory);
        TranslationResult result = new TranslationResult();
        engine.translate("ciao", new CustomLocale("it", "IT"), result);
        identifier.succeed("it");
        assertEquals("ciao", result.text);
        assertTrue(models.checkedTags.isEmpty());
        assertEquals(0, factory.translatorCreates);
        assertEquals(1, identifier.closeCalls.get());
    }

    @Test public void nonEnglishPairRequiresBothModels() {
        FakeRemoteModelClient models = new FakeRemoteModelClient();
        models.downloaded = true;
        FakeFactory factory = new FakeFactory();
        FakeIdentifier identifier = factory.enqueueIdentifier();
        factory.enqueueTranslator();
        MlKitTextTranslationEngine engine = new MlKitTextTranslationEngine(mapper(), models, factory);
        engine.translate("ciao", new CustomLocale("vi", "VN"), new TranslationResult());
        identifier.succeed("it-IT");
        assertEquals(Arrays.asList("it", "vi"), models.checkedTags);
    }

    @Test public void missingModelIsRecoverableAndDoesNotCreateTranslator() {
        FakeRemoteModelClient models = new FakeRemoteModelClient();
        models.downloaded = false;
        FakeFactory factory = new FakeFactory();
        FakeIdentifier identifier = factory.enqueueIdentifier();
        MlKitTextTranslationEngine engine = new MlKitTextTranslationEngine(mapper(), models, factory);
        TranslationResult result = new TranslationResult();
        engine.translate("hello", new CustomLocale("it", "IT"), result);
        identifier.succeed("en");
        assertEquals(EngineError.Category.MISSING_DEPENDENCY, result.error.getCategory());
        assertEquals(0, factory.translatorCreates);
    }

    @Test public void undAndUnsupportedTargetsReturnUnsupportedLanguage() {
        FakeRemoteModelClient models = new FakeRemoteModelClient();
        FakeFactory factory = new FakeFactory();
        FakeIdentifier identifier = factory.enqueueIdentifier();
        MlKitTextTranslationEngine engine = new MlKitTextTranslationEngine(mapper(), models, factory);
        TranslationResult detected = new TranslationResult();
        engine.translate("?", new CustomLocale("it", "IT"), detected);
        identifier.succeed("und");
        assertEquals(EngineError.Category.UNSUPPORTED_LANGUAGE, detected.error.getCategory());
        TranslationResult target = new TranslationResult();
        engine.translate("hello", new CustomLocale("xx", "ZZ"), target);
        assertEquals(EngineError.Category.UNSUPPORTED_LANGUAGE, target.error.getCategory());
    }

    @Test public void concurrentRequestIsRejectedBusy() {
        FakeFactory factory = new FakeFactory();
        factory.enqueueIdentifier();
        MlKitTextTranslationEngine engine = new MlKitTextTranslationEngine(mapper(), new FakeRemoteModelClient(), factory);
        engine.translate("one", new CustomLocale("it", "IT"), new TranslationResult());
        TranslationResult second = new TranslationResult();
        engine.translate("two", new CustomLocale("it", "IT"), second);
        assertEquals(EngineError.Category.BUSY, second.error.getCategory());
    }

    @Test public void cancelledOldSessionCannotCallbackIntoReplacement() {
        FakeRemoteModelClient models = new FakeRemoteModelClient();
        FakeFactory factory = new FakeFactory();
        FakeIdentifier oldIdentifier = factory.enqueueIdentifier();
        FakeIdentifier newIdentifier = factory.enqueueIdentifier();
        MlKitTextTranslationEngine engine = new MlKitTextTranslationEngine(mapper(), models, factory);
        TranslationResult oldResult = new TranslationResult();
        EngineOperation old = engine.translate("old", new CustomLocale("it", "IT"), oldResult);
        assertTrue(old.cancel());
        TranslationResult replacement = new TranslationResult();
        engine.translate("new", new CustomLocale("it", "IT"), replacement);
        oldIdentifier.succeed("it");
        assertEquals(0, oldResult.calls.get());
        assertEquals(0, replacement.calls.get());
        newIdentifier.succeed("it");
        assertEquals("new", replacement.text);
        assertEquals(1, oldIdentifier.closeCalls.get());
        assertEquals(1, newIdentifier.closeCalls.get());
    }

    @Test public void closeIsIdempotentAndSuppressesLateCallbacks() {
        FakeFactory factory = new FakeFactory();
        FakeIdentifier identifier = factory.enqueueIdentifier();
        MlKitTextTranslationEngine engine = new MlKitTextTranslationEngine(mapper(), new FakeRemoteModelClient(), factory);
        TranslationResult result = new TranslationResult();
        engine.translate("hello", new CustomLocale("it", "IT"), result);
        engine.close();
        engine.close();
        identifier.succeed("en");
        assertEquals(0, result.calls.get());
        assertEquals(1, identifier.closeCalls.get());
    }

    @Test public void explicitDetectionClosesIdentifierAndReturnsNormalizedLocale() {
        FakeFactory factory = new FakeFactory();
        FakeIdentifier identifier = factory.enqueueIdentifier();
        MlKitTextTranslationEngine engine = new MlKitTextTranslationEngine(mapper(), new FakeRemoteModelClient(), factory);
        DetectionResult result = new DetectionResult();
        engine.detectLanguage("hello", result);
        identifier.succeed("en-US");
        assertEquals("en", result.language.getLanguage());
        assertEquals(1, identifier.closeCalls.get());
    }

    @Test public void closedSupportedLanguagesCallbackRunsOutsideEngineLock() throws Exception {
        MlKitTextTranslationEngine engine = new MlKitTextTranslationEngine(mapper(),
                new FakeRemoteModelClient(), new FakeFactory());
        engine.close();
        final AtomicInteger failures = new AtomicInteger();
        engine.getSupportedLanguages(new CustomLocale("en", "US"),
                new TextTranslationEngine.SupportedLanguagesCallback() {
                    @Override public void onLanguages(java.util.List<CustomLocale> languages) {
                        throw new AssertionError("Closed engine must fail");
                    }

                    @Override public void onFailure(EngineError error) {
                        failures.incrementAndGet();
                        Thread reentrant = new Thread(new Runnable() {
                            @Override public void run() { engine.getCapability(); }
                        });
                        reentrant.start();
                        try {
                            reentrant.join(1000L);
                        } catch (InterruptedException interrupted) {
                            Thread.currentThread().interrupt();
                            throw new AssertionError(interrupted);
                        }
                        assertFalse("Callback must not hold the engine lock", reentrant.isAlive());
                    }
                });
        assertEquals(1, failures.get());
    }

    private static final class TranslationResult implements TextTranslationEngine.TranslationCallback {
        private final AtomicInteger calls = new AtomicInteger();
        private String text;
        private EngineError error;
        @Override public void onTranslated(String text, CustomLocale outputLanguage) { calls.incrementAndGet(); this.text = text; }
        @Override public void onFailure(EngineError error) { calls.incrementAndGet(); this.error = error; }
    }

    private static final class DetectionResult implements TextTranslationEngine.LanguageDetectionCallback {
        private CustomLocale language;
        @Override public void onDetected(CustomLocale language) { this.language = language; }
        @Override public void onFailure(EngineError error) { throw new AssertionError(error.getDetail()); }
    }

    private static final class FakeRemoteModelClient implements RemoteModelClient {
        private boolean downloaded;
        private final java.util.List<String> checkedTags = new java.util.ArrayList<>();
        @Override public void isModelDownloaded(String languageTag, AsyncResultCallback<Boolean> callback) {
            checkedTags.add(languageTag);
            callback.onSuccess(downloaded);
        }
        @Override public void getDownloadedModelTags(AsyncResultCallback<Set<String>> callback) { }
        @Override public void download(String languageTag, ModelDownloadPolicy policy, AsyncResultCallback<Void> callback) { }
        @Override public void delete(String languageTag, AsyncResultCallback<Void> callback) { }
    }

    private static final class FakeFactory implements TranslationClientFactory {
        private final Queue<FakeIdentifier> identifiers = new ArrayDeque<>();
        private final Queue<FakeTranslator> translators = new ArrayDeque<>();
        private int translatorCreates;
        private String sourceTag;
        private String targetTag;

        private FakeIdentifier enqueueIdentifier() { FakeIdentifier client = new FakeIdentifier(); identifiers.add(client); return client; }
        private FakeTranslator enqueueTranslator() { FakeTranslator client = new FakeTranslator(); translators.add(client); return client; }
        @Override public LanguageIdentifierClient createLanguageIdentifier() { return identifiers.remove(); }
        @Override public TranslatorClient createTranslator(String sourceLanguageTag, String targetLanguageTag) {
            translatorCreates++;
            sourceTag = sourceLanguageTag;
            targetTag = targetLanguageTag;
            return translators.remove();
        }
    }

    private static final class FakeIdentifier implements TranslationClientFactory.LanguageIdentifierClient {
        private final AtomicInteger closeCalls = new AtomicInteger();
        private AsyncResultCallback<String> callback;
        @Override public void identifyLanguage(String text, AsyncResultCallback<String> callback) { this.callback = callback; }
        @Override public void close() { closeCalls.incrementAndGet(); }
        private void succeed(String tag) { callback.onSuccess(tag); }
    }

    private static final class FakeTranslator implements TranslationClientFactory.TranslatorClient {
        private final AtomicInteger closeCalls = new AtomicInteger();
        private AsyncResultCallback<String> callback;
        @Override public void translate(String text, AsyncResultCallback<String> callback) { this.callback = callback; }
        @Override public void close() { closeCalls.incrementAndGet(); }
        private void succeed(String translated) { callback.onSuccess(translated); }
    }
}
