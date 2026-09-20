/*
 * Copyright 2016 Luca Martino.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nie.translator.rtranslatordevedition.api_management;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * The only persistence boundary for the user-supplied Google service-account credential.
 */
public final class CredentialStore {
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String DEFAULT_KEY_ALIAS = "nie.translator.rtranslatordevedition.gcp_credentials";
    private static final String DEFAULT_STORAGE_FILE = "gcp_credentials.v1";
    private static final byte[] MAGIC = new byte[]{'M', 'C', 'R', 'E', 'D', 1};
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int MAX_CREDENTIAL_SIZE = 1024 * 1024;
    private static final int MAX_IV_SIZE = 32;
    private static final int MAX_CIPHERTEXT_SIZE = MAX_CREDENTIAL_SIZE + 64;

    private final Context context;
    private final String keyAlias;
    private final File storageFile;

    public CredentialStore(Context context) {
        this(context, DEFAULT_KEY_ALIAS, DEFAULT_STORAGE_FILE);
    }

    CredentialStore(Context context, String keyAlias, String storageFileName) {
        this.context = context.getApplicationContext();
        this.keyAlias = keyAlias;
        this.storageFile = new File(this.context.getFilesDir(), storageFileName);
    }

    public synchronized void importCredential(File source) throws IOException {
        if (source == null || !source.isFile()) {
            throw new FileNotFoundException("Credential source is not a readable file");
        }
        try (InputStream input = new FileInputStream(source)) {
            writeEncrypted(readBounded(input, MAX_CREDENTIAL_SIZE));
        }
    }

    /**
     * Opens the decrypted credential and migrates a legacy plaintext file when necessary.
     * The legacy file is deleted only after the encrypted replacement has been persisted.
     */
    public synchronized InputStream openCredential(File legacyPlaintextFile) throws IOException {
        migrateLegacyCredential(legacyPlaintextFile);
        if (!storageFile.isFile()) {
            throw new FileNotFoundException("No encrypted credential is stored");
        }

        try {
            return new WipingByteArrayInputStream(decryptEnvelope(readBounded(
                    new FileInputStream(storageFile), MAX_CIPHERTEXT_SIZE + 64)));
        } catch (GeneralSecurityException e) {
            throw new IOException("Stored credential could not be decrypted", e);
        }
    }

    public synchronized void migrateLegacyCredential(File legacyPlaintextFile) throws IOException {
        if (legacyPlaintextFile == null || !legacyPlaintextFile.isFile()
                || legacyPlaintextFile.equals(storageFile)) {
            return;
        }

        if (!storageFile.isFile()) {
            importCredential(legacyPlaintextFile);
        }

        if (!legacyPlaintextFile.delete() && legacyPlaintextFile.exists()) {
            throw new IOException("Encrypted credential was saved, but legacy plaintext cleanup failed");
        }
    }

    public synchronized void deleteCredential(File legacyPlaintextFile) throws IOException {
        File temporaryFile = new File(storageFile.getParentFile(), storageFile.getName() + ".tmp");
        File backupFile = new File(storageFile.getParentFile(), storageFile.getName() + ".bak");
        boolean encryptedDeleted = !storageFile.exists() || storageFile.delete();
        boolean temporaryDeleted = !temporaryFile.exists() || temporaryFile.delete();
        boolean backupDeleted = !backupFile.exists() || backupFile.delete();
        boolean legacyDeleted = legacyPlaintextFile == null || !legacyPlaintextFile.exists()
                || legacyPlaintextFile.equals(storageFile) || legacyPlaintextFile.delete();

        try {
            KeyStore keyStore = loadKeyStore();
            if (keyStore.containsAlias(keyAlias)) {
                keyStore.deleteEntry(keyAlias);
            }
        } catch (GeneralSecurityException e) {
            throw new IOException("Credential encryption key could not be deleted", e);
        }

        if (!encryptedDeleted || !temporaryDeleted || !backupDeleted || !legacyDeleted) {
            throw new IOException("Credential files could not be deleted");
        }
    }

    File getStorageFileForTesting() {
        return storageFile;
    }

    private void writeEncrypted(byte[] plaintext) throws IOException {
        final byte[] iv;
        final byte[] ciphertext;
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
            cipher.updateAAD(getAssociatedData());
            iv = cipher.getIV();
            ciphertext = cipher.doFinal(plaintext);
        } catch (GeneralSecurityException e) {
            throw new IOException("Credential could not be encrypted", e);
        } finally {
            Arrays.fill(plaintext, (byte) 0);
        }

        File temporaryFile = new File(storageFile.getParentFile(), storageFile.getName() + ".tmp");
        if (temporaryFile.exists() && !temporaryFile.delete()) {
            throw new IOException("Stale credential temporary file could not be removed");
        }

