package nie.translator.rtranslatordevedition.voice_translation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;

/** Guards the Fragment-to-Activity ownership contract around asynchronous bind initiation. */
public class ServiceConnectionOwnershipSourceTest {
    @Test public void bothFragmentsOwnAndCancelReturnedHandleWithoutPlaceholderIds() throws Exception {
        String conversation = read("src/main/java/nie/translator/rtranslatordevedition/voice_translation/"
                + "_conversation_mode/_conversation/main/ConversationMainFragment.java");
        String walkie = read("src/main/java/nie/translator/rtranslatordevedition/voice_translation/"
                + "_walkie_talkie_mode/_walkie_talkie/WalkieTalkieFragment.java");

        assertTrue(conversation.contains("serviceConnectionHandle = activity.connectToConversationService("));
        assertTrue(walkie.contains("serviceConnectionHandle = activity.connectToWalkieTalkieService("));
        assertTrue(conversation.contains("serviceConnectionHandle.cancel();"));
        assertTrue(walkie.contains("serviceConnectionHandle.cancel();"));
        assertFalse(conversation.contains("ConversationServiceCommunicator(0)"));
        assertFalse(walkie.contains("WalkieTalkieServiceCommunicator(0)"));
        assertFalse(conversation.contains("disconnectFromConversationService"));
        assertFalse(walkie.contains("disconnectFromWalkieTalkieService"));
    }

    @Test public void activityChecksCancellationAcrossStartBindAndDeliveryOwnership() throws Exception {
        String activity = read("src/main/java/nie/translator/rtranslatordevedition/"
                + "voice_translation/VoiceTranslationActivity.java");

        assertTrue(count(activity, "handle.isCancelled()") >= 6);
        assertTrue(count(activity, "handle.markServiceStarted()") == 2);
        assertTrue(count(activity, "handle.attach(") == 2);
        assertTrue(activity.contains("stopConversationServiceIfUnused()"));
        assertTrue(activity.contains("stopWalkieTalkieServiceIfUnused()"));
        assertFalse(activity.contains("disconnectFromConversationService("));
        assertFalse(activity.contains("disconnectFromWalkieTalkieService("));
    }

    private static int count(String source, String needle) {
        int result = 0;
        int offset = 0;
        while ((offset = source.indexOf(needle, offset)) >= 0) {
            result++;
            offset += needle.length();
        }
        return result;
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8);
    }
}
