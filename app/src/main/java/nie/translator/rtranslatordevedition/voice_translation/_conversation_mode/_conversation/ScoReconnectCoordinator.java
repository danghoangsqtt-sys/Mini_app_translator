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

/**
 * Owns the delayed SCO reconnect work for one ConversationService instance.
 */
final class ScoReconnectCoordinator {
    interface Dispatcher {
        void postDelayed(Runnable runnable, long delayMillis);

        void removeCallbacks(Runnable runnable);
    }

    interface Reconnector {
        void reconnect();
    }

    private final Dispatcher dispatcher;
    private final Reconnector reconnector;
    private Runnable pendingReconnect;
    private long generation;
    private boolean destroyed;

    ScoReconnectCoordinator(Dispatcher dispatcher, Reconnector reconnector) {
        this.dispatcher = dispatcher;
        this.reconnector = reconnector;
    }

    synchronized void scheduleReconnect(long delayMillis) {
        if (destroyed) {
            return;
        }
        cancelPendingReconnectLocked();
        final long reconnectGeneration = ++generation;
        Runnable reconnect = new Runnable() {
            @Override
            public void run() {
                deliverReconnect(this, reconnectGeneration);
            }
        };
        pendingReconnect = reconnect;
        dispatcher.postDelayed(reconnect, delayMillis);
    }

    synchronized void destroy() {
        destroyed = true;
        generation++;
        cancelPendingReconnectLocked();
    }

    private synchronized void deliverReconnect(Runnable reconnect, long reconnectGeneration) {
        if (destroyed || reconnect != pendingReconnect || reconnectGeneration != generation) {
            return;
        }
        pendingReconnect = null;
        // Keep teardown and this callback mutually exclusive: destroy() cannot finish before an
        // already-dispatched reconnect has returned, and a late runnable sees destroyed above.
        reconnector.reconnect();
    }

    private void cancelPendingReconnectLocked() {
        if (pendingReconnect != null) {
            dispatcher.removeCallbacks(pendingReconnect);
            pendingReconnect = null;
        }
    }
}
