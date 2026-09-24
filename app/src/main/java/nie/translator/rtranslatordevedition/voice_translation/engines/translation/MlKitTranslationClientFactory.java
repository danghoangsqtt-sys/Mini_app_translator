package nie.translator.rtranslatordevedition.voice_translation.engines.translation;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import java.util.concurrent.atomic.AtomicBoolean;

/** Creates short-lived official ML Kit clients; adapters make close idempotent. */
public final class MlKitTranslationClientFactory implements TranslationClientFactory {
    @Override public LanguageIdentifierClient createLanguageIdentifier() {
        return new IdentifierAdapter(LanguageIdentification.getClient());
    }

    @Override public TranslatorClient createTranslator(String sourceLanguageTag, String targetLanguageTag) {
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguageTag)
                .setTargetLanguage(targetLanguageTag)
                .build();
        return new TranslatorAdapter(Translation.getClient(options));
    }

    private static final class IdentifierAdapter implements LanguageIdentifierClient {
        private final LanguageIdentifier identifier;
        private final AtomicBoolean closed = new AtomicBoolean();

        private IdentifierAdapter(LanguageIdentifier identifier) {
            this.identifier = identifier;
        }

        @Override public void identifyLanguage(String text, final AsyncResultCallback<String> callback) {
            identifier.identifyLanguage(text)
                    .addOnSuccessListener(new OnSuccessListener<String>() {
                        @Override public void onSuccess(String languageTag) { callback.onSuccess(languageTag); }
                    })
                    .addOnFailureListener(failure(callback));
        }

        @Override public void close() {
            if (closed.compareAndSet(false, true)) {
                identifier.close();
            }
        }
    }

    private static final class TranslatorAdapter implements TranslatorClient {
        private final Translator translator;
        private final AtomicBoolean closed = new AtomicBoolean();

        private TranslatorAdapter(Translator translator) {
            this.translator = translator;
        }

        @Override public void translate(String text, final AsyncResultCallback<String> callback) {
            translator.translate(text)
                    .addOnSuccessListener(new OnSuccessListener<String>() {
                        @Override public void onSuccess(String translated) { callback.onSuccess(translated); }
                    })
                    .addOnFailureListener(failure(callback));
        }

        @Override public void close() {
            if (closed.compareAndSet(false, true)) {
                translator.close();
            }
        }
    }

    private static <T> OnFailureListener failure(final AsyncResultCallback<T> callback) {
        return new OnFailureListener() {
            @Override public void onFailure(Exception error) { callback.onFailure(error); }
        };
    }
}
