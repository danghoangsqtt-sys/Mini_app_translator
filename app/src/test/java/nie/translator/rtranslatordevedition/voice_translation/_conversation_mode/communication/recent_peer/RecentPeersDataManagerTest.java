package nie.translator.rtranslatordevedition.voice_translation._conversation_mode.communication.recent_peer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import nie.translator.rtranslatordevedition.database.entities.RecentPeerEntity;
import org.junit.Test;

public class RecentPeersDataManagerTest {
    @Test public void imageQueuedBeforeIdentityInsertCompletesIsPersisted() {
        FakeStore store = new FakeStore();
        ManualExecutor database = new ManualExecutor();
        RecentPeersDataManager manager = new RecentPeersDataManager(store, database, Runnable::run);
        byte[] image = new byte[] {1, 2, 3, 4};

        manager.upsertRecentPeerIdentity("device", "peer");
        manager.updateRecentPeerImageDataByName("peer", image);

        assertEquals(0, store.rows.size());
        assertEquals(1, database.size());
        database.runAll();
        assertArrayEquals(image, store.rows.get("device").userImage);
    }

    @Test public void identityRefreshPreservesExistingImage() {
        FakeStore store = new FakeStore();
        store.insert(entity("device", "old", new byte[] {9, 8}));
        ManualExecutor database = new ManualExecutor();
        RecentPeersDataManager manager = new RecentPeersDataManager(store, database, Runnable::run);
        manager.upsertRecentPeerIdentity("device", "new");
        database.runAll();
        assertEquals("new", store.rows.get("device").uniqueName);
        assertArrayEquals(new byte[] {9, 8}, store.rows.get("device").userImage);
    }

    @Test public void readsUseCallbackExecutorAndReturnDefensiveLists() {
        FakeStore store = new FakeStore();
        ManualExecutor database = new ManualExecutor();
        ManualExecutor callbacks = new ManualExecutor();
        RecentPeersDataManager manager = new RecentPeersDataManager(store, database, callbacks);
        final ArrayList<RecentPeer>[] first = new ArrayList[1];
        final ArrayList<RecentPeer>[] second = new ArrayList[1];
        manager.getRecentPeers(value -> first[0] = value);
        manager.getRecentPeers(value -> second[0] = value);
        database.runAll();
        assertFalse(callbacks.isEmpty());
        callbacks.runAll();
        assertNotSame(first[0], second[0]);
        first[0].add(null);
        assertTrue(second[0].isEmpty());
    }

    @Test public void imageBurstCoalescesAndPersistsOnlyLatestDefensiveBytes() {
        FakeStore store = new FakeStore();
        ManualExecutor worker = new ManualExecutor();
        BoundedSerialExecutor bounded = new BoundedSerialExecutor(worker, 4);
        RecentPeersDataManager manager = new RecentPeersDataManager(store, bounded, Runnable::run);
        manager.upsertRecentPeerIdentity("device", "peer");
        byte[] latest = null;
        for (int index = 0; index < 100; index++) {
            latest = new byte[] {(byte) index};
            manager.updateRecentPeerImageDataByName("peer", latest);
        }
        latest[0] = 0;

        assertEquals(2, bounded.pendingCount());
        worker.runAll();
        assertArrayEquals(new byte[] {99}, store.rows.get("device").userImage);
    }

    @Test public void overflowIsReportedOnCallbackExecutorWithoutSensitiveData() {
        FakeStore store = new FakeStore();
        ManualExecutor worker = new ManualExecutor();
        ManualExecutor callbacks = new ManualExecutor();
        BoundedSerialExecutor bounded = new BoundedSerialExecutor(worker, 2);
        RecentPeersDataManager manager = new RecentPeersDataManager(store, bounded, callbacks);
        final ArrayList<RecentPeersDataManager.PersistenceFailure> failures = new ArrayList<>();
        manager.addFailureListener(failures::add);

        manager.upsertRecentPeerIdentity("one", "peer-one");
        manager.upsertRecentPeerIdentity("two", "peer-two");
        manager.upsertRecentPeerIdentity("three", "peer-three");

        assertTrue(failures.isEmpty());
        callbacks.runAll();
        assertEquals(1, failures.size());
        assertEquals(RecentPeersDataManager.Operation.IDENTITY, failures.get(0).getOperation());
        assertEquals(RecentPeersDataManager.FailureReason.QUEUE_FULL, failures.get(0).getReason());
    }

