package com.bh.android.TargetHomeLauncher;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.util.Log;
import android.widget.Toast;

public class BHAppUtility {

    private final static boolean DBG = FeatureList.DBG;
    private final static String LOG_TAG = "BH_BHAppUtility";

    public BHAppUtility() {
        if (DBG) Log.v(LOG_TAG, "BHAppUtility: Constructor !");
    }

    public static void startActivitySafely(Context context, Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(LOG_TAG, "Does not have the permission to launch " + intent +
                    ".Make sure to create a MAIN intent-filter for the corresponding activity" +
                    "or use the exported attribute for this activity");
        }
    }

    public static String getSoftwareVersion(Context context) {
        String appVersion = "1.00";
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
            Log.v(LOG_TAG, packageInfo.versionName);
            appVersion = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(LOG_TAG, "Package name not found", e);
        }
        ;

        return appVersion;
    }
    // BH_Lin@20100617	-------------------------------------------------------<	
}