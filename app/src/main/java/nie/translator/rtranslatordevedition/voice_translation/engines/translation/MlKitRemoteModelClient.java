package nie.translator.rtranslatordevedition.voice_translation.engines.translation;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.TranslateRemoteModel;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Official RemoteModelManager adapter. Google Tasks intentionally remain non-cancellable here. */
public final class MlKitRemoteModelClient implements RemoteModelClient {
    private final RemoteModelManager manager;

    public MlKitRemoteModelClient() {
        this(RemoteModelManager.getInstance());
    }

    MlKitRemoteModelClient(RemoteModelManager manager) {
        this.manager = manager;
    }

    @Override public void isModelDownloaded(String languageTag, final AsyncResultCallback<Boolean> callback) {
        if (TranslateLanguage.ENGLISH.equals(languageTag)) {
            callback.onSuccess(true);
            return;
        }
        manager.isModelDownloaded(model(languageTag))
                .addOnSuccessListener(new OnSuccessListener<Boolean>() {
                    @Override public void onSuccess(Boolean result) { callback.onSuccess(Boolean.TRUE.equals(result)); }
                })
                .addOnFailureListener(failure(callback));
    }

    @Override public void getDownloadedModelTags(final AsyncResultCallback<Set<String>> callback) {
        manager.getDownloadedModels(TranslateRemoteModel.class)
                .addOnSuccessListener(new OnSuccessListener<Set<TranslateRemoteModel>>() {
                    @Override public void onSuccess(Set<TranslateRemoteModel> models) {
                        LinkedHashSet<String> tags = new LinkedHashSet<>();
                        for (TranslateRemoteModel model : models) {
                            tags.add(model.getLanguage());
                        }
                        callback.onSuccess(Collections.unmodifiableSet(tags));
                    }
                })
                .addOnFailureListener(failure(callback));
    }

    @Override public void download(String languageTag, ModelDownloadPolicy policy, final AsyncResultCallback<Void> callback) {
        rejectEnglish(languageTag);
        DownloadConditions.Builder builder = new DownloadConditions.Builder();
        if (policy == ModelDownloadPolicy.WIFI_ONLY) {
            builder.requireWifi();
        }
        manager.download(model(languageTag), builder.build())
                .addOnSuccessListener(success(callback))
                .addOnFailureListener(failure(callback));
    }

    @Override public void delete(String languageTag, final AsyncResultCallback<Void> callback) {
        rejectEnglish(languageTag);
        manager.deleteDownloadedModel(model(languageTag))
                .addOnSuccessListener(success(callback))
                .addOnFailureListener(failure(callback));
    }

    private static TranslateRemoteModel model(String languageTag) {
        return new TranslateRemoteModel.Builder(languageTag).build();
    }

    private static void rejectEnglish(String languageTag) {
        if (TranslateLanguage.ENGLISH.equals(languageTag)) {
            throw new IllegalArgumentException("English is built in");
        }
    }

    private static OnSuccessListener<Void> success(final AsyncResultCallback<Void> callback) {
        return new OnSuccessListener<Void>() {
            @Override public void onSuccess(Void ignored) { callback.onSuccess(null); }
        };
    }

    private static <T> OnFailureListener failure(final AsyncResultCallback<T> callback) {
        return new OnFailureListener() {
            @Override public void onFailure(Exception error) { callback.onFailure(error); }
        };
    }
}
