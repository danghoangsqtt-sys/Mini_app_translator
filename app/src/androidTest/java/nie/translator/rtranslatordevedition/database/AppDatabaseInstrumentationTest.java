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

import android.content.Context;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import nie.translator.rtranslatordevedition.database.dao.MyDao;
import nie.translator.rtranslatordevedition.database.entities.Hour;
import nie.translator.rtranslatordevedition.database.entities.RecentPeerEntity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

@RunWith(AndroidJUnit4.class)
public class AppDatabaseInstrumentationTest {
    private static final int TEST_YEAR = 2099;
    private static final int TEST_MONTH = 12;
    private static final int TEST_DAY = 31;
    private static final int TEST_HOUR = 23;
    private static final String TEST_DEVICE_ID = "vp-room-singleton-test";

    private Context context;
    private AppDatabase database;
    private MyDao dao;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getTargetContext();
        database = AppDatabase.getInstance(context);
        dao = database.myDao();
        dao.deleteHour(TEST_YEAR, TEST_MONTH, TEST_DAY, TEST_HOUR);
        RecentPeerEntity existingPeer = dao.loadRecentPeer(TEST_DEVICE_ID);
        if (existingPeer != null) {
            dao.deleteRecentPeers(existingPeer);
        }
    }

    @After
    public void tearDown() {
        AppDatabase reopened = AppDatabase.getInstance(context);
        reopened.myDao().deleteHour(TEST_YEAR, TEST_MONTH, TEST_DAY, TEST_HOUR);
        RecentPeerEntity testPeer = reopened.myDao().loadRecentPeer(TEST_DEVICE_ID);
        if (testPeer != null) {
            reopened.myDao().deleteRecentPeers(testPeer);
        }
        AppDatabase.resetInstanceForTesting();
    }

    @Test
    public void singleton_reusesInstanceAndPreservesBothEntityTypesAcrossReopen() {
        assertSame(database, AppDatabase.getInstance(context));

        Hour hour = new Hour();
        hour.id = TEST_HOUR;
        hour.year = TEST_YEAR;
        hour.month = TEST_MONTH;
        hour.day = TEST_DAY;
        hour.consumption = 1.5f;
        dao.insertHours(hour);

        RecentPeerEntity peer = new RecentPeerEntity();
        peer.deviceId = TEST_DEVICE_ID;
        peer.uniqueName = "Room test peer";
        dao.insertRecentPeers(peer);

        AppDatabase.resetInstanceForTesting();
        AppDatabase reopened = AppDatabase.getInstance(context);

        Hour reopenedHour = reopened.myDao().loadDayHour(TEST_YEAR, TEST_MONTH, TEST_DAY,
                TEST_HOUR);
        RecentPeerEntity reopenedPeer = reopened.myDao().loadRecentPeer(TEST_DEVICE_ID);
        assertNotNull(reopenedHour);
        assertEquals(1.5f, reopenedHour.consumption, 0.0f);
        assertNotNull(reopenedPeer);
        assertEquals("Room test peer", reopenedPeer.uniqueName);
    }
}
