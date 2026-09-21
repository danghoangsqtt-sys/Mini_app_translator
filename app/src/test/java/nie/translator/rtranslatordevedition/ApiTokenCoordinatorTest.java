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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ApiTokenCoordinatorTest {
    private static final long TIMEOUT_SECONDS = 2;

    @Test
    public void simultaneousRequestsShareOneFetchAndNotifyEachListenerOnce() throws Exception {
        final ManualExecutor executor = new ManualExecutor();
        final AtomicInteger fetchCount = new AtomicInteger();
        final RecordingListener first = new RecordingListener();
        final RecordingListener second = new RecordingListener();
        final RecordingListener third = new RecordingListener();
        final ApiTokenCoordinator coordinator = newCoordinator(executor,
                new ApiTokenCoordinator.TokenFetcher() {
                    @Override
                    public AccessToken fetch() {
                        fetchCount.incrementAndGet();
                        return token("shared");
                    }
                }, new RecordingScheduler());

        final CountDownLatch ready = new CountDownLatch(3);
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch returned = new CountDownLatch(3);
        startRequester(coordinator, first, ready, start, returned);
        startRequester(coordinator, second, ready, start, returned);
        startRequester(coordinator, third, ready, start, returned);

        assertTrue(ready.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        start.countDown();
        assertTrue(returned.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        executor.runAll();

        assertEquals(1, fetchCount.get());
        assertEquals(1, first.successCount.get());
        assertEquals(1, second.successCount.get());
        assertEquals(1, third.successCount.get());
        assertEquals(0, first.failureCount.get() + second.failureCount.get() + third.failureCount.get());
    }

    @Test
    public void resetFailsPendingListenerAndLateRefreshCannotOverwriteNewToken() throws Exception {
        final BlockingFetcher fetcher = new BlockingFetcher(token("old"), token("new"));
        final RecordingListener staleListener = new RecordingListener();
        final RecordingListener freshListener = new RecordingListener();
        final RecordingListener cachedListener = new RecordingListener();
        final ApiTokenCoordinator coordinator = newCoordinator(new ThreadPerTaskExecutor(), fetcher,
                new RecordingScheduler());

        coordinator.request(false, staleListener);
        assertTrue(fetcher.firstStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        coordinator.reset();
        coordinator.request(false, freshListener);
        assertTrue(freshListener.terminal.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        fetcher.releaseFirst.countDown();
        assertTrue(fetcher.firstFinished.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        coordinator.request(true, cachedListener);

        assertEquals(0, staleListener.successCount.get());
        assertEquals(1, staleListener.failureCount.get());
        assertEquals(1, freshListener.successCount.get());
        assertEquals(0, freshListener.failureCount.get());
        assertEquals(1, cachedListener.successCount.get());
        assertSame(fetcher.newToken, cachedListener.token.get());
    }

    @Test
    public void callbackCanRequestCachedTokenWithoutDeadlock() {
        final AtomicInteger fetchCount = new AtomicInteger();
        final RecordingListener nestedListener = new RecordingListener();
        final AtomicInteger outerCallbacks = new AtomicInteger();
        final ApiTokenCoordinator coordinator = newCoordinator(new DirectExecutor(),
                new ApiTokenCoordinator.TokenFetcher() {
                    @Override
                    public AccessToken fetch() {
                        fetchCount.incrementAndGet();
                        return token("reentrant");
                    }
                }, new RecordingScheduler());

        coordinator.request(false, new ApiTokenCoordinator.Listener() {
            @Override
            public void onSuccess(AccessToken accessToken) {
                outerCallbacks.incrementAndGet();
                coordinator.request(true, nestedListener);
            }

            @Override
            public void onFailure(IOException exception) {
                throw new AssertionError(exception);
            }
        });

        assertEquals(1, fetchCount.get());
        assertEquals(1, outerCallbacks.get());
        assertEquals(1, nestedListener.successCount.get());
    }

    @Test
    public void resetCancelsOnlyTheCoordinatorRefreshRunnable() {
        final RecordingScheduler scheduler = new RecordingScheduler();
        final ApiTokenCoordinator coordinator = newCoordinator(new DirectExecutor(),
                new ApiTokenCoordinator.TokenFetcher() {
                    @Override
                    public AccessToken fetch() {
                        return token("scheduled");
                    }
                }, scheduler);

        coordinator.request(false, new RecordingListener());
        Runnable ownedRefresh = scheduler.scheduled.get(0);
        coordinator.reset();

        assertEquals(1, scheduler.cancelled.size());
        assertSame(ownedRefresh, scheduler.cancelled.get(0));
    }

    private static ApiTokenCoordinator newCoordinator(Executor executor,
                                                        ApiTokenCoordinator.TokenFetcher fetcher,
                                                        ApiTokenCoordinator.Scheduler scheduler) {
        return new ApiTokenCoordinator(executor, fetcher, scheduler, 1000);
    }

    private static AccessToken token(String value) {
        return new AccessToken(value, new Date(System.currentTimeMillis() + 60 * 60 * 1000));
    }

    private static void startRequester(final ApiTokenCoordinator coordinator,
                                       final ApiTokenCoordinator.Listener listener,
                                       final CountDownLatch ready, final CountDownLatch start,
                                       final CountDownLatch returned) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ready.countDown();
                try {
                    start.await();
                    coordinator.request(false, listener);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } finally {
                    returned.countDown();
                }
            }
        }).start();
    }

    private static final class RecordingListener implements ApiTokenCoordinator.Listener {
        private final AtomicInteger successCount = new AtomicInteger();
        private final AtomicInteger failureCount = new AtomicInteger();
        private final AtomicReference<AccessToken> token = new AtomicReference<AccessToken>();
        private final CountDownLatch terminal = new CountDownLatch(1);

        @Override
        public void onSuccess(AccessToken accessToken) {
            token.set(accessToken);
            successCount.incrementAndGet();
            terminal.countDown();
        }

        @Override
        public void onFailure(IOException exception) {
            failureCount.incrementAndGet();
            terminal.countDown();
        }
    }

    private static final class ManualExecutor implements Executor {
        private final List<Runnable> tasks = new ArrayList<Runnable>();

        @Override
        public synchronized void execute(Runnable command) {
            tasks.add(command);
        }

        private synchronized void runAll() {
            List<Runnable> pending = new ArrayList<Runnable>(tasks);
            tasks.clear();
            for (Runnable task : pending) {
                task.run();
            }
        }
    }

    private static final class DirectExecutor implements Executor {
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }

    private static final class ThreadPerTaskExecutor implements Executor {
        @Override
        public void execute(Runnable command) {
            new Thread(command).start();
        }
    }

    private static final class RecordingScheduler implements ApiTokenCoordinator.Scheduler {
        private final List<Runnable> scheduled = new ArrayList<Runnable>();
        private final List<Runnable> cancelled = new ArrayList<Runnable>();

        @Override
        public void schedule(Runnable runnable, long delayMillis) {
            scheduled.add(runnable);
        }

        @Override
        public void cancel(Runnable runnable) {
            cancelled.add(runnable);
        }
    }

    private static final class BlockingFetcher implements ApiTokenCoordinator.TokenFetcher {
        private final AccessToken oldToken;
        private final AccessToken newToken;
        private final AtomicInteger callCount = new AtomicInteger();
        private final CountDownLatch firstStarted = new CountDownLatch(1);
        private final CountDownLatch releaseFirst = new CountDownLatch(1);
        private final CountDownLatch firstFinished = new CountDownLatch(1);

        private BlockingFetcher(AccessToken oldToken, AccessToken newToken) {
            this.oldToken = oldToken;
            this.newToken = newToken;
        }

        @Override
        public AccessToken fetch() throws IOException {
            if (callCount.incrementAndGet() == 1) {
                firstStarted.countDown();
                try {
                    releaseFirst.await();
                    return oldToken;
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while simulating an old refresh", exception);
                } finally {
                    firstFinished.countDown();
                }
            }
            return newToken;
        }
    }
}
