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

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RecordingSessionControllerTest {
    @Test
    public void stopAndJoin_repeatedBlockedReads_stopBeforeReaderOwnedRelease() throws Exception {
        for (int iteration = 0; iteration < 100; iteration++) {
            final CountDownLatch readStarted = new CountDownLatch(1);
            final CountDownLatch stopRequested = new CountDownLatch(1);
            final AtomicInteger order = new AtomicInteger();
            final AtomicInteger stopOrder = new AtomicInteger();
            final AtomicInteger releaseOrder = new AtomicInteger();

            Thread reader = new Thread(new Runnable() {
                @Override
                public void run() {
                    readStarted.countDown();
                    try {
                        while (!stopRequested.await(1, TimeUnit.SECONDS)) {
                            // Simulate a device read which returns only after capture is stopped.
                        }
                    } catch (InterruptedException ignored) {
                        // stopCapture runs before interrupt, so the stop latch is already open.
                    } finally {
                        releaseOrder.set(order.incrementAndGet());
                    }
                }
            }, "fake-audio-reader-" + iteration);
            reader.start();
            assertTrue(readStarted.await(1, TimeUnit.SECONDS));

            boolean stopped = RecordingSessionController.stopAndJoin(reader, new Runnable() {
                @Override
                public void run() {
                    stopOrder.set(order.incrementAndGet());
                    stopRequested.countDown();
                }
            }, 1000);

            assertTrue(stopped);
            assertFalse(reader.isAlive());
            assertTrue(stopOrder.get() > 0);
            assertTrue(releaseOrder.get() > stopOrder.get());
        }
    }
}
