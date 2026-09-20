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

import io.grpc.Status;
import nie.translator.rtranslatordevedition.tools.ErrorCodes;

/** Gates terminal stream events and translates gRPC failures into existing app reasons. */
final class RecognizerStreamErrorHandler {
    interface Listener {
        void onStreamError(long streamId, int reason);
    }

    private final Listener listener;
    private boolean streamActive;
    private long activeStreamId;

    RecognizerStreamErrorHandler(Listener listener) {
        this.listener = listener;
    }

    synchronized long onStreamStarted() {
        activeStreamId++;
        streamActive = true;
        return activeStreamId;
    }

    synchronized boolean onStreamFinished(long streamId) {
        if (!streamActive || streamId != activeStreamId) {
            return false;
        }
        streamActive = false;
        return true;
    }

    boolean onError(long streamId, Throwable error) {
        final int reason;
        synchronized (this) {
            if (!streamActive || streamId != activeStreamId) {
                return false;
            }
            streamActive = false;
            reason = mapReason(error);
        }
        listener.onStreamError(streamId, reason);
        return true;
    }

    private static int mapReason(Throwable error) {
        Status.Code code = error == null
                ? Status.Code.UNKNOWN
                : Status.fromThrowable(error).getCode();
        switch (code) {
            case UNAVAILABLE:
            case DEADLINE_EXCEEDED:
                return ErrorCodes.MISSED_CONNECTION;
            case UNAUTHENTICATED:
            case PERMISSION_DENIED:
                return ErrorCodes.WRONG_API_KEY;
            default:
                return ErrorCodes.ERROR;
        }
    }
}
