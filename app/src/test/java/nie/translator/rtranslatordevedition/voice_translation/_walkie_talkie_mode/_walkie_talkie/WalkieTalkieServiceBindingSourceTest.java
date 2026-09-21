/*
 * Copyright 2016 Luca Martino.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copyFile of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nie.translator.rtranslatordevedition.voice_translation._walkie_talkie_mode._walkie_talkie;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WalkieTalkieServiceBindingSourceTest {
    private static final File SERVICE_SOURCE = new File(
            "src/main/java/nie/translator/rtranslatordevedition/voice_translation/"
                    + "_walkie_talkie_mode/_walkie_talkie/WalkieTalkieService.java");

    @Test
    public void destroyTracksBindAttemptsInsteadOfUnbindingBothConnectionsUnconditionally()
            throws IOException {
        assertTrue("WalkieTalkieService source must exist", SERVICE_SOURCE.isFile());

        String source = readFile(SERVICE_SOURCE);

        assertFalse("first connection must not be unbound without an attempt check",
                source.contains("unbindService(firstLanguageConnection);"));
        assertFalse("second connection must not be unbound without an attempt check",
                source.contains("unbindService(secondLanguageConnection);"));
        assertAttemptIsRecordedBeforeBind(source,
                "firstLanguageBindingAttempts.recordBindAttempt();",
                "bindService(intent, firstLanguageConnection, Service.BIND_AUTO_CREATE);");
        assertAttemptIsRecordedBeforeBind(source,
                "secondLanguageBindingAttempts.recordBindAttempt();",
                "bindService(intent, secondLanguageConnection, Service.BIND_AUTO_CREATE);");
        assertCleanupContinuesToTheParentService(source);
    }

    private static void assertAttemptIsRecordedBeforeBind(String source, String recordAttempt,
                                                           String bind) {
        int recordPosition = source.indexOf(recordAttempt);
        int bindPosition = source.indexOf(bind);
        assertTrue("Every bindService call must first record its attempt", recordPosition >= 0);
        assertTrue("Every bindService call must first record its attempt",
                bindPosition >= 0 && recordPosition < bindPosition);
    }

    private static void assertCleanupContinuesToTheParentService(String source) {
        int firstStop = source.indexOf("stopFirstLanguageCommunication();");
        int secondStop = source.indexOf("stopSecondLanguageCommunication();");
        int firstRelease = source.indexOf("releaseBindings(firstLanguageBindingAttempts");
        int secondRelease = source.indexOf("releaseBindings(secondLanguageBindingAttempts");
        int parentDestroy = source.indexOf("super.onDestroy();");

        assertTrue("onDestroy must stop both communicators before releasing bindings",
                firstStop >= 0 && secondStop > firstStop);
        assertTrue("onDestroy must release both connection attempt sets",
                firstRelease > secondStop && secondRelease > firstRelease);
        assertTrue("onDestroy must always continue to the parent cleanup after local cleanup",
                parentDestroy > secondRelease);
    }

    private static String readFile(File source) throws IOException {
        FileInputStream input = new FileInputStream(source);
        try {
            StringBuilder content = new StringBuilder();
            byte[] buffer = new byte[1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                content.append(new String(buffer, 0, count, "UTF-8"));
            }
            return content.toString();
        } finally {
            input.close();
        }
    }
}
