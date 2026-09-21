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

package nie.translator.rtranslatordevedition.voice_translation._conversation_mode._conversation;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConversationServiceReconnectSourceTest {
    @Test
    public void serviceDestroysItsReconnectOwnerBeforeBluetoothTeardown() throws IOException {
        File serviceFile = new File("src/main/java/nie/translator/rtranslatordevedition/"
                + "voice_translation/_conversation_mode/_conversation/ConversationService.java");
        String source = new String(Files.readAllBytes(serviceFile.toPath()), Charset.forName("UTF-8"));

        int onDestroy = source.indexOf("public void onDestroy()");
        int lifecycleDestroyed = source.indexOf("scoReconnectCoordinator.destroy();", onDestroy);
        int helperStopped = source.indexOf("mBluetoothHelper.stop();", onDestroy);
        assertTrue("destroy lifecycle state must precede helper teardown",
                lifecycleDestroyed >= 0 && lifecycleDestroyed < helperStopped);
        assertTrue(source.contains("scoReconnectCoordinator.scheduleReconnect(1000);"));
        assertFalse(source.contains("static Handler mHandler"));
        assertFalse(source.contains("removeCallbacksAndMessages(null)"));
    }
}
