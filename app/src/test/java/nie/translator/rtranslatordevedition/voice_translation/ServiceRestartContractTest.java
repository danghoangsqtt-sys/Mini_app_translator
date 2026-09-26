package nie.translator.rtranslatordevedition.voice_translation;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertTrue;

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

    private static String readFile(String fileName) throws Exception {
        return new String(Files.readAllBytes(new File(fileName).toPath()), StandardCharsets.UTF_8);
    }
}
