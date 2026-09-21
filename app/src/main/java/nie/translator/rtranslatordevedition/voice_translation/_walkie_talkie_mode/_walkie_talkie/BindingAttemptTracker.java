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

/**
 * Tracks one active bindService registration for one ServiceConnection. A false bind result still
 * requires unbind, but an active connection cannot be registered twice because the framework
 * reuses its dispatcher and rejects a second teardown unbind.
 */
final class BindingAttemptTracker {
    interface Unbinder {
        void unbind();
    }

    interface FailureListener {
        void onFailure(RuntimeException error);
    }

    private boolean active;

    synchronized boolean recordBindAttempt() {
        if (active) return false;
        active = true;
        return true;
    }

    synchronized int getPendingUnbindCount() {
        return active ? 1 : 0;
    }

    void releaseAll(Unbinder unbinder, FailureListener failureListener) {
        if (consumeActiveAttempt()) {
            try {
                unbinder.unbind();
            } catch (RuntimeException error) {
                failureListener.onFailure(error);
            }
        }
    }

    private synchronized boolean consumeActiveAttempt() {
        if (!active) {
            return false;
        }
        active = false;
        return true;
    }
}
