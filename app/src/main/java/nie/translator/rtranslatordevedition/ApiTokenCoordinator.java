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

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Serializes access-token publication and its one owned delayed refresh operation.
 */
final class ApiTokenCoordinator {
    interface TokenFetcher {
        AccessToken fetch() throws IOException;
    }

    interface Scheduler {
        void schedule(Runnable runnable, long delayMillis);

        void cancel(Runnable runnable);
    }

    interface Listener {
        void onSuccess(AccessToken token);

        void onFailure(IOException exception);
    }

    private final Object lock = new Object();
    private final Executor executor;
    private final TokenFetcher tokenFetcher;
    private final Scheduler scheduler;
    private final long refreshMarginMillis;
    private final ArrayDeque<Listener> waitingListeners = new ArrayDeque<Listener>();

    private AccessToken token;
    private boolean fetchInFlight;
    private long generation;
    private Runnable scheduledRefresh;

    ApiTokenCoordinator(Executor executor, TokenFetcher tokenFetcher, Scheduler scheduler,
                        long refreshMarginMillis) {
        this.executor = executor;
        this.tokenFetcher = tokenFetcher;
        this.scheduler = scheduler;
        this.refreshMarginMillis = refreshMarginMillis;
    }

    void request(boolean recycleResult, Listener listener) {
        AccessToken cachedToken = null;
        boolean startFetch = false;
        long fetchGeneration = 0;

        synchronized (lock) {
            if (recycleResult && isUsable(token)) {
                cachedToken = token;
            } else {
                if (listener != null) {
                    waitingListeners.addLast(listener);
                }
                if (!fetchInFlight) {
                    fetchInFlight = true;
                    fetchGeneration = generation;
                    startFetch = true;
                }
            }
        }

        if (cachedToken != null) {
            notifySuccess(listener, cachedToken);
        } else if (startFetch) {
            startFetch(fetchGeneration);
        }
    }

    void reset() {
        List<Listener> listeners;
        synchronized (lock) {
            generation++;
            token = null;
            fetchInFlight = false;
            cancelScheduledRefreshLocked();
            listeners = drainWaitingListenersLocked();
        }
        notifyFailure(listeners, new IOException("Credential changed while access token refresh was pending"));
    }

    private void startFetch(final long fetchGeneration) {
        try {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        AccessToken fetchedToken = tokenFetcher.fetch();
                        if (fetchedToken == null) {
                            completeFetch(fetchGeneration, null,
                                    new IOException("Credential refresh returned no access token"));
                        } else {
                            completeFetch(fetchGeneration, fetchedToken, null);
                        }
                    } catch (IOException exception) {
                        completeFetch(fetchGeneration, null, exception);
                    } catch (RuntimeException exception) {
                        completeFetch(fetchGeneration, null,
                                new IOException("Credential refresh failed", exception));
                    }
                }
            });
        } catch (RuntimeException exception) {
            completeFetch(fetchGeneration, null,
                    new IOException("Unable to start credential refresh", exception));
        }
    }

    private void completeFetch(long fetchGeneration, AccessToken fetchedToken,
                               IOException exception) {
        List<Listener> listeners;
        synchronized (lock) {
            if (fetchGeneration != generation) {
                return;
            }

            fetchInFlight = false;
            listeners = drainWaitingListenersLocked();
            if (exception == null) {
                token = fetchedToken;
                scheduleRefreshLocked(fetchedToken);
            }
        }

        if (exception == null) {
            notifySuccess(listeners, fetchedToken);
        } else {
            notifyFailure(listeners, exception);
        }
    }

    private void scheduleRefreshLocked(AccessToken accessToken) {
        cancelScheduledRefreshLocked();
        if (accessToken.getExpirationTime() == null) {
            return;
        }

        long delayMillis = Math.max(accessToken.getExpirationTime().getTime()
                - System.currentTimeMillis() - refreshMarginMillis, refreshMarginMillis);
        scheduledRefresh = new Runnable() {
            @Override
            public void run() {
                synchronized (lock) {
                    if (scheduledRefresh != this) {
                        return;
                    }
                    scheduledRefresh = null;
                }
                request(false, null);
            }
        };
        scheduler.schedule(scheduledRefresh, delayMillis);
    }

    private void cancelScheduledRefreshLocked() {
        if (scheduledRefresh != null) {
            scheduler.cancel(scheduledRefresh);
            scheduledRefresh = null;
        }
    }

    private boolean isUsable(AccessToken accessToken) {
        return accessToken != null && accessToken.getExpirationTime() != null
                && accessToken.getExpirationTime().getTime() > System.currentTimeMillis();
    }

    private List<Listener> drainWaitingListenersLocked() {
        List<Listener> listeners = new ArrayList<Listener>(waitingListeners);
        waitingListeners.clear();
        return listeners;
    }

    private void notifySuccess(Listener listener, AccessToken accessToken) {
        if (listener != null) {
            listener.onSuccess(accessToken);
        }
    }

    private void notifySuccess(List<Listener> listeners, AccessToken accessToken) {
        for (Listener listener : listeners) {
            listener.onSuccess(accessToken);
        }
    }

    private void notifyFailure(List<Listener> listeners, IOException exception) {
        for (Listener listener : listeners) {
            listener.onFailure(exception);
        }
    }
}
