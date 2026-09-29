package nie.translator.rtranslatordevedition.diagnostics;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.os.Build;
import androidx.annotation.RequiresApi;
import java.util.List;

/** Keeps API 30 process-exit calls out of the code path loaded on older Android versions. */
@RequiresApi(api = Build.VERSION_CODES.R)
final class Api30ProcessExitReader {
    private static final int MAX_HISTORY_RECORDS = 8;

    private Api30ProcessExitReader() {
    }

    static ProcessExitRecord readLatest(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) {
            return ProcessExitRecord.noHistory();
        }
        List<ApplicationExitInfo> history = manager.getHistoricalProcessExitReasons(
                context.getPackageName(), 0, MAX_HISTORY_RECORDS);
        ApplicationExitInfo latest = null;
        if (history != null) {
            for (ApplicationExitInfo candidate : history) {
                if (candidate != null && (latest == null
                        || candidate.getTimestamp() > latest.getTimestamp())) {
                    latest = candidate;
                }
            }
        }
        if (latest == null) {
            return ProcessExitRecord.noHistory();
        }
        return ProcessExitRecord.fromPlatform(latest.getReason(), latest.getStatus(),
                latest.getImportance(), latest.getTimestamp(),
                latest.getProcessStateSummary());
    }

    static void writeProcessSummary(Context context, byte[] summary) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            manager.setProcessStateSummary(summary);
        }
    }
}
