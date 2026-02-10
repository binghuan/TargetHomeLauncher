package com.bh.android.TargetHomeLauncher;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class TargetHomeReceiver extends BroadcastReceiver {

    private boolean DBG = FeatureList.DBG;

    private static final String LOG_TAG = "BH_TargetHomeReceiver";

    private Handler mHandler;

    private NotificationManager mNotificationManager = null;

    private Context mContext = null;
    public static boolean isEnableAutoRotate = false;

    public TargetHomeReceiver() {
        if (DBG) Log.v(LOG_TAG, "OoO Constructor");

        if (mHandler == null) {
            if (DBG) Log.v(LOG_TAG, "OoO newHandler");
            mHandler = new Handler();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void init(Context context) {
        if (DBG) Log.d(LOG_TAG, "OoO init");
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        mContext = context;

        if (mNotificationManager == null) {
            if (DBG) Log.d(LOG_TAG, "OoO getSystemServer ");
            mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        }

        if (DBG) Log.d(LOG_TAG, "OoO onReceiveIntent " + action);
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            if (TargetHomeConfigure.isFunctionEnabled(context) == true) {
                if (DBG) Log.d(LOG_TAG, "OoO Enabling Function");
                TargetHomeConfigure.setFunctionEnabled(context, true);
            } else {
                if (DBG) Log.d(LOG_TAG, "OoO not to Enabling Function");
            }
        } else if (action.equals(Intent.ACTION_PACKAGE_REPLACED)) {

            String dataString = intent.getDataString();
            if (!dataString.contains(context.getPackageName())) {
                return;
            }

            if ((TargetHomeConfigure.isFunctionEnabled(context) == true) &&
                    (TargetHomeConfigure.isDefaultHomeApp(context, context.getPackageName()) == true)) {
                TargetHomeConfigure.setFunctionEnabled(context, true);
            } else {
                TargetHomeConfigure.setFunctionEnabled(context, false);
            }
        }
    }
}
