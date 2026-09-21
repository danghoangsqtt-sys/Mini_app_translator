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
import java.util.ArrayList;
import java.util.List;

/**
 * Dispatches token callbacks onto the application's main queue.
 */
final class ApiTokenCallbackDispatcher {
    interface MainThreadExecutor {
        void execute(Runnable runnable);
    }

    interface Callback {
        void onSuccess(AccessToken accessToken);

        void onFailure(IOException exception);
    }

    static final class Registration {
        private final Callback callback;
        private final long generation;
        private int state = PENDING;

        private Registration(Callback callback, long generation) {
            this.callback = callback;
            this.generation = generation;
        }
    }

    private static final int PENDING = 0;
    private static final int QUEUED = 1;
    private static final int TERMINAL = 2;

    private final Object lock = new Object();
    private final MainThreadExecutor mainThreadExecutor;
    private final List<Registration> registrations = new ArrayList<Registration>();
    private long generation;

    ApiTokenCallbackDispatcher(MainThreadExecutor mainThreadExecutor) {
        this.mainThreadExecutor = mainThreadExecutor;
    }

    Registration register(Callback callback) {
        synchronized (lock) {
            Registration registration = new Registration(callback, generation);
            registrations.add(registration);
            return registration;
        }
    }

    void postSuccess(final Registration registration, final AccessToken accessToken) {
        queue(registration, new Delivery() {
            @Override
            public void deliver(Callback callback) {
                callback.onSuccess(accessToken);
            }
        });
    }

    void postFailure(final Registration registration, final IOException exception) {
        queue(registration, new Delivery() {
            @Override
            public void deliver(Callback callback) {
                callback.onFailure(exception);
            }
        });
    }

    void reset(final IOException exception) {
        List<Registration> invalidated = new ArrayList<Registration>();
        synchronized (lock) {
            generation++;
            for (Registration registration : registrations) {
                if (registration.state != TERMINAL) {
                    registration.state = TERMINAL;
                    invalidated.add(registration);
                }
            }
            registrations.removeAll(invalidated);
        }
        for (final Registration registration : invalidated) {
            mainThreadExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    registration.callback.onFailure(exception);
                }
            });
        }
    }

    private void queue(final Registration registration, final Delivery delivery) {
        synchronized (lock) {
            if (registration.state != PENDING || registration.generation != generation) {
                return;
            }
            registration.state = QUEUED;
        }
        mainThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Callback callback = null;
                synchronized (lock) {
                    if (registration.state == QUEUED && registration.generation == generation) {
                        registration.state = TERMINAL;
                        registrations.remove(registration);
                        callback = registration.callback;
                    }
                }
                if (callback != null) {
                    delivery.deliver(callback);
                }
            }
        });
    }

    private interface Delivery {
        void deliver(Callback callback);
    }
}
