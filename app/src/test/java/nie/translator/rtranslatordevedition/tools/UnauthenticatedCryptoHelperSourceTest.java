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

package nie.translator.rtranslatordevedition.tools;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;

public class UnauthenticatedCryptoHelperSourceTest {
    private static final File PRODUCTION_SOURCE = new File("src/main/java");

    @Test
    public void productionJavaDoesNotContainLegacyUnauthenticatedCrypto() throws Exception {
        List<File> sourceFiles = new ArrayList<File>();
        collectJavaFiles(PRODUCTION_SOURCE, sourceFiles);

        for (File sourceFile : sourceFiles) {
            String source = new String(Files.readAllBytes(sourceFile.toPath()),
                    StandardCharsets.UTF_8);

            assertFalse(sourceFile + " must not use AES/CTR", source.contains("AES/CTR"));
            assertFalse(sourceFile + " must not declare CipherData",
                    source.contains("class CipherData"));
            assertFalse(sourceFile + " must not declare encript", source.contains("encript("));
            assertFalse(sourceFile + " must not declare decript", source.contains("decript("));
            assertFalse(sourceFile + " must not declare decriptToString",
                    source.contains("decriptToString"));
        }
    }

    private static void collectJavaFiles(File directory, List<File> sourceFiles) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                collectJavaFiles(file, sourceFiles);
            } else if (file.getName().endsWith(".java")) {
                sourceFiles.add(file);
            }
        }
    }
}
