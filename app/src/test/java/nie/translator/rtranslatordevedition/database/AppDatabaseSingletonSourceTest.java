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

package nie.translator.rtranslatordevedition.database;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AppDatabaseSingletonSourceTest {
    private static final File APP_DATABASE =
            new File("src/main/java/nie/translator/rtranslatordevedition/database/AppDatabase.java");
    private static final File CONSUMPTIONS_MANAGER =
            new File("src/main/java/nie/translator/rtranslatordevedition/api_management/ConsumptionsDataManager.java");
    private static final File RECENT_PEERS_MANAGER =
            new File("src/main/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/communication/recent_peer/RecentPeersDataManager.java");

    @Test
    public void productionCode_centralizesRoomBuilderInAppDatabase() throws Exception {
        String appDatabase = read(APP_DATABASE);

        assertEquals("Only AppDatabase may create the consumption database", 1,
                countOccurrences(appDatabase, "Room.databaseBuilder"));
        assertTrue(read(CONSUMPTIONS_MANAGER).contains("AppDatabase.getInstance(context)"));
        assertTrue(read(RECENT_PEERS_MANAGER).contains("AppDatabase.getInstance(context)"));
    }

    private static String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private static int countOccurrences(String content, String value) {
        int count = 0;
        int position = 0;
        while ((position = content.indexOf(value, position)) >= 0) {
            count++;
            position += value.length();
        }
        return count;
    }
}
