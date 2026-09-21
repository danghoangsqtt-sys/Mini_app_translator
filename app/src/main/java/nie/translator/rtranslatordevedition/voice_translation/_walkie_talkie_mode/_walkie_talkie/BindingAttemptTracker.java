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
 * Counts bindService registrations for one ServiceConnection. Android requires one unbind for
 * every bindService call, including calls that return false, so this deliberately does not track
 * the return value or onServiceConnected.
 */
final class BindingAttemptTracker {
    interface Unbinder {
        void unbind();
    }

    interface FailureListener {
        void onFailure(RuntimeException error);
    }

    private int pendingUnbinds;

    synchronized void recordBindAttempt() {
        pendingUnbinds++;
    }

    synchronized int getPendingUnbindCount() {
        return pendingUnbinds;
    }

    void releaseAll(Unbinder unbinder, FailureListener failureListener) {
        while (consumeOneAttempt()) {
            try {
                unbinder.unbind();
            } catch (RuntimeException error) {
                failureListener.onFailure(error);
            }
        }
    }

    private synchronized boolean consumeOneAttempt() {
        if (pendingUnbinds == 0) {
            return false;
        }
        pendingUnbinds--;
        return true;
    }
}
