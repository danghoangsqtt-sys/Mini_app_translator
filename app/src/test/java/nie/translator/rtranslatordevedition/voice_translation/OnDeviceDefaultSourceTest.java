package nie.translator.rtranslatordevedition.voice_translation;

import static org.junit.Assert.assertFalse;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import org.junit.Test;

public class OnDeviceDefaultSourceTest {
    @Test public void defaultModeServicesDoNotConstructLegacyCloudOrRecorder() throws Exception {
        assertClean("src/main/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/_conversation/ConversationService.java");
        assertClean("src/main/java/nie/translator/rtranslatordevedition/voice_translation/_walkie_talkie_mode/_walkie_talkie/WalkieTalkieService.java");
    }
    private static void assertClean(String path) throws Exception {
        String source = new String(Files.readAllBytes(new File(path).toPath()), Charset.forName("UTF-8"));
        assertFalse(source.contains("new Translator(")); assertFalse(source.contains("new Recognizer(")); assertFalse(source.contains("new Recorder("));
    }
}
