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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import nie.translator.rtranslatordevedition.database.AppDatabase;
import nie.translator.rtranslatordevedition.database.dao.MyDao;
import nie.translator.rtranslatordevedition.database.entities.RecentPeerEntity;
import nie.translator.rtranslatordevedition.tools.Tools;

/** Serializes all recent-peer reads and writes so Bluetooth handshake frames cannot race Room. */
public class RecentPeersDataManager {
    static final int MAX_PENDING_DATABASE_TASKS = 32;

    interface Store {
        void insert(RecentPeerEntity entity);
        void delete(RecentPeerEntity entity);
        RecentPeerEntity[] loadAll();
        RecentPeerEntity loadById(String deviceId);
        RecentPeerEntity loadByName(String uniqueName);
    }

    private final Store store;
    public enum Operation { INSERT, IDENTITY, IMAGE, NAME, DELETE, READ_ALL, READ_ONE, QUEUE }
    public enum FailureReason { QUEUE_FULL, CLOSED, STORE_ERROR }

    public static final class PersistenceFailure {
        private final Operation operation;
        private final FailureReason reason;

        private PersistenceFailure(Operation operation, FailureReason reason) {
            this.operation = operation;
            this.reason = reason;
        }

        public Operation getOperation() { return operation; }
        public FailureReason getReason() { return reason; }
    }

    public interface PersistenceFailureListener {
        void onPersistenceFailure(PersistenceFailure failure);
    }

    private final BoundedSerialExecutor databaseExecutor;
    private final Executor callbackExecutor;
    private final ArrayList<PersistenceFailureListener> failureListeners = new ArrayList<>();
    private final AtomicLong operationSequence = new AtomicLong();

    public RecentPeersDataManager(Context context) {
        this(new RoomStore(AppDatabase.getInstance(context).myDao()), createDatabaseExecutor(), mainExecutor());
    }

    RecentPeersDataManager(Store store, Executor databaseExecutor, Executor callbackExecutor) {
        this(store, new BoundedSerialExecutor(databaseExecutor, MAX_PENDING_DATABASE_TASKS),
                callbackExecutor);
    }

    RecentPeersDataManager(Store store, BoundedSerialExecutor databaseExecutor,
                           Executor callbackExecutor) {
        this.store = store;
        this.databaseExecutor = databaseExecutor;
        this.callbackExecutor = callbackExecutor;
    }

