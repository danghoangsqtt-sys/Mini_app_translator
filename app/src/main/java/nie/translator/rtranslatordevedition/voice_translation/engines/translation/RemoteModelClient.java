package nie.translator.rtranslatordevedition.voice_translation.engines.translation;

import java.util.Set;

/** Test seam for the official ML Kit RemoteModelManager operations used by this task. */
public interface RemoteModelClient {
    void isModelDownloaded(String languageTag, AsyncResultCallback<Boolean> callback);
    void getDownloadedModelTags(AsyncResultCallback<Set<String>> callback);
    void download(String languageTag, ModelDownloadPolicy policy, AsyncResultCallback<Void> callback);
    void delete(String languageTag, AsyncResultCallback<Void> callback);
}
