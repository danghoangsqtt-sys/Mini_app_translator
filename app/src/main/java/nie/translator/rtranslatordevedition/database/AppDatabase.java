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

package nie.translator.rtranslatordevedition.database;

import android.content.Context;
import androidx.annotation.VisibleForTesting;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import nie.translator.rtranslatordevedition.database.dao.MyDao;
import nie.translator.rtranslatordevedition.database.entities.Hour;
import nie.translator.rtranslatordevedition.database.entities.RecentPeerEntity;


@androidx.room.Database(version = 1, entities = {Hour.class, RecentPeerEntity.class})
abstract public class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "consumption_credit_dp";
    private static volatile AppDatabase instance;

    abstract public MyDao myDao();

    /**
     * All consumers of the usage/recent-peer database must share this instance. Building from
     * the application context prevents an Activity from being retained beyond its lifecycle.
     */
    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, DATABASE_NAME).build();
                }
            }
        }
        return instance;
    }

    @VisibleForTesting
    static void resetInstanceForTesting() {
        synchronized (AppDatabase.class) {
            if (instance != null) {
                instance.close();
                instance = null;
            }
        }
    }
}