        boolean written = false;
        try (FileOutputStream fileOutput = new FileOutputStream(temporaryFile);
             DataOutputStream output = new DataOutputStream(fileOutput)) {
            output.write(MAGIC);
            output.writeByte(iv.length);
            output.writeInt(ciphertext.length);
            output.write(iv);
            output.write(ciphertext);
            output.flush();
            fileOutput.getFD().sync();
            written = true;
        } finally {
            Arrays.fill(ciphertext, (byte) 0);
            if (!written) {
                temporaryFile.delete();
            }
        }

        File backupFile = new File(storageFile.getParentFile(), storageFile.getName() + ".bak");
        if (backupFile.exists() && !backupFile.delete()) {
            temporaryFile.delete();
            throw new IOException("Stale credential backup could not be removed");
        }

        boolean previousMoved = storageFile.exists() && storageFile.renameTo(backupFile);
        if (storageFile.exists() && !previousMoved) {
            temporaryFile.delete();
            throw new IOException("Previous encrypted credential could not be preserved");
        }
        if (!temporaryFile.renameTo(storageFile)) {
            if (previousMoved) {
                backupFile.renameTo(storageFile);
            }
            temporaryFile.delete();
            throw new IOException("Encrypted credential could not be committed");
        }
        // A stale backup is still encrypted and is retried during the next write/delete.
        backupFile.delete();
    }

    private byte[] decryptEnvelope(byte[] envelope) throws IOException, GeneralSecurityException {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(envelope))) {
            byte[] magic = new byte[MAGIC.length];
            input.readFully(magic);
            if (!Arrays.equals(MAGIC, magic)) {
                throw new IOException("Unsupported credential envelope");
            }

            int ivLength = input.readUnsignedByte();
            int ciphertextLength = input.readInt();
            if (ivLength < 12 || ivLength > MAX_IV_SIZE
                    || ciphertextLength <= GCM_TAG_LENGTH_BITS / 8
                    || ciphertextLength > MAX_CIPHERTEXT_SIZE) {
                throw new IOException("Malformed credential envelope");
            }

            byte[] iv = new byte[ivLength];
            byte[] ciphertext = new byte[ciphertextLength];
            input.readFully(iv);
            input.readFully(ciphertext);
            if (input.read() != -1) {
                throw new IOException("Credential envelope contains trailing data");
            }

            try {
                Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
                cipher.init(Cipher.DECRYPT_MODE, getExistingKey(),
                        new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
                cipher.updateAAD(getAssociatedData());
                byte[] plaintext = cipher.doFinal(ciphertext);
                if (plaintext.length > MAX_CREDENTIAL_SIZE) {
                    Arrays.fill(plaintext, (byte) 0);
                    throw new IOException("Decrypted credential exceeds the size limit");
                }
                return plaintext;
            } finally {
                Arrays.fill(ciphertext, (byte) 0);
            }
        } catch (EOFException e) {
            throw new IOException("Credential envelope is truncated", e);
        } finally {
            Arrays.fill(envelope, (byte) 0);
        }
    }

    private SecretKey getOrCreateKey() throws GeneralSecurityException, IOException {
        KeyStore keyStore = loadKeyStore();
        Key existingKey = keyStore.getKey(keyAlias, null);
        if (existingKey instanceof SecretKey) {
            return (SecretKey) existingKey;
        }

        KeyGenerator generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
        generator.init(new KeyGenParameterSpec.Builder(
                keyAlias, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generator.generateKey();
    }

    private SecretKey getExistingKey() throws GeneralSecurityException, IOException {
        Key key = loadKeyStore().getKey(keyAlias, null);
        if (!(key instanceof SecretKey)) {
            throw new GeneralSecurityException("Credential encryption key is unavailable");
        }
        return (SecretKey) key;
    }

    private KeyStore loadKeyStore() throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);
        return keyStore;
    }

    private byte[] getAssociatedData() {
        return ("MiniConversationCredentialStore:v1:" + context.getPackageName())
                .getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] readBounded(InputStream input, int maximumSize) throws IOException {
        try (InputStream boundedInput = input;
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int count;
            while ((count = boundedInput.read(buffer)) != -1) {
                total += count;
                if (total > maximumSize) {
                    throw new IOException("Credential exceeds the size limit");
                }
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }

    private static final class WipingByteArrayInputStream extends ByteArrayInputStream {
        WipingByteArrayInputStream(byte[] buffer) {
            super(buffer);
        }

        @Override
        public void close() throws IOException {
            Arrays.fill(buf, (byte) 0);
            super.close();
        }
    }
}