    @Test public void storeFailureIsReportedAndFollowingWriteStillRuns() {
        FakeStore store = new FakeStore();
        store.failNextInsert = true;
        ManualExecutor worker = new ManualExecutor();
        ManualExecutor callbacks = new ManualExecutor();
        RecentPeersDataManager manager = new RecentPeersDataManager(store, worker, callbacks);
        final ArrayList<RecentPeersDataManager.PersistenceFailure> failures = new ArrayList<>();
        manager.addFailureListener(failures::add);

        manager.upsertRecentPeerIdentity("bad", "first");
        manager.upsertRecentPeerIdentity("good", "second");
        worker.runAll();
        callbacks.runAll();

        assertEquals(1, failures.size());
        assertEquals(RecentPeersDataManager.FailureReason.STORE_ERROR, failures.get(0).getReason());
        assertTrue(store.rows.containsKey("good"));
    }

    @Test public void closeDropsPendingAndPostCloseWritesFailDeterministically() {
        FakeStore store = new FakeStore();
        ManualExecutor worker = new ManualExecutor();
        ManualExecutor callbacks = new ManualExecutor();
        RecentPeersDataManager manager = new RecentPeersDataManager(store, worker, callbacks);
        final ArrayList<RecentPeersDataManager.PersistenceFailure> failures = new ArrayList<>();
        manager.addFailureListener(failures::add);
        manager.upsertRecentPeerIdentity("device", "peer");

        manager.close();
        manager.updateRecentPeerName("peer", "new-peer");
        worker.runAll();
        callbacks.runAll();

        assertTrue(store.rows.isEmpty());
        assertEquals(2, failures.size());
        assertEquals(RecentPeersDataManager.Operation.QUEUE, failures.get(0).getOperation());
        assertEquals(RecentPeersDataManager.FailureReason.CLOSED, failures.get(0).getReason());
        assertEquals(RecentPeersDataManager.Operation.NAME, failures.get(1).getOperation());
        assertEquals(RecentPeersDataManager.FailureReason.CLOSED, failures.get(1).getReason());
    }

    @Test public void failingObserverCannotHideSanitizedFailureFromOtherObservers() {
        FakeStore store = new FakeStore();
        store.failNextInsert = true;
        ManualExecutor worker = new ManualExecutor();
        RecentPeersDataManager manager = new RecentPeersDataManager(store, worker, Runnable::run);
        final int[] observed = {0};
        manager.addFailureListener(value -> { throw new IllegalStateException("observer"); });
        manager.addFailureListener(value -> {
            observed[0]++;
            assertEquals(RecentPeersDataManager.Operation.IDENTITY, value.getOperation());
            assertEquals(RecentPeersDataManager.FailureReason.STORE_ERROR, value.getReason());
        });

        manager.upsertRecentPeerIdentity("device", "peer");
        worker.runAll();

        assertEquals(1, observed[0]);
    }

    private static RecentPeerEntity entity(String id, String name, byte[] image) {
        RecentPeerEntity entity = new RecentPeerEntity();
        entity.deviceId = id;
        entity.uniqueName = name;
        entity.userImage = image;
        return entity;
    }

    private static final class ManualExecutor implements Executor {
        private final ArrayDeque<Runnable> tasks = new ArrayDeque<>();
        @Override public void execute(Runnable command) { tasks.add(command); }
        int size() { return tasks.size(); }
        boolean isEmpty() { return tasks.isEmpty(); }
        void runAll() { while (!tasks.isEmpty()) { tasks.remove().run(); } }
    }

    private static final class FakeStore implements RecentPeersDataManager.Store {
        private final Map<String, RecentPeerEntity> rows = new LinkedHashMap<>();
        private boolean failNextInsert;
        @Override public void insert(RecentPeerEntity entity) {
            if (failNextInsert) {
                failNextInsert = false;
                throw new IllegalStateException("database unavailable");
            }
            rows.put(entity.deviceId, entity);
        }
        @Override public void delete(RecentPeerEntity entity) { rows.remove(entity.deviceId); }
        @Override public RecentPeerEntity[] loadAll() { return rows.values().toArray(new RecentPeerEntity[0]); }
        @Override public RecentPeerEntity loadById(String deviceId) { return rows.get(deviceId); }
        @Override public RecentPeerEntity loadByName(String uniqueName) {
            for (RecentPeerEntity entity : rows.values()) {
                if (uniqueName.equals(entity.uniqueName)) return entity;
            }
            return null;
        }
    }
}
