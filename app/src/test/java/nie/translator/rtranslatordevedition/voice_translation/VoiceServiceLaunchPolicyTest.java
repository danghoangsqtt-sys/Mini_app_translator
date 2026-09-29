package nie.translator.rtranslatordevedition.voice_translation;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VoiceServiceLaunchPolicyTest {
    @Test
    public void microphonePermissionIsRequiredForBothModes() {
        assertEquals(VoiceTranslationService.MISSING_MIC_PERMISSION,
                VoiceTranslationActivity.resolveVoiceServicePermissionError(false, false, false));
        assertEquals(VoiceTranslationService.MISSING_MIC_PERMISSION,
                VoiceTranslationActivity.resolveVoiceServicePermissionError(false, true, true));
    }

    @Test
    public void bluetoothConnectIsRequiredOnlyForConversation() {
        assertEquals(0,
                VoiceTranslationActivity.resolveVoiceServicePermissionError(true, false, false));
        assertEquals(VoiceTranslationService.MISSING_NEARBY_PERMISSION,
                VoiceTranslationActivity.resolveVoiceServicePermissionError(true, true, false));
        assertEquals(0,
                VoiceTranslationActivity.resolveVoiceServicePermissionError(true, true, true));
    }

    @Test
    public void permissionGuardPrecedesEveryForegroundServiceStart() throws Exception {
        String activity = read("src/main/java/nie/translator/rtranslatordevedition/"
                + "voice_translation/VoiceTranslationActivity.java");

        assertGuardBeforeStart(activity, "private void startConversationService", true);
        assertGuardBeforeStart(activity, "private void startWalkieTalkieService", false);

        String conversation = read("src/main/java/nie/translator/rtranslatordevedition/"
                + "voice_translation/_conversation_mode/_conversation/main/ConversationMainFragment.java");
        String walkie = read("src/main/java/nie/translator/rtranslatordevedition/"
                + "voice_translation/_walkie_talkie_mode/_walkie_talkie/WalkieTalkieFragment.java");
        String fragment = read("src/main/java/nie/translator/rtranslatordevedition/"
                + "voice_translation/VoiceTranslationFragment.java");
        assertTrue(conversation.contains("if (!prepareVoiceServiceConnection())"));
        assertTrue(walkie.contains("if (!prepareVoiceServiceConnection())"));
        assertTrue(fragment.contains("if (voiceTranslationServiceCommunicator != null)"));
        assertTrue(fragment.contains("VoiceTranslationActivity.PAIRING_FRAGMENT"));
    }

    private static void assertGuardBeforeStart(String source, String method, boolean bluetoothRequired) {
        int methodStart = source.indexOf(method);
        int permissionGuard = source.indexOf("getVoiceServicePermissionError(" + bluetoothRequired + ")", methodStart);
        int serviceStart = source.indexOf("ContextCompat.startForegroundService(", methodStart);
        assertTrue(methodStart >= 0);
        assertTrue(permissionGuard > methodStart);
        assertTrue(serviceStart > permissionGuard);
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8);
    }
}
