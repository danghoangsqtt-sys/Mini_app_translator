package nie.translator.rtranslatordevedition.api_management;

import android.content.Context;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class CredentialStoreInstrumentedTest {
    private CredentialStore store;
    private File legacyFile;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getTargetContext();
        String suffix = UUID.randomUUID().toString();
        store = new CredentialStore(context,
                context.getPackageName() + ".credential_test." + suffix,
                "credential_test_" + suffix + ".bin");
        legacyFile = new File(context.getFilesDir(), "legacy_credential_" + suffix + ".json");
    }

    @After
    public void tearDown() throws IOException {
        store.deleteCredential(legacyFile);
    }

    @Test
    public void importCredential_encryptsAtRestAndDecryptsOnRead() throws Exception {
        byte[] credential = sampleCredential();
        writeFile(legacyFile, credential);

        store.importCredential(legacyFile);

        byte[] persisted = readFile(store.getStorageFileForTesting());
        String persistedText = new String(persisted, StandardCharsets.UTF_8);
        assertFalse(persistedText.contains("private_key"));
        assertFalse(persistedText.contains("client_email"));
        assertArrayEquals(credential, readAll(store.openCredential(null)));
    }

    @Test
    public void openCredential_migratesAndDeletesLegacyPlaintext() throws Exception {
        byte[] credential = sampleCredential();
        writeFile(legacyFile, credential);

        assertArrayEquals(credential, readAll(store.openCredential(legacyFile)));

        assertTrue(store.getStorageFileForTesting().isFile());
        assertFalse(legacyFile.exists());
    }

    @Test
    public void deleteCredential_removesEncryptedAndLegacyFiles() throws Exception {
        writeFile(legacyFile, sampleCredential());
        store.importCredential(legacyFile);

        store.deleteCredential(legacyFile);

        assertFalse(store.getStorageFileForTesting().exists());
        assertFalse(legacyFile.exists());
    }

    private static byte[] sampleCredential() {
        return ("{\"type\":\"service_account\",\"client_email\":\"test@example.invalid\"," +
                "\"private_key\":\"-----BEGIN PRIVATE KEY-----fake-----END PRIVATE KEY-----\"}")
                .getBytes(StandardCharsets.UTF_8);
    }

    private static void writeFile(File file, byte[] bytes) throws IOException {
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(bytes);
            output.getFD().sync();
        }
    }

    private static byte[] readFile(File file) throws IOException {
        return readAll(new FileInputStream(file));
    }

    private static byte[] readAll(InputStream input) throws IOException {
        try (InputStream source = input;
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int count;
            while ((count = source.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }
}
