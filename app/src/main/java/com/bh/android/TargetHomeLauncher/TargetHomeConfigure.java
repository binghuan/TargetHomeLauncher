package com.bh.android.TargetHomeLauncher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class TargetHomeConfigure extends Activity implements OnItemClickListener {

    public final static boolean DBG = FeatureList.DBG;
    private final static String LOG_TAG = "BH_TargetHomeLauncher";

    private Context mContext = null;

    private void setContext(Context context) {
        mContext = context;
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BHAppUtility.getSoftwareVersion(this);

        setContext(this);
        mPkgMgr = getPackageManager();


        // Request progress bar
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setProgressBarIndeterminateVisibility(true);

        setContentView(R.layout.target_home_configure);
    }

    private void showNowLoading(boolean enabled) {

        View displayLoading = (View) findViewById(R.id.process_area);
        if (enabled) {
            displayLoading.setVisibility(View.VISIBLE);
        } else {
            displayLoading.setVisibility(View.INVISIBLE);
        }
    }

    private void checkFunctionEnabled() {
        // Check if current package is setup by default.
        if (isDefaultHomeApp(this, this.getPackageName()) == true) {
            if (isFunctionEnabled(this) == false) {
                setFunctionEnabled(this, true);
            }
        } else {
            if (isFunctionEnabled(this) == true) {
                Toast.makeText(this, R.string.indicator_off, Toast.LENGTH_SHORT).show();
                setFunctionEnabled(this, false);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DBG) Log.v(LOG_TAG, "OoO onResume");

        checkFunctionEnabled();

        readToLoadApps();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (DBG) Log.v(LOG_TAG, "OoO onPause");

        setPreHomeApp(this, mDefaultHomeApp.pkgName);

        mNeedFirstSearch = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (DBG) Log.v(LOG_TAG, "OoO onDestroy");
    }

    private void readToLoadApps() {
        if (DBG) Log.v(LOG_TAG, "OoO readyToLoadApps");

        mHandler.sendEmptyMessage(WM_LOADING_APP_START);
        new LoadAppsTask().execute();
    }

    public class LoadAppsTask extends AsyncTask<Object, Object, Object> {

        @Override
        protected Object doInBackground(Object... arg0) {

            loadApplications(true, mContext);
            LoadHomeApps(true);

            // Check If home app is changed
            if (
                    !mContext.getPackageName().equals(mDefaultHomeApp.pkgName)
                            && (!getPreHomeAppPkgName(mContext).equals(mDefaultHomeApp.pkgName) &&
                            !TextUtils.isEmpty(mDefaultHomeApp.pkgName))
                    ) {
                new flashCursorTask().execute();
            }

            mHandler.sendEmptyMessage(WM_LOAD_RECENT_APPS_COMPLETE);

            return null;
        }
    }

    private final int WM_LOAD_RECENT_APPS_COMPLETE = 1001;
    private final int WM_FLASH_ITEM_YELLOW = 1002;
    private final int WM_FLASH_ITEM_WHITE = 1003;
    private final int WM_LOADING_APP_START = 1004;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case WM_LOADING_APP_START:

                    showNowLoading(true);
                    setProgressBarIndeterminateVisibility(true);

                    break;

                case WM_LOAD_RECENT_APPS_COMPLETE:
                    showNowLoading(false);

                    bindApplications();
                    bindButtons();

                    bindHomeApp();

                    setProgressBarIndeterminateVisibility(false);

                    break;

                case WM_FLASH_ITEM_YELLOW:
                    View view = (View) findViewById(R.id.home_app);
                    view.setBackgroundColor(Color.YELLOW);

                    break;

                case WM_FLASH_ITEM_WHITE:
                    View v = (View) findViewById(R.id.home_app);
                    v.setBackgroundColor(Color.WHITE);

                    break;

                default:
                    break;
            }
        }
    };

    public class flashCursorTask extends AsyncTask<Object, Object, Object> {

        @Override
        protected Object doInBackground(Object... arg0) {
            // TODO Auto-generated method stub

            mHandler.sendEmptyMessage(WM_FLASH_ITEM_YELLOW);

            sleep(500);
            mHandler.sendEmptyMessage(WM_FLASH_ITEM_WHITE);
            sleep(200);
            mHandler.sendEmptyMessage(WM_FLASH_ITEM_YELLOW);
            sleep(200);
            mHandler.sendEmptyMessage(WM_FLASH_ITEM_WHITE);

            return null;
        }
    }

    private void sleep(long milli) {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private PackageManager mPkgMgr = null;
    private ListView mlistOfAllApps = null;

    // BH_Lin@20101208	------------------------------------------------------->
    // purpose: enhance performance.
    // 
    static class ViewHolder {
        ImageView appIcon;
        TextView appName;
    }
    // BH_Lin@20101208	-------------------------------------------------------<


    private ApplicationsAdapter mApplicationsAdapter = null;


    /**
     * Creates a new appplications adapter for the grid view and registers it.
     */
    private void bindApplications() {

        if (DBG) Log.v(LOG_TAG, "+ bindApplications");

        mlistOfAllApps = (ListView) findViewById(R.id.list_all_apps);

        if (mAppsList == null) {
            Log.e(LOG_TAG, "mAppsList is null object");
        }
        mApplicationsAdapter = new ApplicationsAdapter(this, mAppsList);

        if (mlistOfAllApps == null) {
            Log.e(LOG_TAG, "mlistOfAllApps is null object");
        } else {
            mlistOfAllApps.setAdapter(mApplicationsAdapter);
            mlistOfAllApps.setSelection(0);
        }

        // Show Default Home App's Info

    }

    ImageView homeImg = null;
    TextView homeAppVer = null;
    TextView homeAppTitle = null;

    private void bindHomeApp() {

        if (DBG) Log.v(LOG_TAG, "+ bindHomeApp");

        homeImg = (ImageView) findViewById(R.id.home_app_icon);
        homeAppVer = (TextView) findViewById(R.id.home_app_version);
        homeAppTitle = (TextView) findViewById(R.id.home_app_name);

        if ((TextUtils.isEmpty(getDefaultHomeAppPkgName(this)) != true)
                && isDefaultHomeApp(this, this.getPackageName())) {
            // show Target Home App
            // 1. icon, 2. Name, 3. Version
            bindTargetHomeApp();

            return;
        }

        homeImg.setImageDrawable(mDefaultHomeApp.appIcon);
        homeAppTitle.setText(mDefaultHomeApp.textAppName);
        if (TextUtils.isEmpty(mDefaultHomeApp.textVersion)) {
            homeAppVer.setText("");
        } else {
            homeAppVer.setText("v" + mDefaultHomeApp.textVersion);
        }
    }

    public static Drawable getAppDrawable(Context context) {

        return null;
    }

    private void bindTargetHomeApp() {

        if (DBG) Log.v(LOG_TAG, "+ bindTargetHomeApp");

        String pkgName = getDefaultHomeAppPkgName(this);
        PackageInfo pkgInfo = null;
        android.content.pm.ApplicationInfo appInfo = null;
        try {
            pkgInfo = mPkgMgr.getPackageInfo(pkgName, 0);
            appInfo = mPkgMgr.getApplicationInfo(pkgName, 0);
        } catch (NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // setup icon
        if (mPkgMgr.getApplicationIcon(appInfo) != null)
            homeImg.setImageDrawable(mPkgMgr.getApplicationIcon(appInfo));
        // setup Title
        if (appInfo.loadLabel(mPkgMgr) != null)
            homeAppTitle.setText(appInfo.loadLabel(mPkgMgr));
        // setup Version
        if (pkgInfo.versionName != null)
            homeAppVer.setText("v" + pkgInfo.versionName);
    }

    /*
    * Binds actions to the various buttons
    */
    private void bindButtons() {
        if (DBG) Log.v(LOG_TAG, "bindButtons");
        mlistOfAllApps.setOnItemClickListener(this);
    }


    // BH_Lin@20110405	------------------------------------------------------>
    // purpose:
    public static final String TARGET_HOME_LAUNCHER_KEY = "target_home_launcher_key";

    public static void setFunctionEnabled(Context context, boolean enabled) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(TARGET_HOME_LAUNCHER_KEY, enabled);
        editor.commit();
        if (DBG) Log.d(LOG_TAG, "setFunctionEnabled(" + enabled);

        if (enabled == true) {
            setMood(context, R.drawable.icon_48x48, R.string.indicator_on, true);
        } else {
            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(R.layout.target_home_configure);
            context.getPackageManager().clearPackagePreferredActivities(context.getPackageName());
            TargetHomeConfigure.setDefaultHomeApp(context, "", "");
        }
    }

    public static boolean isFunctionEnabled(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        boolean ret = sp.getBoolean(TARGET_HOME_LAUNCHER_KEY, false);

        if (DBG) Log.d(LOG_TAG, "isFunctionEnabled(" + ret);
        return ret;
    }
    // BH_Lin@20110405	------------------------------------------------------<

    public static final String PRE_HOME_APP_PKG_KEY = "pre_home_pkg_key";

    public static void setPreHomeApp(Context context, String pkgName) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(PRE_HOME_APP_PKG_KEY, pkgName);
        editor.commit();

        if (DBG) Log.d(LOG_TAG, "PRE_HOME_APP_PKG_KEY(" + pkgName);
    }

    public static String getPreHomeAppPkgName(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String ret = sp.getString(PRE_HOME_APP_PKG_KEY, "");

        if (DBG) Log.d(LOG_TAG, "getPreHomeAppPkgName(" + ret);
        return ret;
    }

    // BH_Lin@20110404	------------------------------------------------------->
    // purpose: sharePreference to get/set default home app's info.
    public static final String DEFAULT_HOME_APP_PKG_KEY = "default_home_pkg_key";
    public static final String DEFAULT_HOME_APP_CLS_KEY = "default_home_class_key";

    public static void setDefaultHomeApp(Context context, String pkgName, String className) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();

        editor.putString(DEFAULT_HOME_APP_PKG_KEY, pkgName);
        editor.putString(DEFAULT_HOME_APP_CLS_KEY, className);
        editor.commit();

        if (DBG)
            Log.d(LOG_TAG, "setDefaultHomeApp(" + pkgName + "," + className);
    }

    public static String getDefaultHomeAppPkgName(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String ret = sp.getString(DEFAULT_HOME_APP_PKG_KEY, "");

        if (DBG) Log.d(LOG_TAG, "getDefaultHomeAppPkgName(" + ret);
        return ret;
    }

    public static String getDefaultHomeAppClassName(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String ret = sp.getString(DEFAULT_HOME_APP_CLS_KEY, "");

        if (DBG) Log.d(LOG_TAG, "getDefaultHomeAppClassName(" + ret);
        return ret;
    }
    // BH_Lin@20110404	-------------------------------------------------------<

    private final static String EXTRA_KEY_APP_PKG_NAME = "pkg";

    // attributes used as keys when passing values to InstalledAppDetails activity
    public static final String APP_PKG_PREFIX = "com.android.settings.";
    public static final String EXTRA_KEY_APP_PKG_NAME_1_6 = APP_PKG_PREFIX + "ApplicationPkgName";

    private void showAppInfoDetail(String nameOfPackage) {
        if (android.os.Build.VERSION.SDK_INT <= 4) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setClassName("com.android.settings",
                    "com.android.settings.InstalledAppDetails");
            intent.putExtra(EXTRA_KEY_APP_PKG_NAME_1_6, nameOfPackage);
            mContext.startActivity(intent);
        } else if (android.os.Build.VERSION.SDK_INT >= 9) {// BH_Lin@20110403
            Uri pkgUri = Uri.parse("package:" + nameOfPackage);
            Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS",
                    pkgUri);
            intent.setClassName("com.android.settings",
                    "com.android.settings.applications.InstalledAppDetails");
            mContext.startActivity(intent);
        } else {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setClassName("com.android.settings",
                    "com.android.settings.InstalledAppDetails");
            intent.putExtra(EXTRA_KEY_APP_PKG_NAME, nameOfPackage);
            mContext.startActivity(intent);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {

        if (isDefaultHomeApp(mContext, mContext.getPackageName()) == false) {

            if (TextUtils.isEmpty(mDefaultHomeApp.pkgName)) {
                new AlertDialog.Builder(mContext)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(android.R.string.dialog_alert_title)
                        .setMessage(R.string.dialog_msg_notify_setup_default)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                showHomeMenu();

                            }
                        })
                        .setNeutralButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        })
                        .show();

            } else {

                new AlertDialog.Builder(mContext)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(android.R.string.dialog_alert_title)
                        .setMessage(R.string.dialog_msg_notify_clear_default)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                showAppInfoDetail(mDefaultHomeApp.pkgName);
                            }
                        })
                        .setNeutralButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        })
                        .show();
            }


            return;
        }

        ApplicationInfo app = (ApplicationInfo) parent.getItemAtPosition(position);

        String nameOfPackage = "";
        if (app.intent != null) {
            nameOfPackage = app.intent.getComponent().getPackageName();
        } else {
            nameOfPackage = app.pkgName.toString();
        }

        final String nameOfApp = app.title.toString();
        String nameOfClass = "";
        if (app.intent != null) {
            nameOfClass = app.intent.getComponent().getClassName();
        } else {
            nameOfClass = app.className.toString();
        }

        if (DBG)
            Log.v(LOG_TAG, "Click (" + position + "," + id + ", " + nameOfPackage
                    + ", " + nameOfApp + ", " + nameOfClass);

        // If the user selects an app to be the Home App
        // get Package Name
        setDefaultHomeApp(mContext, nameOfPackage, nameOfClass);

        // update info. of Home App.
        new flashCursorTask().execute();

        bindHomeApp();

    }


    /**
     * GridView adapter to show the list of all installed applications.
     */
    private class ApplicationsAdapter extends ArrayAdapter<ApplicationInfo> {
        public ApplicationsAdapter(Context context, ArrayList<ApplicationInfo> apps) {
            super(context, 0, apps);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder = null;

            if (convertView == null) {
                LayoutInflater mInflater = getLayoutInflater();
                convertView = mInflater.inflate(R.layout.application, parent, false);
                holder = new ViewHolder();

                holder.appIcon = (ImageView) convertView.findViewById(R.id.app_icon);
                holder.appName = (TextView) convertView.findViewById(R.id.app_name);
                convertView.setTag(holder);

            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final ApplicationInfo info = mAppsList.get(position);
            Drawable icon = info.icon;
            holder.appIcon.setImageDrawable(icon);
            holder.appName.setText(info.title);

            return convertView;
        }
    }

    // BH_Lin@20110404	------------------------------------------------------>
    // purpose: Query Current Launcher
    // 

    @SuppressWarnings("rawtypes")
    public static boolean isDefaultHomeApp(Context context, String pkgName) {

        PackageManager pkgMgr = context.getPackageManager();

        boolean ret = false;

        // Get list of preferred activities
        List prefActList = new ArrayList();
        // Intent list cannot be null. so pass empty list
        List intentList = new ArrayList();

        int result = pkgMgr.getPreferredActivities(prefActList, intentList, pkgName);

        if (DBG) Log.d(LOG_TAG, "getPreferredActivities( " +
                result);
        if (DBG)
            Log.i(LOG_TAG, "Have " + prefActList.size() + " number of activities in prefered list");
        if (prefActList.size() > 0) {
            ret = true;
        }

        return ret;
    }

    private void LoadHomeApps(boolean needSearch) {

        if (DBG) Log.v(LOG_TAG, "+ LoadHomeApps");

        if (needSearch == false) {
            if (DBG) Log.v(LOG_TAG, "no need to loadApplications");
            return;
        }

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_HOME);

        List<ResolveInfo> apps = mPkgMgr.queryIntentActivities(mainIntent, 0);

        cleanHomeAppInfo();

        if (apps != null) {
            final int count = apps.size();
            for (int i = 0; i < count; i++) {

                ResolveInfo info = apps.get(i);

                // get default launcher's label
                if (DBG)
                    Log.v(LOG_TAG, "OoO label: " + info.loadLabel(mPkgMgr));

                String pkgName = info.activityInfo.applicationInfo.packageName;
                // get default launcher's package name
                if (DBG) Log.v(LOG_TAG, "OoO pkgName: " + pkgName);


                PackageInfo packageInfo = null;
                try {
                    packageInfo = getPackageManager().getPackageInfo(
                            info.activityInfo.packageName, 0);
                } catch (NameNotFoundException e) {
                    e.printStackTrace();
                }

                ApplicationInfo application = new ApplicationInfo();
                application.title = info.loadLabel(mPkgMgr);
                application.version = packageInfo.versionName;
                application.icon = info.activityInfo.loadIcon(mPkgMgr);
                application.pkgName = pkgName;
                application.className = info.activityInfo.name;

                if (isDefaultHomeApp(this, pkgName) == true) {
                    makeCloneOfHomeAppInfo(application);
                }

                mAppsList.add(application);
            }
        }
    }

    private void cleanHomeAppInfo() {

        mDefaultHomeApp = null;
        mDefaultHomeApp = new DefaultHomeHolder();
    }

    private void makeCloneOfHomeAppInfo(ApplicationInfo application) {
        if (DBG) Log.v(LOG_TAG, "+ makeCloneOfHomeAppInfo");
        mDefaultHomeApp.appIcon = application.icon;
        mDefaultHomeApp.className = application.className.toString();
        mDefaultHomeApp.pkgName = application.pkgName.toString();
        mDefaultHomeApp.textAppName = application.title.toString();
        mDefaultHomeApp.textVersion = application.version;
    }

    private ArrayList<ApplicationInfo> mAppsList = null;

    // BH_Lin@20110404	------------------------------------------------------->
    // purpose:

    static class DefaultHomeHolder {
        Drawable appIcon;
        String textAppName;
        String textVersion;
        String pkgName;
        String className;
    }

    DefaultHomeHolder mDefaultHomeApp = new DefaultHomeHolder();

    /**
     * Maximum number of recent tasks to query.
     */
    private static final int MAX_RECENT_TASKS = 25;

    /**
     * Loads the list of installed applications in mApplications.
     */
    public void loadApplications(boolean isLaunching, Context context) {
        if (DBG) Log.v(LOG_TAG, "loadApplications");

        if (mPkgMgr == null) {
            mPkgMgr = context.getPackageManager();
        }

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        if (DBG) Log.v(LOG_TAG, "queryIntentActivities");
        final List<ResolveInfo> apps = mPkgMgr.queryIntentActivities(mainIntent, 0);
        if (DBG) Log.v(LOG_TAG, "sorting");

        if (DBG) Log.v(LOG_TAG, "ready to add app info");

        if (apps != null) {
            final int count = apps.size();

            if (mAppsList == null) {
                mAppsList = new ArrayList<ApplicationInfo>(count);
            }
            mAppsList.clear();

            for (int i = 0; i < count; i++) {
                ApplicationInfo application = new ApplicationInfo();
                ResolveInfo info = apps.get(i);

                application.title = info.loadLabel(mPkgMgr);

                application.setActivity(new ComponentName(
                                info.activityInfo.applicationInfo.packageName,
                                info.activityInfo.name),
                        Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                application.icon = info.activityInfo.loadIcon(mPkgMgr);


                // BH_Lin@20100530 -------------------------------------------->
                // purpose: get Application Version 
                // 

                PackageInfo packageInfo = null;

                try {
                    packageInfo = getPackageManager().getPackageInfo(
                            info.activityInfo.packageName, 0);
                    application.version = packageInfo.versionName;
                } catch (NameNotFoundException e) {
                    e.printStackTrace();
                }

                int FLAG_EXTERNAL_STORAGE = 0x40000;
                int FLAG_FORWARD_LOCK = 0x100000;

                boolean moveDisable = true;

                try {
                    android.content.pm.ApplicationInfo mAppInfo =
                            mPkgMgr.getApplicationInfo(packageInfo.packageName,
                                    PackageManager.GET_UNINSTALLED_PACKAGES);

                    if (DBG)
                        Log.v(LOG_TAG, packageInfo.packageName + ":" + mAppInfo.sourceDir);

                    String appSourceDir = mAppInfo.sourceDir;
                    if ((appSourceDir.indexOf("/data/app") != -1) ||
                            (appSourceDir.indexOf("/system/app") != -1)) {
                        application.isInternal = true;
                    }


                    if ((mAppInfo.flags & FLAG_EXTERNAL_STORAGE) != 0) {
                        if (DBG) Log.v(LOG_TAG,
                                "Always let apps move to internal storage from sdcard");
                        moveDisable = false;
                    } else {
                        if ((mAppInfo.flags & FLAG_FORWARD_LOCK) == 0 &&
                                (mAppInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 &&

                                packageInfo != null) {

                        }
                    }
                } catch (NameNotFoundException e) {
                    Log.e(LOG_TAG, "Exception when retrieving package: " +
                            packageInfo.versionName, e);
                }

                application.isMovable = (!moveDisable);

                mAppsList.add(application);
            }
        }

        // BH_Lin@20100508	--------------------------------------------------->
        // purpose: enhance sorting method.
        Collections.sort(mAppsList, new Comparator<ApplicationInfo>() {
            public int compare(ApplicationInfo arg0, ApplicationInfo arg1) {
                return ((String) arg0.getOrder()).compareTo((String) arg1.getOrder());
            }
        });
    }

    private ProgressDialog mDialog = null;

    private static ApplicationInfo applicationInfo = new ApplicationInfo();
    public static int mLoadingAppsCounter = 0;

    private static ApplicationInfo getApplicationInfo(PackageManager manager, Intent intent) {
        final ResolveInfo resolveInfo = manager.resolveActivity(intent, 0);

        if (resolveInfo == null) {
            return null;
        }

        final ApplicationInfo info = new ApplicationInfo();
        final ActivityInfo activityInfo = resolveInfo.activityInfo;
        info.icon = activityInfo.loadIcon(manager);
        if (info.title == null || info.title.length() == 0) {
            info.title = activityInfo.loadLabel(manager);
        }
        if (info.title == null) {
            info.title = "";
        }
        return info;
    }

    private static int MENU_ITEM_BASE_INDEX = 0;
    private static final int MENU_ITEM_VIEW_MORE_APPS = MENU_ITEM_BASE_INDEX++;
    private static final int MENU_ITEM_ABOUT = MENU_ITEM_BASE_INDEX++;
    private static final int MENU_ITEM_HOME_MENU = MENU_ITEM_BASE_INDEX++;
    private static final int MENU_ITEM_RESET = MENU_ITEM_BASE_INDEX++;

    private boolean mNeedFirstSearch = true;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        if (DBG) Log.v(LOG_TAG, "onCreateOptionsMenu");

        menu.add(0, MENU_ITEM_RESET, 0, R.string.menu_reset)
                .setOnMenuItemClickListener(mReset)
                .setIcon(android.R.drawable.ic_menu_revert);

        menu.add(0, MENU_ITEM_HOME_MENU, 0, R.string.menu_home)
                .setOnMenuItemClickListener(mHomeMenu)
                .setIcon(android.R.drawable.ic_menu_agenda);

        if (FeatureList.FEATURE_SAMSUNG_APPS != true) {
            menu.add(0, MENU_ITEM_VIEW_MORE_APPS, 0, R.string.menu_more_apps)
                    .setOnMenuItemClickListener(mMoreApps)
                    .setIcon(android.R.drawable.ic_menu_view);
        }


        menu.add(0, MENU_ITEM_ABOUT, 0, R.string.menu_about)
                .setOnMenuItemClickListener(mAbout)
                .setIcon(android.R.drawable.ic_menu_info_details);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {


        return true;
    }

    private MenuItem.OnMenuItemClickListener mReset = new MenuItem.OnMenuItemClickListener() {

        public boolean onMenuItemClick(MenuItem item) {
            if (DBG)
                Log.v(LOG_TAG, "mHomeMenu, Current Home(" + mDefaultHomeApp.pkgName);

            if (isDefaultHomeApp(mContext, mContext.getPackageName()) == true) {
                getPackageManager().clearPackagePreferredActivities(mContext.getPackageName());

                checkFunctionEnabled();
                readToLoadApps();
            } else {
                Toast.makeText(mContext, R.string.msg_no_need_reset, Toast.LENGTH_SHORT).show();
            }

            return true;
        }
    };

    // BH_Lin@20100617	------------------------------------------------------->
    // purpose: about me
    private MenuItem.OnMenuItemClickListener mAbout = new MenuItem.OnMenuItemClickListener() {

        public boolean onMenuItemClick(MenuItem item) {
            // TODO Auto-generated method stub
            if (DBG) Log.v(LOG_TAG, "mAbout");

            LayoutInflater factory = LayoutInflater.from(mContext);
            final View textEntryView = factory.inflate(R.layout.about, null);

            final TextView appVersion =
                    (TextView) textEntryView.findViewById(R.id.app_version);
            appVersion.setText(BHAppUtility.getSoftwareVersion(mContext));

            final TextView sendMail =
                    (TextView) textEntryView.findViewById(R.id.act_sendmail);

            // text4 illustrates constructing a styled string containing a
            // link without using HTML at all.  Again, for a fixed string
            // you should probably be using a string resource, not a
            // hardcoded value.

            SpannableString ss = new SpannableString(
                    getText(R.string.sendmail).toString());

            ss.setSpan(new URLSpan("mailto:binghuanlin@gmail"), 0,
                    getText(R.string.sendmail).toString().length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            sendMail.setText(ss);

            sendMail.setMovementMethod(LinkMovementMethod.getInstance());

            new AlertDialog.Builder(mContext)
                    .setIcon(R.drawable.icon)
                    .setTitle(R.string.app_name)
                    .setView(textEntryView)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                    /* User clicked OK so do some stuff */
                        }
                    }).show();

            return true;
        }
    };

    private MenuItem.OnMenuItemClickListener mMoreApps = new MenuItem.OnMenuItemClickListener() {

        public boolean onMenuItemClick(MenuItem item) {
            if (DBG) Log.v(LOG_TAG, "mMoreApps");

            //BHAppUtility.playSound(mContext, BHAppUtility.SOUND_VIEW_MORE_APPS);

            Uri uri = Uri.parse("market://search?q=pub:BH_Lin");
            Intent launchIntent = new Intent(Intent.ACTION_VIEW, uri);
            BHAppUtility.startActivitySafely(mContext, launchIntent);
            //where pkg_name is the full package path for an application

            return true;
        }
    };

    private void showHomeMenu() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setClassName("android", "com.android.internal.app.ResolverActivity");
        BHAppUtility.startActivitySafely(mContext, intent);
    }

    private MenuItem.OnMenuItemClickListener mHomeMenu = new MenuItem.OnMenuItemClickListener() {

        public boolean onMenuItemClick(MenuItem item) {
            if (DBG)
                Log.v(LOG_TAG, "mHomeMenu, Current Home(" + mDefaultHomeApp.pkgName);

            //adb shell am start -a android.intent.action.MAIN \
            //-c android.intent.category.HOME -f 0x10200000 \
            //-n android/com.android.internal.app.ResolverActivity

//            ComponentName mainComponent = new ComponentName(mContext ,
//            		TargetHomeConfigure.class);
//            getPackageManager().setComponentEnabledSetting(mainComponent,
//            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,PackageManager.DONT_KILL_APP);

            showHomeMenu();

            //where pkg_name is the full package path for an application

            return true;
        }
    };


    public static void setMood(Context context, int moodId, int textId, boolean showTicker) {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        String tickerText = showTicker ? context.getString(textId) : null;

        Intent pendingIntent = new Intent(Intent.ACTION_MAIN);
        pendingIntent.addCategory(Intent.CATEGORY_HOME);
        pendingIntent.setClassName("android", "com.android.internal.app.ResolverActivity");

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, pendingIntent, flags);

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= 26) {
             String channelId = "target_home_channel";
             android.app.NotificationChannel channel = new android.app.NotificationChannel(channelId, "Target Home", NotificationManager.IMPORTANCE_LOW);
             mNotificationManager.createNotificationChannel(channel);
             builder = new Notification.Builder(context, channelId);
        } else {
             builder = new Notification.Builder(context);
        }

        builder.setSmallIcon(moodId)
                .setTicker(tickerText)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(context.getText(R.string.app_name))
                .setContentText(context.getText(R.string.notice_msg))
                .setContentIntent(contentIntent)
                .setOngoing(true);

        Notification notification;
        if (Build.VERSION.SDK_INT >= 16) {
            notification = builder.build();
        } else {
            notification = builder.getNotification();
        }

        // Send the notification.
        // We use a layout id because it is a unique number.  We use it later to cancel.
        mNotificationManager.notify(R.layout.target_home_configure, notification);
    }
}