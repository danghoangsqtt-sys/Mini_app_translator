package nie.translator.rtranslatordevedition.diagnostics;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.preference.PreferenceManager;
import androidx.core.content.ContextCompat;
import java.io.IOException;
import java.io.OutputStream;

/** Process-wide, local-only diagnostics entry point. */
public final class AppDiagnostics {
    public static final String EXPORT_FILE_NAME = "mini-conversation-diagnostics.txt";
    private static final String PREF_SELECTED_MODE = "fragment";
    private static final String BLUETOOTH_SCAN_PERMISSION =
            "android.permission.BLUETOOTH_SCAN";
    private static final String BLUETOOTH_CONNECT_PERMISSION =
            "android.permission.BLUETOOTH_CONNECT";
    private static final String BLUETOOTH_ADVERTISE_PERMISSION =
            "android.permission.BLUETOOTH_ADVERTISE";
    private static volatile AppDiagnostics instance;

    private final Context applicationContext;
    private final DiagnosticState state;
    private final ProcessExitRecord processExit;

    private AppDiagnostics(Context context, DiagnosticState state,
                           ProcessExitRecord processExit) {
        this.applicationContext = context.getApplicationContext();
        this.state = state;
        this.processExit = processExit;
    }

    public static synchronized AppDiagnostics initialize(Context context) {
        if (instance != null) {
            return instance;
        }
        Context applicationContext = context.getApplicationContext();
        DiagnosticState state = new DiagnosticState();
        ProcessExitRecord processExit = ProcessExitRecord.unavailable();
        DiagnosticEvent.ErrorCategory historyError = DiagnosticEvent.ErrorCategory.NONE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                processExit = Api30ProcessExitReader.readLatest(applicationContext);
            } catch (RuntimeException error) {
                historyError = DiagnosticEvent.ErrorCategory.fromThrowable(error);
            }
        }
        AppDiagnostics diagnostics = new AppDiagnostics(applicationContext, state, processExit);
        instance = diagnostics;
        diagnostics.recordInternal(diagnostics.readSelectedMode(),
                DiagnosticEvent.Stage.PROCESS_CREATED,
                DiagnosticEvent.Operation.APP_START,
                DiagnosticEvent.ErrorCategory.NONE, true);
        if (historyError != DiagnosticEvent.ErrorCategory.NONE) {
            diagnostics.recordInternal(DiagnosticEvent.Mode.UNKNOWN,
                    DiagnosticEvent.Stage.PROCESS_EXIT_READ,
                    DiagnosticEvent.Operation.READ_EXIT_HISTORY, historyError, true);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            diagnostics.recordInternal(DiagnosticEvent.Mode.UNKNOWN,
                    DiagnosticEvent.Stage.PROCESS_EXIT_READ,
                    DiagnosticEvent.Operation.READ_EXIT_HISTORY,
                    DiagnosticEvent.ErrorCategory.NONE, true);
        }
        return diagnostics;
    }

    public static AppDiagnostics getOrInitialize(Context context) {
        AppDiagnostics current = instance;
        return current != null ? current : initialize(context);
    }

    public static void recordEvent(DiagnosticEvent.Mode mode, DiagnosticEvent.Stage stage,
                                   DiagnosticEvent.Operation operation,
                                   DiagnosticEvent.ErrorCategory errorCategory) {
        AppDiagnostics current = instance;
        if (current != null) {
            current.recordInternal(mode, stage, operation, errorCategory, true);
        }
    }

    public static void recordFailure(DiagnosticEvent.Mode mode, DiagnosticEvent.Stage stage,
                                     DiagnosticEvent.Operation operation, Throwable error) {
        recordEvent(mode, stage, operation,
                DiagnosticEvent.ErrorCategory.fromThrowable(error));
    }

    public static Intent newExportIntent() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, EXPORT_FILE_NAME);
        return intent;
    }

    public byte[] buildReportBytes() {
        return DiagnosticReport.toBytes(buildReportData());
    }

    public void writeReport(OutputStream output) throws IOException {
        DiagnosticReport.write(buildReportData(), output);
    }

    private void recordInternal(DiagnosticEvent.Mode mode, DiagnosticEvent.Stage stage,
                                DiagnosticEvent.Operation operation,
                                DiagnosticEvent.ErrorCategory errorCategory,
                                boolean updateProcessSummary) {
        state.record(mode, stage, operation, errorCategory);
        if (updateProcessSummary && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Api30ProcessExitReader.writeProcessSummary(
                        applicationContext, state.encodeProcessSummary());
            } catch (RuntimeException error) {
                state.record(DiagnosticEvent.Mode.UNKNOWN, DiagnosticEvent.Stage.PROCESS_EXIT_READ,
                        DiagnosticEvent.Operation.WRITE_PROCESS_SUMMARY,
                        DiagnosticEvent.ErrorCategory.fromThrowable(error));
            }
        }
    }

    private DiagnosticReport.Data buildReportData() {
        PackageVersion version = readPackageVersion();
        return new DiagnosticReport.Data(version.name, version.code, Build.VERSION.SDK_INT,
                readDeviceClass(), permissionState(Manifest.permission.RECORD_AUDIO, false),
                permissionState(BLUETOOTH_SCAN_PERMISSION, true),
                permissionState(BLUETOOTH_CONNECT_PERMISSION, true),
                permissionState(BLUETOOTH_ADVERTISE_PERMISSION, true),
                readSelectedMode(), state.snapshot(), processExit);
    }

    private PackageVersion readPackageVersion() {
        try {
            PackageInfo packageInfo = applicationContext.getPackageManager()
                    .getPackageInfo(applicationContext.getPackageName(), 0);
            long versionCode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                    ? packageInfo.getLongVersionCode() : packageInfo.versionCode;
            return new PackageVersion(packageInfo.versionName, versionCode);
        } catch (PackageManager.NameNotFoundException error) {
            recordInternal(DiagnosticEvent.Mode.UNKNOWN, DiagnosticEvent.Stage.EXPORT,
                    DiagnosticEvent.Operation.EXPORT_DIAGNOSTICS,
                    DiagnosticEvent.ErrorCategory.INVALID_STATE, false);
            return new PackageVersion("unknown", 0L);
        }
    }

    private DiagnosticReport.DeviceClass readDeviceClass() {
        ActivityManager manager = (ActivityManager) applicationContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        boolean lowRam = manager != null && manager.isLowRamDevice();
        Configuration configuration = applicationContext.getResources().getConfiguration();
        int smallestWidth = configuration.smallestScreenWidthDp;
        if (smallestWidth >= 720) {
            return lowRam ? DiagnosticReport.DeviceClass.LOW_RAM_TABLET
                    : DiagnosticReport.DeviceClass.LARGE_TABLET;
        }
        if (smallestWidth >= 600) {
            return lowRam ? DiagnosticReport.DeviceClass.LOW_RAM_TABLET
                    : DiagnosticReport.DeviceClass.TABLET;
        }
        return lowRam ? DiagnosticReport.DeviceClass.LOW_RAM_PHONE
                : DiagnosticReport.DeviceClass.PHONE;
    }

    private DiagnosticReport.PermissionState permissionState(String permission,
                                                              boolean nearbyPermission) {
        if (nearbyPermission && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return DiagnosticReport.PermissionState.NOT_REQUIRED;
        }
        return ContextCompat.checkSelfPermission(applicationContext, permission)
                == PackageManager.PERMISSION_GRANTED
                ? DiagnosticReport.PermissionState.GRANTED
                : DiagnosticReport.PermissionState.DENIED;
    }

    private DiagnosticEvent.Mode readSelectedMode() {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(applicationContext);
        return DiagnosticEvent.Mode.fromPersistedFragment(
                preferences.getInt(PREF_SELECTED_MODE, 0));
    }

    private static final class PackageVersion {
        private final String name;
        private final long code;

        private PackageVersion(String name, long code) {
            this.name = name;
            this.code = code;
        }
    }
}
