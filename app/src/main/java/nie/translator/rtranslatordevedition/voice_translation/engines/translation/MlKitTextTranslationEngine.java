package nie.translator.rtranslatordevedition.voice_translation.engines.translation;

import com.google.mlkit.nl.translate.TranslateLanguage;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import nie.translator.rtranslatordevedition.tools.CustomLocale;
import nie.translator.rtranslatordevedition.voice_translation.engines.CapabilityState;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineCapability;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineError;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineOperation;
import nie.translator.rtranslatordevedition.voice_translation.engines.TextTranslationEngine;

/** On-device engine: identify source, verify explicit model availability, then translate. */
public final class MlKitTextTranslationEngine implements TextTranslationEngine {
    private enum Mode { TRANSLATE, DETECT }

    private final Object lock = new Object();
    private final MlKitLanguageMapper mapper;
    private final RemoteModelClient modelClient;
    private final TranslationClientFactory clientFactory;
    private boolean closed;
    private Session activeSession;

    public MlKitTextTranslationEngine() {
        this(new MlKitLanguageMapper(), new MlKitRemoteModelClient(), new MlKitTranslationClientFactory());
    }

    MlKitTextTranslationEngine(MlKitLanguageMapper mapper, RemoteModelClient modelClient,
                               TranslationClientFactory clientFactory) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.modelClient = Objects.requireNonNull(modelClient, "modelClient");
        this.clientFactory = Objects.requireNonNull(clientFactory, "clientFactory");
    }

    @Override public EngineCapability getCapability() {
        synchronized (lock) {
            return closed
                    ? new EngineCapability(CapabilityState.CLOSED, "On-device translator is closed")
                    : new EngineCapability(CapabilityState.AVAILABLE, "On-device translation is available");
        }
    }

    @Override public EngineOperation translate(String text, CustomLocale outputLanguage,
                                               TranslationCallback callback) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(outputLanguage, "outputLanguage");
        Objects.requireNonNull(callback, "callback");
        final String targetTag;
        try {
            targetTag = mapper.toSupportedTag(outputLanguage);
        } catch (IllegalArgumentException error) {
            return rejected(callback, MlKitErrorMapper.unsupportedLanguage());
        }
        Session session = new Session(Mode.TRANSLATE, text, targetTag, outputLanguage, callback, null);
        if (!activate(session)) {
            failRejected(session);
            return session.operation;
        }
        startIdentification(session);
        return session.operation;
    }

    @Override public EngineOperation detectLanguage(String text, LanguageDetectionCallback callback) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(callback, "callback");
        Session session = new Session(Mode.DETECT, text, null, null, null, callback);
        if (!activate(session)) {
            failRejected(session);
            return session.operation;
        }
        startIdentification(session);
        return session.operation;
    }

    @Override public EngineOperation getSupportedLanguages(CustomLocale displayLanguage,
                                                           final SupportedLanguagesCallback callback) {
        Objects.requireNonNull(displayLanguage, "displayLanguage");
        Objects.requireNonNull(callback, "callback");
        final EngineOperation operation = new EngineOperation(null);
        final boolean isClosed;
        synchronized (lock) {
            isClosed = closed;
        }
        if (isClosed) {
            operation.complete(new Runnable() {
                @Override public void run() { callback.onFailure(MlKitErrorMapper.closed()); }
            });
            return operation;
        }
        final List<CustomLocale> languages = mapper.getSupportedLocales(displayLanguage);
        operation.complete(new Runnable() {
            @Override public void run() { callback.onLanguages(languages); }
        });
        return operation;
    }

    @Override public void close() {
        final Session session;
        synchronized (lock) {
            if (closed) { return; }
            closed = true;
            session = activeSession;
            activeSession = null;
        }
        if (session != null) {
            session.operation.cancel();
            session.closeClients();
        }
    }

    private boolean activate(final Session session) {
        synchronized (lock) {
            if (closed || (activeSession != null && activeSession.operation.isActive())) {
                return false;
            }
            activeSession = session;
            return true;
        }
    }

    private void startIdentification(final Session session) {
        final TranslationClientFactory.LanguageIdentifierClient identifier;
        try {
            identifier = clientFactory.createLanguageIdentifier();
        } catch (RuntimeException error) {
            completeFailure(session, MlKitErrorMapper.map(error));
            return;
        }
        if (!attachIdentifier(session, identifier)) {
            identifier.close();
            return;
        }
        try {
            identifier.identifyLanguage(session.text, new AsyncResultCallback<String>() {
                @Override public void onSuccess(String languageTag) {
                    session.closeIdentifier();
                    handleIdentified(session, languageTag);
                }

                @Override public void onFailure(Exception error) {
                    session.closeIdentifier();
                    completeFailure(session, MlKitErrorMapper.map(error));
                }
            });
        } catch (RuntimeException error) {
            session.closeIdentifier();
            completeFailure(session, MlKitErrorMapper.map(error));
        }
    }

    private void handleIdentified(final Session session, String identifiedTag) {
        if (!isCurrent(session)) { return; }
        final String sourceTag;
        try {
            sourceTag = mapper.toSupportedTag(identifiedTag);
        } catch (IllegalArgumentException error) {
            completeFailure(session, MlKitErrorMapper.unsupportedLanguage());
            return;
        }
        if (session.mode == Mode.DETECT) {
            final CustomLocale language = mapper.toCustomLocale(sourceTag);
            complete(session, new Runnable() {
                @Override public void run() { session.detectionCallback.onDetected(language); }
            });
            return;
        }
        session.sourceTag = sourceTag;
        if (sourceTag.equals(session.targetTag)) {
            complete(session, new Runnable() {
                @Override public void run() {
                    session.translationCallback.onTranslated(session.text, session.outputLanguage);
                }
            });
            return;
        }
        Set<String> required = new LinkedHashSet<>();
        if (!TranslateLanguage.ENGLISH.equals(sourceTag)) { required.add(sourceTag); }
        if (!TranslateLanguage.ENGLISH.equals(session.targetTag)) { required.add(session.targetTag); }
        checkModels(session, new ArrayList<>(required), 0);
    }

    private void checkModels(final Session session, final List<String> required, final int index) {
        if (!isCurrent(session)) { return; }
        if (index >= required.size()) {
            startTranslation(session);
            return;
        }
        try {
            modelClient.isModelDownloaded(required.get(index), new AsyncResultCallback<Boolean>() {
                @Override public void onSuccess(Boolean downloaded) {
                    if (!isCurrent(session)) { return; }
                    if (!Boolean.TRUE.equals(downloaded)) {
                        completeFailure(session, MlKitErrorMapper.missingModel());
                        return;
                    }
                    checkModels(session, required, index + 1);
                }

                @Override public void onFailure(Exception error) {
                    completeFailure(session, MlKitErrorMapper.map(error));
                }
            });
        } catch (RuntimeException error) {
            completeFailure(session, MlKitErrorMapper.map(error));
        }
    }

    private void startTranslation(final Session session) {
        final TranslationClientFactory.TranslatorClient translator;
        try {
            translator = clientFactory.createTranslator(session.sourceTag, session.targetTag);
        } catch (RuntimeException error) {
            completeFailure(session, MlKitErrorMapper.map(error));
            return;
        }
        if (!attachTranslator(session, translator)) {
            translator.close();
            return;
        }
        try {
            translator.translate(session.text, new AsyncResultCallback<String>() {
                @Override public void onSuccess(final String translated) {
                    session.closeTranslator();
                    complete(session, new Runnable() {
                        @Override public void run() {
                            session.translationCallback.onTranslated(translated, session.outputLanguage);
                        }
                    });
                }

                @Override public void onFailure(Exception error) {
                    session.closeTranslator();
                    completeFailure(session, MlKitErrorMapper.map(error));
                }
            });
        } catch (RuntimeException error) {
            session.closeTranslator();
            completeFailure(session, MlKitErrorMapper.map(error));
        }
    }

    private boolean attachIdentifier(Session session, TranslationClientFactory.LanguageIdentifierClient client) {
        synchronized (lock) {
            if (!isCurrentLocked(session)) { return false; }
            session.identifier = client;
            return true;
        }
    }

    private boolean attachTranslator(Session session, TranslationClientFactory.TranslatorClient client) {
        synchronized (lock) {
            if (!isCurrentLocked(session)) { return false; }
            session.translator = client;
            return true;
        }
    }

    private boolean isCurrent(Session session) {
        synchronized (lock) {
            return isCurrentLocked(session);
        }
    }

    private boolean isCurrentLocked(Session session) {
        return !closed && activeSession == session && session.operation.isActive();
    }

    private void completeFailure(final Session session, final EngineError error) {
        if (session.mode == Mode.TRANSLATE) {
            complete(session, new Runnable() {
                @Override public void run() { session.translationCallback.onFailure(error); }
            });
        } else {
            complete(session, new Runnable() {
                @Override public void run() { session.detectionCallback.onFailure(error); }
            });
        }
    }

    private void complete(final Session session, final Runnable callback) {
        if (!isCurrent(session)) {
            session.closeClients();
            return;
        }
        session.operation.complete(new Runnable() {
            @Override public void run() {
                detach(session);
                session.closeClients();
                callback.run();
            }
        });
    }

    private void detach(Session session) {
        synchronized (lock) {
            if (activeSession == session) {
                activeSession = null;
            }
        }
    }

    private void cancel(Session session) {
        detach(session);
        session.closeClients();
    }

    private void failRejected(Session session) {
        final EngineError error;
        synchronized (lock) {
            error = closed ? MlKitErrorMapper.closed() : MlKitErrorMapper.busy();
        }
        if (session.mode == Mode.TRANSLATE) {
            session.operation.complete(new Runnable() {
                @Override public void run() { session.translationCallback.onFailure(error); }
            });
        } else {
            session.operation.complete(new Runnable() {
                @Override public void run() { session.detectionCallback.onFailure(error); }
            });
        }
    }

    private static EngineOperation rejected(final TranslationCallback callback, final EngineError error) {
        EngineOperation operation = new EngineOperation(null);
        operation.complete(new Runnable() {
            @Override public void run() { callback.onFailure(error); }
        });
        return operation;
    }

    private final class Session {
        private final Mode mode;
        private final String text;
        private final String targetTag;
        private final CustomLocale outputLanguage;
        private final TranslationCallback translationCallback;
        private final LanguageDetectionCallback detectionCallback;
        private final EngineOperation operation;
        private String sourceTag;
        private TranslationClientFactory.LanguageIdentifierClient identifier;
        private TranslationClientFactory.TranslatorClient translator;

        private Session(Mode mode, String text, String targetTag, CustomLocale outputLanguage,
                        TranslationCallback translationCallback,
                        LanguageDetectionCallback detectionCallback) {
            this.mode = mode;
            this.text = text;
            this.targetTag = targetTag;
            this.outputLanguage = outputLanguage;
            this.translationCallback = translationCallback;
            this.detectionCallback = detectionCallback;
            this.operation = new EngineOperation(new EngineOperation.CancelAction() {
                @Override public void onCancel() { cancel(Session.this); }
            });
        }

        private void closeIdentifier() {
            final TranslationClientFactory.LanguageIdentifierClient current;
            synchronized (lock) {
                current = identifier;
                identifier = null;
            }
            if (current != null) { current.close(); }
        }

        private void closeTranslator() {
            final TranslationClientFactory.TranslatorClient current;
            synchronized (lock) {
                current = translator;
                translator = null;
            }
            if (current != null) { current.close(); }
        }

        private void closeClients() {
            closeIdentifier();
            closeTranslator();
        }
    }
}
