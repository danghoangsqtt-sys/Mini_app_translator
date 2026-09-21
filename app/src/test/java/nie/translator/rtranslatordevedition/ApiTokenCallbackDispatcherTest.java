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

package nie.translator.rtranslatordevedition;

import com.google.auth.oauth2.AccessToken;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.Assert.assertEquals;

public class ApiTokenCallbackDispatcherTest {
    @Test
    public void queuedOldSuccessIsReplacedByResetFailureAndFreshSuccess() {
        ManualMainQueue mainQueue = new ManualMainQueue();
        ApiTokenCallbackDispatcher dispatcher = new ApiTokenCallbackDispatcher(mainQueue);
        RecordingCallback oldCredentialCallback = new RecordingCallback();
        RecordingCallback newCredentialCallback = new RecordingCallback();

        ApiTokenCallbackDispatcher.Registration oldRegistration =
                dispatcher.register(oldCredentialCallback);
        dispatcher.postSuccess(oldRegistration, token("old"));
        dispatcher.reset(new IOException("Credential changed"));

        ApiTokenCallbackDispatcher.Registration newRegistration =
                dispatcher.register(newCredentialCallback);
        dispatcher.postSuccess(newRegistration, token("new"));
        mainQueue.runAll();

        assertEquals(0, oldCredentialCallback.successCount);
        assertEquals(1, oldCredentialCallback.failureCount);
        assertEquals(1, newCredentialCallback.successCount);
        assertEquals(0, newCredentialCallback.failureCount);
    }

    @Test
    public void lateSuccessAfterResetDoesNotDuplicateTheResetFailure() {
        ManualMainQueue mainQueue = new ManualMainQueue();
        ApiTokenCallbackDispatcher dispatcher = new ApiTokenCallbackDispatcher(mainQueue);
        RecordingCallback callback = new RecordingCallback();
        ApiTokenCallbackDispatcher.Registration registration = dispatcher.register(callback);

        dispatcher.reset(new IOException("Credential deleted"));
        dispatcher.postSuccess(registration, token("old"));
        mainQueue.runAll();

        assertEquals(0, callback.successCount);
        assertEquals(1, callback.failureCount);
    }

    @Test
    public void coordinatorCompletionQueuedBeforeResetCannotBeatFreshCredentialCallback() {
        ManualMainQueue mainQueue = new ManualMainQueue();
        ManualExecutor worker = new ManualExecutor();
        ApiTokenCallbackDispatcher dispatcher = new ApiTokenCallbackDispatcher(mainQueue);
        ApiTokenCoordinator coordinator = new ApiTokenCoordinator(worker,
                new ApiTokenCoordinator.TokenFetcher() {
                    private int callCount;

                    @Override
                    public AccessToken fetch() {
                        callCount++;
                        return token(callCount == 1 ? "old" : "new");
                    }
                }, new NoOpScheduler(), 1000);
        RecordingCallback oldCredentialCallback = new RecordingCallback();
        RecordingCallback newCredentialCallback = new RecordingCallback();

        ApiTokenCallbackDispatcher.Registration oldRegistration =
                dispatcher.register(oldCredentialCallback);
        coordinator.request(false, forward(dispatcher, oldRegistration));
        worker.runNext();

        dispatcher.reset(new IOException("Credential changed"));
        coordinator.reset();

        ApiTokenCallbackDispatcher.Registration newRegistration =
                dispatcher.register(newCredentialCallback);
        coordinator.request(false, forward(dispatcher, newRegistration));
        worker.runNext();
        mainQueue.runAll();

        assertEquals(0, oldCredentialCallback.successCount);
        assertEquals(1, oldCredentialCallback.failureCount);
        assertEquals(1, newCredentialCallback.successCount);
        assertEquals(0, newCredentialCallback.failureCount);
    }

    private static ApiTokenCoordinator.Listener forward(
            final ApiTokenCallbackDispatcher dispatcher,
            final ApiTokenCallbackDispatcher.Registration registration) {
        return new ApiTokenCoordinator.Listener() {
            @Override
            public void onSuccess(AccessToken accessToken) {
                dispatcher.postSuccess(registration, accessToken);
            }

            @Override
            public void onFailure(IOException exception) {
                dispatcher.postFailure(registration, exception);
            }
        };
    }

    private static AccessToken token(String value) {
        return new AccessToken(value, new Date(System.currentTimeMillis() + 60000));
    }

    private static final class ManualMainQueue
            implements ApiTokenCallbackDispatcher.MainThreadExecutor {
        private final List<Runnable> runnables = new ArrayList<Runnable>();

        @Override
        public void execute(Runnable runnable) {
            runnables.add(runnable);
        }

        private void runAll() {
            List<Runnable> pending = new ArrayList<Runnable>(runnables);
            runnables.clear();
            for (Runnable runnable : pending) {
                runnable.run();
            }
        }
    }

    private static final class RecordingCallback implements ApiTokenCallbackDispatcher.Callback {
        private int successCount;
        private int failureCount;

        @Override
        public void onSuccess(AccessToken accessToken) {
            successCount++;
        }

        @Override
        public void onFailure(IOException exception) {
            failureCount++;
        }
    }

    private static final class ManualExecutor implements Executor {
        private final List<Runnable> runnables = new ArrayList<Runnable>();

        @Override
        public void execute(Runnable runnable) {
            runnables.add(runnable);
        }

        private void runNext() {
            Runnable runnable = runnables.remove(0);
            runnable.run();
        }
    }

    private static final class NoOpScheduler implements ApiTokenCoordinator.Scheduler {
        @Override
        public void schedule(Runnable runnable, long delayMillis) {
        }

        @Override
        public void cancel(Runnable runnable) {
        }
    }
}
