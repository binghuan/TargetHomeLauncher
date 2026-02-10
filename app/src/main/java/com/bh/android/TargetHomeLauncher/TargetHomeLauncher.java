package com.bh.android.TargetHomeLauncher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

public class TargetHomeLauncher extends Activity {

    private final boolean DBG = FeatureList.DBG;
    private final String LOG_TAG = "BH_TargetHomeLauncher";

    @SuppressWarnings("unused")
    private Context mContext = null;

    private void setContext(Context context) {
        mContext = context;
    }

    private void launchTargetHomeLauncher() {
        if (DBG) Log.v(LOG_TAG, "+ launchTargetHomeLauncher");

        String pkgName = TargetHomeConfigure.getDefaultHomeAppPkgName(this);
        String pkgClass = TargetHomeConfigure.getDefaultHomeAppClassName(this);

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (DBG) Log.d(LOG_TAG, "-- The PreferredLauncher is(" + pkgName +
                ", " + pkgClass);

        if (TextUtils.isEmpty(pkgName) ||
                TextUtils.isEmpty(pkgClass)) {
            intent.setClass(this, TargetHomeConfigure.class);
        } else {
            intent.setClassName(pkgName, pkgClass);
        }

        BHAppUtility.startActivitySafely(this, intent);
    }


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (DBG) Log.v(LOG_TAG, "OoO onCreate");

        setContext(this);

        launchTargetHomeLauncher();

        finish();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DBG) Log.v(LOG_TAG, "OoO onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (DBG) Log.v(LOG_TAG, "OoO onPause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (DBG) Log.v(LOG_TAG, "OoO onDestroy");
    }
}