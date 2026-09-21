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

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ScoReconnectCoordinatorTest {
    @Test
    public void pendingReconnectCannotRunAfterDestroy() {
        FakeDispatcher dispatcher = new FakeDispatcher();
        RecordingReconnector reconnector = new RecordingReconnector();
        ScoReconnectCoordinator coordinator = new ScoReconnectCoordinator(dispatcher, reconnector);

        coordinator.scheduleReconnect(1000);
        Runnable pending = dispatcher.lastPosted();
        coordinator.destroy();
        pending.run();

        assertTrue(dispatcher.wasRemoved(pending));
        assertEquals(0, reconnector.calls);
    }

    @Test
    public void dispatchedReconnectThatArrivesLateCannotRunAfterDestroy() {
        FakeDispatcher dispatcher = new FakeDispatcher();
        RecordingReconnector reconnector = new RecordingReconnector();
        ScoReconnectCoordinator coordinator = new ScoReconnectCoordinator(dispatcher, reconnector);

        coordinator.scheduleReconnect(1000);
        Runnable dispatchedButNotRun = dispatcher.dispatchNextWithoutRunning();
        coordinator.destroy();
        dispatchedButNotRun.run();

        assertEquals(0, reconnector.calls);
    }

    @Test
    public void oldInstanceReconnectCannotAffectANewLiveInstance() {
        FakeDispatcher oldDispatcher = new FakeDispatcher();
        RecordingReconnector oldReconnector = new RecordingReconnector();
        ScoReconnectCoordinator oldCoordinator = new ScoReconnectCoordinator(oldDispatcher, oldReconnector);
        FakeDispatcher newDispatcher = new FakeDispatcher();
        RecordingReconnector newReconnector = new RecordingReconnector();
        ScoReconnectCoordinator newCoordinator = new ScoReconnectCoordinator(newDispatcher, newReconnector);

        oldCoordinator.scheduleReconnect(1000);
        Runnable oldRunnable = oldDispatcher.lastPosted();
        newCoordinator.scheduleReconnect(1000);
        Runnable newRunnable = newDispatcher.lastPosted();
        oldCoordinator.destroy();

        oldRunnable.run();
        newRunnable.run();

        assertEquals(0, oldReconnector.calls);
        assertEquals(1, newReconnector.calls);
    }

    @Test
    public void repeatedReconnectRequestsCancelOnlyTheirOwnPriorRunnable() {
        FakeDispatcher dispatcher = new FakeDispatcher();
        RecordingReconnector reconnector = new RecordingReconnector();
        ScoReconnectCoordinator coordinator = new ScoReconnectCoordinator(dispatcher, reconnector);

        coordinator.scheduleReconnect(1000);
        Runnable first = dispatcher.lastPosted();
        coordinator.scheduleReconnect(1000);
        Runnable second = dispatcher.lastPosted();
        first.run();
        second.run();
        coordinator.destroy();
        coordinator.scheduleReconnect(1000);

        assertTrue(dispatcher.wasRemoved(first));
        assertEquals(1, reconnector.calls);
        assertEquals(1, dispatcher.posted.size());
    }

    @Test
    public void liveServiceReconnectsOnce() {
        FakeDispatcher dispatcher = new FakeDispatcher();
        RecordingReconnector reconnector = new RecordingReconnector();
        ScoReconnectCoordinator coordinator = new ScoReconnectCoordinator(dispatcher, reconnector);

        coordinator.scheduleReconnect(1000);
        dispatcher.lastPosted().run();

        assertEquals(1, reconnector.calls);
        assertFalse(dispatcher.wasRemoved(dispatcher.lastPosted()));
    }

    private static final class FakeDispatcher implements ScoReconnectCoordinator.Dispatcher {
        private final List<Runnable> posted = new ArrayList<Runnable>();
        private final List<Runnable> removed = new ArrayList<Runnable>();

        @Override
        public void postDelayed(Runnable runnable, long delayMillis) {
            posted.add(runnable);
        }

        @Override
        public void removeCallbacks(Runnable runnable) {
            removed.add(runnable);
            posted.remove(runnable);
        }

        Runnable lastPosted() {
            return posted.get(posted.size() - 1);
        }

        Runnable dispatchNextWithoutRunning() {
            return posted.remove(0);
        }

        boolean wasRemoved(Runnable runnable) {
            return removed.contains(runnable);
        }
    }

    private static final class RecordingReconnector implements ScoReconnectCoordinator.Reconnector {
        private int calls;

        @Override
        public void reconnect() {
            calls++;
        }
    }
}
