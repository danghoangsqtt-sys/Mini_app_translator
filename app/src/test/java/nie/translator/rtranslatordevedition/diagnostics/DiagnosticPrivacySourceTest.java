package nie.translator.rtranslatordevedition.diagnostics;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.Test;

public class DiagnosticPrivacySourceTest {
    @Test
    public void diagnosticsNeverReadRawExitOrStableDeviceIdentity() throws IOException {
        String sources = readTree(new File("src/main/java/nie/translator/"
                + "rtranslatordevedition/diagnostics"));

        assertFalse(sources.contains("getDescription("));
        assertFalse(sources.contains("getTraceInputStream("));
        assertFalse(sources.contains("getProcessName("));
        assertFalse(sources.contains("getPss("));
        assertFalse(sources.contains("getRss("));
        assertFalse(sources.contains("Build.MODEL"));
        assertFalse(sources.contains("Build.SERIAL"));
        assertFalse(sources.contains("ANDROID_ID"));
        assertFalse(sources.contains("printStackTrace("));
        assertFalse(sources.contains("getMessage("));
        assertTrue(sources.contains("MAX_PROCESS_SUMMARY_BYTES = 128"));
        assertTrue(sources.contains("MAX_REPORT_BYTES = 8 * 1024"));
    }

    @Test
    public void scopedPrintStackTraceHandlersAreRemoved() throws IOException {
        String generalService = read(new File("src/main/java/nie/translator/"
                + "rtranslatordevedition/GeneralService.java"));
        String communicator = read(new File("src/main/java/nie/translator/"
                + "rtranslatordevedition/tools/services_communication/ServiceCommunicator.java"));
        String settings = read(new File("src/main/java/nie/translator/"
                + "rtranslatordevedition/settings/SettingsFragment.java"));
        String api = read(new File("src/main/java/nie/translator/"
                + "rtranslatordevedition/api_management/ApiManagementFragment.java"));
        String diagnostics = read(new File("src/main/java/nie/translator/"
                + "rtranslatordevedition/diagnostics/AppDiagnostics.java"));

        assertFalse(generalService.contains("printStackTrace("));
        assertFalse(communicator.contains("printStackTrace("));
        assertFalse(settings.contains("printStackTrace("));
        assertFalse(api.contains("printStackTrace("));
        assertTrue(settings.contains("AppDiagnostics.newExportIntent()"));
        assertTrue(diagnostics.contains("Intent.ACTION_CREATE_DOCUMENT"));
    }

    @Test
    public void manifestAddsNoDiagnosticStorageOrLogPermission() throws IOException {
        String manifest = read(new File("src/main/AndroidManifest.xml"));
        assertFalse(manifest.contains("android.permission.READ_LOGS"));
        assertFalse(manifest.contains("android.permission.WRITE_EXTERNAL_STORAGE"));
        assertFalse(manifest.contains("android.permission.MANAGE_EXTERNAL_STORAGE"));
    }

    private static String readTree(File root) throws IOException {
        StringBuilder output = new StringBuilder();
        try (Stream<Path> paths = Files.walk(root.toPath())) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .forEach(path -> {
                        try {
                            output.append(new String(Files.readAllBytes(path),
                                    StandardCharsets.UTF_8));
                        } catch (IOException error) {
                            throw new IllegalStateException(error);
                        }
                    });
        }
        return output.toString();
    }

    private static String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
