package nie.translator.rtranslatordevedition.voice_translation;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class ServiceRestartContractTest {
    @Test
    public void baseVoiceServiceStopsNullRestartBeforeReadingNotification() throws Exception {
        String source = readFile("src/main/java/nie/translator/rtranslatordevedition/"
                + "voice_translation/VoiceTranslationService.java");

        int nullGuard = source.indexOf("if (intent == null)");
        int stop = source.indexOf("stopSelf(startId);", nullGuard);
        int nonSticky = source.indexOf("return START_NOT_STICKY;", stop);
        int notificationRead = source.indexOf("intent.getParcelableExtra", nullGuard);

        assertTrue(nullGuard >= 0);
        assertTrue(stop > nullGuard);
        assertTrue(nonSticky > stop);
        assertTrue(notificationRead > nonSticky);
    }

    @Test
    public void walkieServiceDelegatesNullRestartBeforeReadingLanguages() throws Exception {
        String source = readFile("src/main/java/nie/translator/rtranslatordevedition/"
                + "voice_translation/_walkie_talkie_mode/_walkie_talkie/"
                + "WalkieTalkieService.java");

        int nullGuard = source.indexOf("if (intent == null)");
        int parent = source.indexOf("return super.onStartCommand(null, flags, startId);", nullGuard);
        int languageRead = source.indexOf("intent.getSerializableExtra", nullGuard);

        assertTrue(nullGuard >= 0);
        assertTrue(parent > nullGuard);
        assertTrue(languageRead > parent);
    }

    @Test
    public void activityUsesForegroundServiceLaunchForBothModes() throws Exception {
        String source = readFile("src/main/java/nie/translator/rtranslatordevedition/"
                + "voice_translation/VoiceTranslationActivity.java");

        assertTrue(count(source, "ContextCompat.startForegroundService(") == 2);
        assertFalse(source.contains("if (startService(intent) != null)"));
    }

    @Test
    public void servicePromotesImmediatelyAndStaysForegroundAcrossBinding() throws Exception {
        String source = readFile("src/main/java/nie/translator/rtranslatordevedition/"
                + "voice_translation/VoiceTranslationService.java");

        int onStart = source.indexOf("public int onStartCommand");
        int notificationRead = source.indexOf("intent.getParcelableExtra", onStart);
        int promotion = source.indexOf("if (!promoteToForeground())", notificationRead);
        int nonSticky = source.indexOf("return START_NOT_STICKY;", promotion);
        int onRebind = source.indexOf("public void onRebind");
        int rebindEnd = source.indexOf("}", onRebind);

        assertTrue(onStart >= 0 && notificationRead > onStart);
        assertTrue(promotion > notificationRead && nonSticky > promotion);
        assertFalse(source.substring(onRebind, rebindEnd).contains("stopForeground"));
        assertTrue(source.contains("startForeground(11, notification, getForegroundServiceTypes())"));
        assertTrue(source.contains("requiresBluetoothConnectForForeground()"));
    }

    @Test
    public void conversationAndWalkieUseMinimumForegroundTypes() throws Exception {
        String conversation = readFile("src/main/java/nie/translator/rtranslatordevedition/"
                + "voice_translation/_conversation_mode/_conversation/ConversationService.java");
        String walkie = readFile("src/main/java/nie/translator/rtranslatordevedition/"
                + "voice_translation/_walkie_talkie_mode/_walkie_talkie/WalkieTalkieService.java");

        assertTrue(conversation.contains("FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE"));
        assertTrue(conversation.contains("requiresBluetoothConnectForForeground()"));
        assertFalse(walkie.contains("FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE"));
    }

    private static int count(String value, String needle) {
        int matches = 0;
        int offset = 0;
        while ((offset = value.indexOf(needle, offset)) >= 0) {
            matches++;
            offset += needle.length();
        }
        return matches;
    }

    private static String readFile(String fileName) throws Exception {
        return new String(Files.readAllBytes(new File(fileName).toPath()), StandardCharsets.UTF_8);
    }
}
