package nie.translator.rtranslatordevedition;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.Test;

public class BluetoothTransportSecuritySourceTest {
    @Test
    public void activeTransportIsVendoredAndSensitiveTagsAreGone() throws IOException {
        String build = read(new File("build.gradle"));
        assertFalse(build.contains("com.github.niedev:BluetoothCommunicator"));

        File sourceRoot = new File("src/main/java/com/bluetooth/communicator");
        assertTrue(sourceRoot.isDirectory());
        StringBuilder sources = new StringBuilder();
        try (Stream<Path> paths = Files.walk(sourceRoot.toPath())) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> {
                        try {
                            sources.append(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
                        } catch (IOException error) {
                            throw new IllegalStateException(error);
                        }
                    });
        }
        String all = sources.toString();
        assertFalse(all.contains("Log.e(\"messageSend\""));
        assertFalse(all.contains("Log.e(\"dataSend\""));
        assertFalse(all.contains("Log.e(\"clientMessageReceive\""));
        assertFalse(all.contains("Log.e(\"clientDataReceive\""));
        assertTrue(all.contains("MAX_IN_FLIGHT_PER_STREAM = 4"));
        assertTrue(all.contains("INBOUND_TIMEOUT_MS = 10_000L"));
    }

    @Test
    public void releaseRulesStripLogsAndArtifactGateExists() throws IOException {
        String rules = read(new File("proguard-rules.pro"));
        String build = read(new File("build.gradle"));
        assertTrue(rules.contains("-assumenosideeffects class android.util.Log"));
        assertTrue(rules.contains("public void printStackTrace();"));
        assertTrue(build.contains("verifyReleasePrivacy"));
        assertTrue(build.contains("recognizerResultFinal"));
    }

    @Test
    public void peerImagesAreBoundedAndDecodedOffCallbackPath() throws IOException {
        String tools = read(new File("src/main/java/nie/translator/rtranslatordevedition/tools/Tools.java"));
        String communicator = read(new File("src/main/java/nie/translator/rtranslatordevedition/"
                + "voice_translation/_conversation_mode/communication/ConversationBluetoothCommunicator.java"));
        assertTrue(tools.contains("MAX_PEER_IMAGE_ENCODED_BYTES = 192 * 1024"));
        assertTrue(tools.contains("inJustDecodeBounds = true"));
        assertTrue(communicator.contains("new ArrayBlockingQueue<Runnable>(MAX_PENDING_IMAGE_DECODES)"));
        assertTrue(communicator.contains("peerImageExecutor.execute"));
        assertFalse(communicator.contains("Tools.convertBytesToBitmap(data.getData())"));
    }

    private static String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
