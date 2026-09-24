package nie.translator.rtranslatordevedition.voice_translation.engines.translation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineError;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineOperation;
import org.junit.Test;

public class TranslationModelManagerTest {
    private static MlKitLanguageMapper mapper() {
        return new MlKitLanguageMapper(Arrays.asList("en", "it", "vi"));
    }

    @Test public void englishIsBuiltInAndCannotBeDownloadedOrDeleted() {
        FakeRemoteModelClient client = new FakeRemoteModelClient();
        TranslationModelManager manager = new TranslationModelManager(mapper(), client);
        assertEquals(TranslationModelState.BUILT_IN, manager.getSnapshot().get(0).getState());
        RecordingOperationCallback download = new RecordingOperationCallback();
        RecordingOperationCallback delete = new RecordingOperationCallback();
        manager.download("en-US", ModelDownloadPolicy.WIFI_ONLY, download);
        manager.delete("en", delete);
        assertEquals(EngineError.Category.UNSUPPORTED, download.error.getCategory());
        assertEquals(EngineError.Category.UNSUPPORTED, delete.error.getCategory());
        assertEquals(0, client.downloadCalls);
        assertEquals(0, client.deleteCalls);
    }

    @Test public void refreshPublishesImmutableDownloadedSnapshot() {
        FakeRemoteModelClient client = new FakeRemoteModelClient();
        TranslationModelManager manager = new TranslationModelManager(mapper(), client);
        RecordingModelsCallback callback = new RecordingModelsCallback();
        manager.refresh(callback);
        client.downloadedCallback.onSuccess(Collections.singleton("it-IT"));
        assertEquals(3, callback.models.size());
        assertEquals(TranslationModelState.DOWNLOADED, find(callback.models, "it").getState());
        try {
            callback.models.clear();
            throw new AssertionError("Expected immutable models");
        } catch (UnsupportedOperationException expected) {
            // Expected.
        }
    }

    @Test public void downloadUsesHonestStatesAndSuppressesDuplicate() {
        FakeRemoteModelClient client = new FakeRemoteModelClient();
        TranslationModelManager manager = new TranslationModelManager(mapper(), client);
        final List<TranslationModelState> states = new ArrayList<>();
        manager.setObserver(new TranslationModelManager.Observer() {
            @Override public void onModelChanged(TranslationModel model) { states.add(model.getState()); }
        });
        RecordingOperationCallback first = new RecordingOperationCallback();
        RecordingOperationCallback duplicate = new RecordingOperationCallback();
        manager.download("it-IT", ModelDownloadPolicy.WIFI_ONLY, first);
        manager.download("it", ModelDownloadPolicy.ANY_NETWORK, duplicate);
        assertEquals(Arrays.asList(TranslationModelState.QUEUED, TranslationModelState.DOWNLOADING), states);
        assertEquals(ModelDownloadPolicy.WIFI_ONLY, client.lastPolicy);
        assertEquals(1, client.downloadCalls);
        assertEquals(EngineError.Category.BUSY, duplicate.error.getCategory());
        client.downloadCallback.onSuccess(null);
        assertEquals(TranslationModelState.DOWNLOADED, first.model.getState());
        assertEquals(TranslationModelState.DOWNLOADED, states.get(states.size() - 1));
    }

    @Test public void anyNetworkIsForwardedOnlyWhenExplicitlySelected() {
        FakeRemoteModelClient client = new FakeRemoteModelClient();
        TranslationModelManager manager = new TranslationModelManager(mapper(), client);
        manager.download("vi", ModelDownloadPolicy.ANY_NETWORK, new RecordingOperationCallback());
        assertEquals(ModelDownloadPolicy.ANY_NETWORK, client.lastPolicy);
    }

    @Test public void failureTransitionsToFailedWithSanitizedError() {
        FakeRemoteModelClient client = new FakeRemoteModelClient();
        TranslationModelManager manager = new TranslationModelManager(mapper(), client);
        RecordingOperationCallback callback = new RecordingOperationCallback();
        manager.download("it", ModelDownloadPolicy.WIFI_ONLY, callback);
        client.downloadCallback.onFailure(new IllegalStateException("sensitive"));
        assertEquals(EngineError.Category.INTERNAL_FAILURE, callback.error.getCategory());
        assertEquals(TranslationModelState.FAILED, find(manager.getSnapshot(), "it").getState());
        assertFalse(callback.error.getDetail().contains("sensitive"));
    }

    @Test public void cancelSuppressesLateDownloadCallback() {
        FakeRemoteModelClient client = new FakeRemoteModelClient();
        TranslationModelManager manager = new TranslationModelManager(mapper(), client);
        RecordingOperationCallback callback = new RecordingOperationCallback();
        EngineOperation operation = manager.download("it", ModelDownloadPolicy.WIFI_ONLY, callback);
        assertTrue(operation.cancel());
        client.downloadCallback.onSuccess(null);
        assertEquals(0, callback.calls.get());
        assertEquals(TranslationModelState.NOT_DOWNLOADED, find(manager.getSnapshot(), "it").getState());
    }

