/*
 * Copyright 2016 Luca Martino.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copyFile of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nie.translator.rtranslatordevedition.voice_translation._conversation_mode.communication.recent_peer;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import nie.translator.rtranslatordevedition.database.AppDatabase;
import nie.translator.rtranslatordevedition.database.dao.MyDao;
import nie.translator.rtranslatordevedition.database.entities.RecentPeerEntity;
import nie.translator.rtranslatordevedition.tools.Tools;

/** Serializes all recent-peer reads and writes so Bluetooth handshake frames cannot race Room. */
public class RecentPeersDataManager {
    private static final String TAG = "RecentPeersData";

    interface Store {
        void insert(RecentPeerEntity entity);
        void delete(RecentPeerEntity entity);
        RecentPeerEntity[] loadAll();
        RecentPeerEntity loadById(String deviceId);
        RecentPeerEntity loadByName(String uniqueName);
    }

    private final Store store;
    private final Executor databaseExecutor;
    private final Executor callbackExecutor;

    public RecentPeersDataManager(Context context) {
        this(new RoomStore(AppDatabase.getInstance(context).myDao()), createDatabaseExecutor(), mainExecutor());
    }

    RecentPeersDataManager(Store store, Executor databaseExecutor, Executor callbackExecutor) {
        this.store = store;
        this.databaseExecutor = databaseExecutor;
        this.callbackExecutor = callbackExecutor;
    }

    private static ExecutorService createDatabaseExecutor() {
        return Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override public Thread newThread(Runnable runnable) {
                return new Thread(runnable, "recent-peers-db");
            }
        });
    }

    private static Executor mainExecutor() {
        final Handler handler = new Handler(Looper.getMainLooper());
        return new Executor() {
            @Override public void execute(Runnable command) {
                handler.post(command);
            }
        };
    }

    public void insertRecentPeer(@NonNull String deviceId, String uniqueName, Bitmap userImage) {
        byte[] image = userImage == null ? null : Tools.convertBitmapToBytes(userImage);
        insertRecentPeerData(deviceId, uniqueName, image);
    }

    void insertRecentPeerData(@NonNull final String deviceId, final String uniqueName,
                              @Nullable final byte[] userImage) {
        databaseExecutor.execute(new Runnable() {
            @Override public void run() {
                store.insert(entity(deviceId, uniqueName, userImage));
            }
        });
    }

    /** Upserts handshake identity while retaining an image received during an earlier session. */
    public void upsertRecentPeerIdentity(@NonNull final String deviceId, final String uniqueName) {
        databaseExecutor.execute(new Runnable() {
            @Override public void run() {
                RecentPeerEntity current = store.loadById(deviceId);
                byte[] image = current == null ? null : current.userImage;
                store.insert(entity(deviceId, uniqueName, image));
            }
        });
    }

    public void updateRecentPeerImageByName(@NonNull String uniqueName, @Nullable Bitmap image) {
        byte[] imageData = image == null ? null : Tools.convertBitmapToBytes(image);
        updateRecentPeerImageDataByName(uniqueName, imageData);
    }

    void updateRecentPeerImageDataByName(@NonNull final String uniqueName,
                                         @Nullable final byte[] imageData) {
        databaseExecutor.execute(new Runnable() {
            @Override public void run() {
                RecentPeerEntity current = store.loadByName(uniqueName);
                if (current != null) {
                    store.insert(entity(current.deviceId, current.uniqueName, imageData));
                }
            }
        });
    }

    public void updateRecentPeerName(@NonNull final String oldUniqueName,
                                     @NonNull final String newUniqueName) {
        databaseExecutor.execute(new Runnable() {
            @Override public void run() {
                RecentPeerEntity current = store.loadByName(oldUniqueName);
                if (current != null) {
                    store.insert(entity(current.deviceId, newUniqueName, current.userImage));
                }
            }
        });
    }

    public void deleteRecentPeer(final RecentPeer peer) {
        databaseExecutor.execute(new Runnable() {
            @Override public void run() {
                store.delete(entity(peer.getDeviceID(), peer.getUniqueName(), peer.getUserImageData()));
            }
        });
    }

    public void getRecentPeers(final RecentPeersListener responseListener) {
        databaseExecutor.execute(new Runnable() {
            @Override public void run() {
                final ArrayList<RecentPeer> peers = new ArrayList<>();
                try {
                    RecentPeerEntity[] entities = store.loadAll();
                    if (entities != null) {
                        for (RecentPeerEntity entity : entities) {
                            peers.add(entity.getRecentPeer());
                        }
                    }
                } catch (RuntimeException error) {
                    Log.e(TAG, "Unable to load recent peers", error);
                }
                callbackExecutor.execute(new Runnable() {
                    @Override public void run() {
                        responseListener.onRecentPeersObtained(new ArrayList<>(peers));
                    }
                });
            }
        });
    }

    public interface RecentPeersListener {
        void onRecentPeersObtained(ArrayList<RecentPeer> recentPeers);
    }

    public void getRecentPeer(final String deviceId, final RecentPeerListener responseListener) {
        loadOne(normalize(deviceId), false, responseListener);
    }

    public void getRecentPeerByName(final String uniqueName, final RecentPeerListener responseListener) {
        loadOne(normalize(uniqueName), true, responseListener);
    }

    private void loadOne(final String key, final boolean byName,
                         final RecentPeerListener responseListener) {
        databaseExecutor.execute(new Runnable() {
            @Override public void run() {
                RecentPeer value = null;
                try {
                    RecentPeerEntity entity = byName ? store.loadByName(key) : store.loadById(key);
                    if (entity != null) {
                        value = entity.getRecentPeer();
                    }
                } catch (RuntimeException error) {
                    Log.e(TAG, "Unable to load recent peer", error);
                }
                final RecentPeer result = value;
                callbackExecutor.execute(new Runnable() {
                    @Override public void run() {
                        responseListener.onRecentPeerObtained(result);
                    }
                });
            }
        });
    }

    public interface RecentPeerListener {
        void onRecentPeerObtained(@Nullable RecentPeer recentPeer);
    }

    private static String normalize(@Nullable String value) {
        return value == null ? "" : value;
    }

    private static RecentPeerEntity entity(String deviceId, String uniqueName, @Nullable byte[] image) {
        RecentPeerEntity entity = new RecentPeerEntity();
        entity.deviceId = deviceId;
        entity.uniqueName = uniqueName;
        entity.userImage = image == null ? null : Arrays.copyOf(image, image.length);
        return entity;
    }

    private static final class RoomStore implements Store {
        private final MyDao dao;

        private RoomStore(MyDao dao) {
            this.dao = dao;
        }

        @Override public void insert(RecentPeerEntity entity) { dao.insertRecentPeers(entity); }
        @Override public void delete(RecentPeerEntity entity) { dao.deleteRecentPeers(entity); }
        @Override public RecentPeerEntity[] loadAll() { return dao.loadRecentPeers(); }
        @Override public RecentPeerEntity loadById(String deviceId) { return dao.loadRecentPeer(deviceId); }
        @Override public RecentPeerEntity loadByName(String uniqueName) { return dao.loadRecentPeerByName(uniqueName); }
    }
}
