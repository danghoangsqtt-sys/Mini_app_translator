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

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class BindingAttemptTrackerTest {
    @Test
    public void destroyBeforeAnyBindDoesNotRequestAnUnbind() {
        BindingAttemptTracker tracker = new BindingAttemptTracker();
        RecordingUnbinder unbinder = new RecordingUnbinder();

        tracker.releaseAll(unbinder, new RecordingFailureListener());

        assertEquals(0, unbinder.calls);
    }

    @Test
    public void oneConnectionCanBindWhileTheOtherNeverDoes() {
        BindingAttemptTracker first = new BindingAttemptTracker();
        BindingAttemptTracker second = new BindingAttemptTracker();
        RecordingUnbinder firstUnbinder = new RecordingUnbinder();
        RecordingUnbinder secondUnbinder = new RecordingUnbinder();
        first.recordBindAttempt();

        first.releaseAll(firstUnbinder, new RecordingFailureListener());
        second.releaseAll(secondUnbinder, new RecordingFailureListener());

        assertEquals(1, firstUnbinder.calls);
        assertEquals(0, secondUnbinder.calls);
    }

    @Test
    public void falseBindResultStillHasOneMatchingUnbind() {
        BindingAttemptTracker tracker = new BindingAttemptTracker();
        RecordingUnbinder unbinder = new RecordingUnbinder();
        boolean bindServiceReturned = false;

        // The return value is intentionally irrelevant to Android's unbind contract.
        tracker.recordBindAttempt();
        tracker.releaseAll(unbinder, new RecordingFailureListener());

        assertEquals(false, bindServiceReturned);
        assertEquals(1, unbinder.calls);
    }

    @Test
    public void bothConnectionsReleaseTheirOwnRegistration() {
        BindingAttemptTracker first = new BindingAttemptTracker();
        BindingAttemptTracker second = new BindingAttemptTracker();
        RecordingUnbinder firstUnbinder = new RecordingUnbinder();
        RecordingUnbinder secondUnbinder = new RecordingUnbinder();
        first.recordBindAttempt();
        second.recordBindAttempt();

        first.releaseAll(firstUnbinder, new RecordingFailureListener());
        second.releaseAll(secondUnbinder, new RecordingFailureListener());

        assertEquals(1, firstUnbinder.calls);
        assertEquals(1, secondUnbinder.calls);
    }

    @Test
    public void repeatedStartsReleaseOnceForEachBindAttempt() {
        BindingAttemptTracker tracker = new BindingAttemptTracker();
        RecordingUnbinder unbinder = new RecordingUnbinder();
        tracker.recordBindAttempt();
        tracker.recordBindAttempt();

        tracker.releaseAll(unbinder, new RecordingFailureListener());

        assertEquals(2, unbinder.calls);
        assertEquals(0, tracker.getPendingUnbindCount());
    }

    @Test
    public void cleanupFailureDoesNotPreventLaterAttemptFromBeingReleased() {
        BindingAttemptTracker tracker = new BindingAttemptTracker();
        FailingFirstUnbinder unbinder = new FailingFirstUnbinder();
        RecordingFailureListener failures = new RecordingFailureListener();
        tracker.recordBindAttempt();
        tracker.recordBindAttempt();

        tracker.releaseAll(unbinder, failures);

        assertEquals(2, unbinder.calls);
        assertEquals(1, failures.failures.size());
        assertEquals(0, tracker.getPendingUnbindCount());
    }

    private static class RecordingUnbinder implements BindingAttemptTracker.Unbinder {
        int calls;

        @Override
        public void unbind() {
            calls++;
        }
    }

    private static final class FailingFirstUnbinder extends RecordingUnbinder {
        @Override
        public void unbind() {
            super.unbind();
            if (calls == 1) {
                throw new RuntimeException("first cleanup failed");
            }
        }
    }

    private static final class RecordingFailureListener
            implements BindingAttemptTracker.FailureListener {
        final List<RuntimeException> failures = new ArrayList<>();

        @Override
        public void onFailure(RuntimeException error) {
            failures.add(error);
        }
    }
}