    @Test public void observerCanCancelQueuedDownloadBeforeSdkStartsAndRestoresState() {
        FakeRemoteModelClient client = new FakeRemoteModelClient();
        final TranslationModelManager manager = new TranslationModelManager(mapper(), client);
        final List<TranslationModelState> states = new ArrayList<>();
        manager.setObserver(new TranslationModelManager.Observer() {
            @Override public void onModelChanged(TranslationModel model) {
                states.add(model.getState());
                if (model.getState() == TranslationModelState.QUEUED) {
                    assertTrue(manager.cancel("it"));
                }
            }
        });
        RecordingOperationCallback callback = new RecordingOperationCallback();
        EngineOperation operation = manager.download("it", ModelDownloadPolicy.WIFI_ONLY, callback);
        assertTrue(operation.isCancelled());
        assertEquals(0, client.downloadCalls);
        assertEquals(0, callback.calls.get());
        assertEquals(Arrays.asList(TranslationModelState.QUEUED,
                TranslationModelState.NOT_DOWNLOADED), states);
        assertEquals(TranslationModelState.NOT_DOWNLOADED, find(manager.getSnapshot(), "it").getState());
    }

    @Test public void observerCanCancelDownloadingBeforeSdkStartsAndRestoresState() {
        FakeRemoteModelClient client = new FakeRemoteModelClient();
        final TranslationModelManager manager = new TranslationModelManager(mapper(), client);
        final List<TranslationModelState> states = new ArrayList<>();
        manager.setObserver(new TranslationModelManager.Observer() {
            @Override public void onModelChanged(TranslationModel model) {
                states.add(model.getState());
                if (model.getState() == TranslationModelState.DOWNLOADING) {
                    assertTrue(manager.cancel("it"));
                }
            }
        });
        RecordingOperationCallback callback = new RecordingOperationCallback();
        EngineOperation operation = manager.download("it", ModelDownloadPolicy.WIFI_ONLY, callback);
        assertTrue(operation.isCancelled());
        assertEquals(0, client.downloadCalls);
        assertEquals(0, callback.calls.get());
        assertEquals(Arrays.asList(TranslationModelState.QUEUED,
                TranslationModelState.DOWNLOADING, TranslationModelState.NOT_DOWNLOADED), states);
        assertEquals(TranslationModelState.NOT_DOWNLOADED, find(manager.getSnapshot(), "it").getState());
    }

    @Test public void observerCanCancelDeletingBeforeSdkStartsAndRestoresState() {
        FakeRemoteModelClient client = new FakeRemoteModelClient();
        final TranslationModelManager manager = new TranslationModelManager(mapper(), client);
        final List<TranslationModelState> states = new ArrayList<>();
        manager.setObserver(new TranslationModelManager.Observer() {
            @Override public void onModelChanged(TranslationModel model) {
                states.add(model.getState());
                if (model.getState() == TranslationModelState.DELETING) {
                    assertTrue(manager.cancel("it"));
                }
            }
        });
        RecordingOperationCallback callback = new RecordingOperationCallback();
        EngineOperation operation = manager.delete("it", callback);
        assertTrue(operation.isCancelled());
        assertEquals(0, client.deleteCalls);
        assertEquals(0, callback.calls.get());
        assertEquals(Arrays.asList(TranslationModelState.DELETING,
                TranslationModelState.NOT_DOWNLOADED), states);
        assertEquals(TranslationModelState.NOT_DOWNLOADED, find(manager.getSnapshot(), "it").getState());
    }

    @Test public void observerClosePreventsSdkStartAfterLogicalOperationBecomesInactive() {
        FakeRemoteModelClient client = new FakeRemoteModelClient();
        final TranslationModelManager manager = new TranslationModelManager(mapper(), client);
        manager.setObserver(new TranslationModelManager.Observer() {
            @Override public void onModelChanged(TranslationModel model) {
                if (model.getState() == TranslationModelState.QUEUED) {
                    manager.close();
                }
            }
        });
        RecordingOperationCallback callback = new RecordingOperationCallback();
        EngineOperation operation = manager.download("it", ModelDownloadPolicy.WIFI_ONLY, callback);
        assertTrue(operation.isCancelled());
        assertEquals(0, client.downloadCalls);
        assertEquals(0, callback.calls.get());
    }

