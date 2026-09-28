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
        assertEquals(2, database.size());
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
        @Override public void insert(RecentPeerEntity entity) { rows.put(entity.deviceId, entity); }
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