    private static ExecutorService createDatabaseExecutor() {
        ThreadFactory factory = new ThreadFactory() {
            @Override public Thread newThread(Runnable runnable) {
                return new Thread(runnable, "recent-peers-db");
            }
        };
        return new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(1), factory,
                new ThreadPoolExecutor.AbortPolicy());
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
        final byte[] image = copy(userImage);
        submitWrite(operationKey("insert", deviceId, uniqueName),
                Operation.INSERT, new Runnable() {
            @Override public void run() {
                store.insert(entity(deviceId, uniqueName, image));
            }
        });
    }

    /** Upserts handshake identity while retaining an image received during an earlier session. */
    public void upsertRecentPeerIdentity(@NonNull final String deviceId, final String uniqueName) {
        submitWrite(operationKey("identity", deviceId, uniqueName),
                Operation.IDENTITY, new Runnable() {
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
        final byte[] image = copy(imageData);
        submitWrite(operationKey("image", uniqueName), Operation.IMAGE, new Runnable() {
            @Override public void run() {
                RecentPeerEntity current = store.loadByName(uniqueName);
                if (current != null) {
                    store.insert(entity(current.deviceId, current.uniqueName, image));
                }
            }
        });
    }

    public void updateRecentPeerName(@NonNull final String oldUniqueName,
                                     @NonNull final String newUniqueName) {
        submitWrite(operationKey("name", oldUniqueName, newUniqueName),
                Operation.NAME, new Runnable() {
            @Override public void run() {
                RecentPeerEntity current = store.loadByName(oldUniqueName);
                if (current != null) {
                    store.insert(entity(current.deviceId, newUniqueName, current.userImage));
                }
            }
        });
    }

    public void deleteRecentPeer(final RecentPeer peer) {
        final String deviceId = peer == null ? "" : peer.getDeviceID();
        final String uniqueName = peer == null ? "" : peer.getUniqueName();
        final byte[] image = peer == null ? null : peer.getUserImageData();
        submitWrite(operationKey("delete", deviceId), Operation.DELETE, new Runnable() {
            @Override public void run() {
                store.delete(entity(deviceId, uniqueName, image));
            }
        });
    }

    public void getRecentPeers(final RecentPeersListener responseListener) {
        boolean accepted = submit("read-all:" + operationSequence.incrementAndGet(), false,
                Operation.READ_ALL, new Runnable() {
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
                    reportFailure(Operation.READ_ALL, FailureReason.STORE_ERROR);
                }
                dispatchCallback(new Runnable() {
                    @Override public void run() {
                        responseListener.onRecentPeersObtained(new ArrayList<>(peers));
                    }
                });
            }
        });
        if (!accepted) {
            dispatchCallback(new Runnable() {
                @Override public void run() {
                    responseListener.onRecentPeersObtained(new ArrayList<RecentPeer>());
                }
            });
        }
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
        final Operation operation = Operation.READ_ONE;
        boolean accepted = submit("read-one:" + operationSequence.incrementAndGet(), false,
                operation, new Runnable() {
            @Override public void run() {
                RecentPeer value = null;
                try {
                    RecentPeerEntity entity = byName ? store.loadByName(key) : store.loadById(key);
                    if (entity != null) {
                        value = entity.getRecentPeer();
                    }
                } catch (RuntimeException error) {
                    reportFailure(operation, FailureReason.STORE_ERROR);
                }
                final RecentPeer result = value;
                dispatchCallback(new Runnable() {
                    @Override public void run() {
                        responseListener.onRecentPeerObtained(result);
                    }
                });
            }
        });
        if (!accepted) {
            dispatchCallback(new Runnable() {
                @Override public void run() { responseListener.onRecentPeerObtained(null); }
            });
        }
    }

    public interface RecentPeerListener {
        void onRecentPeerObtained(@Nullable RecentPeer recentPeer);
    }

    private static String normalize(@Nullable String value) {
        return value == null ? "" : value;
    }

    private static String operationKey(String operation, String... values) {
        StringBuilder key = new StringBuilder(operation);
        for (String value : values) {
            String normalized = normalize(value);
            key.append(':').append(normalized.length()).append('#').append(normalized);
        }
        return key.toString();
    }

    private void submitWrite(String key, final Operation operation, final Runnable task) {
        submit(key, true, operation, new Runnable() {
            @Override public void run() {
                try {
                    task.run();
                } catch (RuntimeException error) {
                    reportFailure(operation, FailureReason.STORE_ERROR);
                }
            }
        });
    }

    private boolean submit(String key, boolean coalesce, Operation operation, Runnable task) {
        BoundedSerialExecutor.SubmitResult result = databaseExecutor.submit(key, coalesce, task);
        if (result == BoundedSerialExecutor.SubmitResult.REJECTED) {
            reportFailure(operation, FailureReason.QUEUE_FULL);
            return false;
        }
        if (result == BoundedSerialExecutor.SubmitResult.CLOSED) {
            reportFailure(operation, FailureReason.CLOSED);
            return false;
        }
        return true;
    }

    public synchronized void addFailureListener(PersistenceFailureListener listener) {
        if (listener != null && !failureListeners.contains(listener)) {
            failureListeners.add(listener);
        }
    }

    public synchronized void removeFailureListener(PersistenceFailureListener listener) {
        failureListeners.remove(listener);
    }

    private void reportFailure(final Operation operation, final FailureReason reason) {
        final ArrayList<PersistenceFailureListener> snapshot;
        synchronized (this) {
            snapshot = new ArrayList<>(failureListeners);
        }
        if (snapshot.isEmpty()) {
            return;
        }
        dispatchCallback(new Runnable() {
            @Override public void run() {
                PersistenceFailure failure = new PersistenceFailure(operation, reason);
                for (PersistenceFailureListener listener : snapshot) {
                    try {
                        listener.onPersistenceFailure(failure);
                    } catch (RuntimeException ignored) {
                        // One observer cannot prevent other sanitized failure notifications.
                    }
                }
            }
        });
    }

    private void dispatchCallback(Runnable callback) {
        try {
            callbackExecutor.execute(callback);
        } catch (RuntimeException ignored) {
            // The callback owner has gone away; never kill the database worker.
        }
    }

    public void close() {
        int dropped = databaseExecutor.shutdownNow();
        if (dropped > 0) {
            reportFailure(Operation.QUEUE, FailureReason.CLOSED);
        }
    }

    private static byte[] copy(@Nullable byte[] value) {
        return value == null ? null : Arrays.copyOf(value, value.length);
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