    @Test public void observerReplacementCannotLetOldOperationStartOrCompleteNewOperation() {
        FakeRemoteModelClient client = new FakeRemoteModelClient();
        final TranslationModelManager manager = new TranslationModelManager(mapper(), client);
        final boolean[] replaced = new boolean[] { false };
        final EngineOperation[] replacement = new EngineOperation[1];
        final RecordingOperationCallback replacementCallback = new RecordingOperationCallback();
        manager.setObserver(new TranslationModelManager.Observer() {
            @Override public void onModelChanged(TranslationModel model) {
                if (!replaced[0] && model.getState() == TranslationModelState.QUEUED) {
                    replaced[0] = true;
                    assertTrue(manager.cancel("it"));
                    replacement[0] = manager.download("it", ModelDownloadPolicy.WIFI_ONLY,
                            replacementCallback);
                }
            }
        });
        RecordingOperationCallback originalCallback = new RecordingOperationCallback();
        EngineOperation original = manager.download("it", ModelDownloadPolicy.WIFI_ONLY, originalCallback);
        assertTrue(original.isCancelled());
        assertTrue(replacement[0].isActive());
        assertEquals(1, client.downloadCalls);
        client.downloadCallback.onSuccess(null);
        assertEquals(0, originalCallback.calls.get());
        assertEquals(1, replacementCallback.calls.get());
    }

    @Test public void closeSuppressesRefreshAndOperationCallbacks() {
        FakeRemoteModelClient client = new FakeRemoteModelClient();
        TranslationModelManager manager = new TranslationModelManager(mapper(), client);
        RecordingModelsCallback refresh = new RecordingModelsCallback();
        RecordingOperationCallback download = new RecordingOperationCallback();
        manager.refresh(refresh);
        manager.download("it", ModelDownloadPolicy.WIFI_ONLY, download);
        manager.close();
        client.downloadedCallback.onSuccess(Collections.singleton("it"));
        client.downloadCallback.onSuccess(null);
        assertEquals(0, refresh.calls.get());
        assertEquals(0, download.calls.get());
    }

    @Test public void staleDeleteCannotCompleteReplacementDownload() {
        FakeRemoteModelClient client = new FakeRemoteModelClient();
        TranslationModelManager manager = new TranslationModelManager(mapper(), client);
        RecordingOperationCallback deleted = new RecordingOperationCallback();
        EngineOperation delete = manager.delete("it", deleted);
        AsyncResultCallback<Void> oldDeleteCallback = client.deleteCallback;
        assertTrue(delete.cancel());
        RecordingOperationCallback downloaded = new RecordingOperationCallback();
        EngineOperation replacement = manager.download("it", ModelDownloadPolicy.WIFI_ONLY, downloaded);
        assertNotEquals(delete, replacement);
        oldDeleteCallback.onSuccess(null);
        assertEquals(0, deleted.calls.get());
        assertEquals(TranslationModelState.DOWNLOADING, find(manager.getSnapshot(), "it").getState());
        client.downloadCallback.onSuccess(null);
        assertEquals(1, downloaded.calls.get());
    }

    private static TranslationModel find(List<TranslationModel> models, String tag) {
        for (TranslationModel model : models) {
            if (tag.equals(model.getLanguageTag())) { return model; }
        }
        throw new AssertionError("Missing model " + tag);
    }

    private static final class RecordingModelsCallback implements TranslationModelManager.ModelsCallback {
        private final AtomicInteger calls = new AtomicInteger();
        private List<TranslationModel> models;
        @Override public void onModels(List<TranslationModel> models) { calls.incrementAndGet(); this.models = models; }
        @Override public void onFailure(EngineError error) { calls.incrementAndGet(); }
    }

    private static final class RecordingOperationCallback implements TranslationModelManager.OperationCallback {
        private final AtomicInteger calls = new AtomicInteger();
        private TranslationModel model;
        private EngineError error;
        @Override public void onSuccess(TranslationModel model) { calls.incrementAndGet(); this.model = model; }
        @Override public void onFailure(EngineError error) { calls.incrementAndGet(); this.error = error; }
    }

    private static final class FakeRemoteModelClient implements RemoteModelClient {
        private int downloadCalls;
        private int deleteCalls;
        private ModelDownloadPolicy lastPolicy;
        private AsyncResultCallback<Set<String>> downloadedCallback;
        private AsyncResultCallback<Void> downloadCallback;
        private AsyncResultCallback<Void> deleteCallback;

        @Override public void isModelDownloaded(String languageTag, AsyncResultCallback<Boolean> callback) { }
        @Override public void getDownloadedModelTags(AsyncResultCallback<Set<String>> callback) { downloadedCallback = callback; }
        @Override public void download(String languageTag, ModelDownloadPolicy policy, AsyncResultCallback<Void> callback) {
            downloadCalls++;
            lastPolicy = policy;
            downloadCallback = callback;
        }
        @Override public void delete(String languageTag, AsyncResultCallback<Void> callback) {
            deleteCalls++;
            deleteCallback = callback;
        }
    }
}
