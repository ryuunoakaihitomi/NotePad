package github.ryuunoakaihitomi.notepad;

import android.app.Application;
import android.content.Context;
import android.os.Process;
import android.os.StrictMode;
import android.os.SystemClock;
import android.util.Log;
import android.util.LogPrinter;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.Arrays;

import github.ryuunoakaihitomi.notepad.ui.EditorActivity;
import github.ryuunoakaihitomi.notepad.ui.MyActivityLifecycleCallbacks;
import github.ryuunoakaihitomi.notepad.util.AndroidCompat;
import github.ryuunoakaihitomi.notepad.util.AppUtils;
import github.ryuunoakaihitomi.notepad.util.FileUtils;
import github.ryuunoakaihitomi.notepad.util.Global;
import github.ryuunoakaihitomi.notepad.util.OsUtils;
import github.ryuunoakaihitomi.notepad.util.UiUtils;
import github.ryuunoakaihitomi.notepad.util.hack.HookUtils;

public class MyApplication extends Application implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "MyApplication";

    private static final int CRASH_FREEZE_DELAY = 1000;

    @Override
    protected void attachBaseContext(Context base) {
        // version log printer
        new LogPrinter(Log.INFO, BuildConfig.APPLICATION_ID)
                .println("Notepad version = " + Arrays.asList(BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME, BuildConfig.BUILD_TYPE));

        HookUtils.removeReflectRestriction(base);
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Thread.setDefaultUncaughtExceptionHandler(this);

        if (!AndroidCompat.isMiui()) UiUtils.defineSystemToast();

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyFlashScreen()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }

        registerActivityLifecycleCallbacks(new MyActivityLifecycleCallbacks());
    }

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        long now = System.currentTimeMillis();
        String buildInfo = OsUtils.getJsonBuildInfo();
        String crashInfo = OsUtils.getJsonCrashReportInfo(BuildConfig.APPLICATION_ID,
                AppUtils.getCurrentProcessName(this),
                now,
                AppUtils.isSystemApp(this),
                getPackageManager().getInstallerPackageName(BuildConfig.APPLICATION_ID),
                e);
        String allInfo = t + System.lineSeparator() +
                crashInfo + System.lineSeparator() +
                buildInfo;

        Log.e(TAG, "uncaughtException: " + System.lineSeparator() + allInfo, e);

        if (BuildConfig.DEBUG) {
            EditorActivity.actionStart(this,
                    EditorActivity.ActionType.CREATE,
                    0,
                    new String[]{t.toString(), allInfo});
            Log.i(TAG, "uncaughtException: show on editor");
        } else {
            /* 由于Log类代码在发布时已经被proguard删除掉（可选），所以需要用另外的方法打印日志 */
            LogPrinter crashPrinter = new LogPrinter(Log.ERROR, TAG);
            crashPrinter.println(Arrays.asList(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE) + " crashed" + System.lineSeparator() +
                    "crash info:" + allInfo + System.lineSeparator() +
                    "raw stacktrace:" + Log.getStackTraceString(e));
        }

        File crashLogDir = getExternalFilesDir(Global.LOG_DIR_NAME);
        if (crashLogDir != null)
            FileUtils.writeTextFile(crashLogDir.getAbsolutePath() + File.separator + "crash_" + now + ".log", allInfo);

        SystemClock.sleep(CRASH_FREEZE_DELAY);
        Process.killProcess(Process.myPid());
        System.exit(-1);
    }
}
