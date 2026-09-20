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

package nie.translator.rtranslatordevedition.voice_translation.cloud_apis.voice;

import java.util.concurrent.TimeUnit;

/** Coordinates a blocking capture loop without owning or releasing its device. */
final class RecordingSessionController {
    private RecordingSessionController() {
    }

    static boolean stopAndJoin(Thread worker, Runnable stopCapture, long timeoutMillis) {
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("timeoutMillis must be positive");
        }

        if (stopCapture != null) {
            stopCapture.run();
        }
        if (worker == null) {
            return true;
        }
        if (worker == Thread.currentThread()) {
            return false;
        }

        worker.interrupt();
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        try {
            while (worker.isAlive()) {
                long remainingNanos = deadline - System.nanoTime();
                if (remainingNanos <= 0) {
                    break;
                }
                long remainingMillis = Math.max(1,
                        TimeUnit.NANOSECONDS.toMillis(remainingNanos));
                worker.join(remainingMillis);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        return !worker.isAlive();
    }
}
