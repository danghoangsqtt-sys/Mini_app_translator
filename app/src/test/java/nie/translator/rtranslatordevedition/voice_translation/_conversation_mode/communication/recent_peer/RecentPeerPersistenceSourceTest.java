package nie.translator.rtranslatordevedition.voice_translation._conversation_mode.communication.recent_peer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;

public class RecentPeerPersistenceSourceTest {
    @Test public void productionQueueAndFailurePropagationRemainBoundedAndObservable()
            throws Exception {
        String manager = read("src/main/java/nie/translator/rtranslatordevedition/"
                + "voice_translation/_conversation_mode/communication/recent_peer/"
                + "RecentPeersDataManager.java");
        String communicator = read("src/main/java/nie/translator/rtranslatordevedition/"
                + "voice_translation/_conversation_mode/communication/"
                + "ConversationBluetoothCommunicator.java");

        assertTrue(manager.contains("MAX_PENDING_DATABASE_TASKS = 32"));
        assertTrue(manager.contains("new ArrayBlockingQueue<Runnable>(1)"));
        assertFalse(manager.contains("Executors.newSingleThreadExecutor"));
        assertTrue(manager.contains("FailureReason.QUEUE_FULL"));
        assertTrue(manager.contains("FailureReason.STORE_ERROR"));
        assertTrue(communicator.contains("addFailureListener(persistenceFailureListener)"));
        assertTrue(communicator.contains("removeFailureListener(persistenceFailureListener)"));
        assertTrue(communicator.contains("onRecentPeerPersistenceFailure(operation, reason)"));
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8);
    }
}
