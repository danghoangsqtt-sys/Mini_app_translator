package nie.translator.rtranslatordevedition.voice_translation.engines.legacy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import nie.translator.rtranslatordevedition.tools.CustomLocale;
import nie.translator.rtranslatordevedition.voice_translation.cloud_apis.CloudApiResult;
import nie.translator.rtranslatordevedition.voice_translation.cloud_apis.translation.Translator;
import nie.translator.rtranslatordevedition.voice_translation.engines.CapabilityState;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineCapability;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineError;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineOperation;
import nie.translator.rtranslatordevedition.voice_translation.engines.TextTranslationEngine;

/** Adapter over the existing cloud translator. Cancellation only suppresses late callbacks: its HTTP work cannot abort. */
public final class LegacyCloudTextTranslationEngine implements TextTranslationEngine {
    private final Object lock = new Object();
    private final Translator translator;
    private final Set<EngineOperation> pendingOperations = new HashSet<>();
    private boolean closed;

    public LegacyCloudTextTranslationEngine(Translator translator) {
        this.translator = Objects.requireNonNull(translator, "translator");
    }

    @Override
    public EngineCapability getCapability() {
        synchronized (lock) {
            return closed
                    ? new EngineCapability(CapabilityState.CLOSED, "Engine is closed")
                    : new EngineCapability(CapabilityState.AVAILABLE, "Legacy cloud translator available");
        }
    }

    @Override
    public EngineOperation translate(String text, CustomLocale outputLanguage, final TranslationCallback callback) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(outputLanguage, "outputLanguage");
        Objects.requireNonNull(callback, "callback");
        final EngineOperation operation = newOperation();
        if (!register(operation)) {
            failClosed(operation, callback);
            return operation;
        }
        try {
            if (!operation.isActive()) { return operation; }
            translator.translate(text, outputLanguage, new Translator.TranslateListener() {
                @Override public void onTranslatedText(final String translated, final CustomLocale outputLanguage) {
                    complete(operation, new Runnable() { @Override public void run() { callback.onTranslated(translated, outputLanguage); } });
                }
                @Override public void onFailure(final int[] reasons, final long value) {
                    complete(operation, new Runnable() { @Override public void run() { callback.onFailure(LegacyErrorMapper.map(reasons, value)); } });
                }
            });
        } catch (RuntimeException error) {
            failInternal(operation, callback);
        }
        return operation;
    }

    @Override
    public EngineOperation detectLanguage(String text, final LanguageDetectionCallback callback) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(callback, "callback");
        final EngineOperation operation = newOperation();
        if (!register(operation)) { failClosed(operation, callback); return operation; }
        try {
            if (!operation.isActive()) { return operation; }
            translator.detectLanguage(new CloudApiResult(text), new Translator.DetectLanguageListener() {
                @Override public void onDetectedText(final CloudApiResult result) {
                    final CustomLocale language = result.getLanguage();
                    if (language == null) { failInternal(operation, callback); return; }
                    complete(operation, new Runnable() { @Override public void run() { callback.onDetected(language); } });
                }
                @Override public void onFailure(final int[] reasons, final long value) {
                    complete(operation, new Runnable() { @Override public void run() { callback.onFailure(LegacyErrorMapper.map(reasons, value)); } });
                }
            });
        } catch (RuntimeException error) {
            failInternal(operation, callback);
        }
        return operation;
    }

    @Override
    public EngineOperation getSupportedLanguages(CustomLocale displayLanguage, final SupportedLanguagesCallback callback) {
        Objects.requireNonNull(displayLanguage, "displayLanguage");
        Objects.requireNonNull(callback, "callback");
        final EngineOperation operation = newOperation();
        if (!register(operation)) { failClosed(operation, callback); return operation; }
        try {
            if (!operation.isActive()) { return operation; }
            translator.getSupportedLanguages(displayLanguage, new Translator.SupportedLanguagesListener() {
                @Override public void onLanguagesListAvailable(ArrayList<CustomLocale> languages) {
                    final List<CustomLocale> copy = Collections.unmodifiableList(new ArrayList<>(languages));
                    complete(operation, new Runnable() { @Override public void run() { callback.onLanguages(copy); } });
                }
                @Override public void onFailure(final int[] reasons, final long value) {
                    complete(operation, new Runnable() { @Override public void run() { callback.onFailure(LegacyErrorMapper.map(reasons, value)); } });
                }
            });
        } catch (RuntimeException error) {
            failInternal(operation, callback);
        }
        return operation;
    }

    @Override public void close() {
        final List<EngineOperation> operations;
        synchronized (lock) {
            if (closed) { return; }
            closed = true;
            operations = new ArrayList<>(pendingOperations);
            pendingOperations.clear();
        }
        for (EngineOperation operation : operations) { operation.cancel(); }
    }

    private EngineOperation newOperation() {
        final EngineOperation[] reference = new EngineOperation[1];
        EngineOperation operation = new EngineOperation(new EngineOperation.CancelAction() {
            @Override public void onCancel() { unregister(reference[0]); }
        });
        reference[0] = operation;
        return operation;
    }

    private boolean register(EngineOperation operation) {
        synchronized (lock) {
            if (closed) { return false; }
            pendingOperations.add(operation);
            return true;
        }
    }

    private void unregister(EngineOperation operation) {
        synchronized (lock) { pendingOperations.remove(operation); }
    }

    private void complete(final EngineOperation operation, final Runnable callback) {
        operation.complete(new Runnable() {
            @Override public void run() {
                unregister(operation);
                callback.run();
            }
        });
    }

    private static EngineError closedError() { return new EngineError(EngineError.Category.CLOSED, new int[0], 0L, "Engine is closed"); }
    private static EngineError internalError() { return new EngineError(EngineError.Category.INTERNAL_FAILURE, new int[0], 0L, "Legacy translator could not start"); }
    private void failClosed(EngineOperation operation, final TranslationCallback callback) { complete(operation, new Runnable() { @Override public void run() { callback.onFailure(closedError()); } }); }
    private void failClosed(EngineOperation operation, final LanguageDetectionCallback callback) { complete(operation, new Runnable() { @Override public void run() { callback.onFailure(closedError()); } }); }
    private void failClosed(EngineOperation operation, final SupportedLanguagesCallback callback) { complete(operation, new Runnable() { @Override public void run() { callback.onFailure(closedError()); } }); }
    private void failInternal(EngineOperation operation, final TranslationCallback callback) { complete(operation, new Runnable() { @Override public void run() { callback.onFailure(internalError()); } }); }
    private void failInternal(EngineOperation operation, final LanguageDetectionCallback callback) { complete(operation, new Runnable() { @Override public void run() { callback.onFailure(internalError()); } }); }
    private void failInternal(EngineOperation operation, final SupportedLanguagesCallback callback) { complete(operation, new Runnable() { @Override public void run() { callback.onFailure(internalError()); } }); }
}
