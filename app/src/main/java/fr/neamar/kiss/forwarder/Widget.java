package fr.neamar.kiss.forwarder;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.os.UserManager;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;

class Widget extends Forwarder {

    private static final int REQUEST_BIND_APPWIDGET = 11;
    public static final int REQUEST_BIND_PENDING_APPWIDGET = 12;

    private static final int REQUEST_PICK_APPWIDGET = 9;
    private static final int REQUEST_CREATE_APPWIDGET = 5;

//    private static final int APPWIDGET_HOST_ID = 442;
    private static final String WIDGET_PREFERENCE_ID = "fr.neamar.kiss.widgetprefs";

    private SharedPreferences widgetPrefs;

    /**
     * Widget fields
     */
    private AppWidgetManager mAppWidgetManager;
    private AppWidgetHost mAppWidgetHost;
    private boolean widgetUsed = false;

    /**
     * View widgets are added to
     */
    private ViewGroup widgetArea;

    Widget(MainActivity mainActivity) {
        super(mainActivity);
    }

    void onCreate() {
        // Initialize widget manager and host, restore widgets
        widgetPrefs = mainActivity.getSharedPreferences(WIDGET_PREFERENCE_ID, Context.MODE_PRIVATE);

//        mAppWidgetManager = AppWidgetManager.getInstance(getApp);
//        mAppWidgetHost = new AppWidgetHost(mainActivity.getApplicationContext(), APPWIDGET_HOST_ID);

        mAppWidgetManager = KissApplication.getAppWidgetManager();
        mAppWidgetHost = KissApplication.getAppWidgetHost();

        widgetArea = mainActivity.findViewById(R.id.widgetLayout);

        restoreWidgets();
    }

    void onStart() {
        // Start listening for widget update
//        mAppWidgetHost.startListening();
    }

    void onStop() {
        // Stop listening for widget update
//        mAppWidgetHost.stopListening();
    }

    void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CREATE_APPWIDGET:
                    addAppWidget(data);
                    break;
                case REQUEST_PICK_APPWIDGET:
                    configureAppWidget(data);
                    break;
            }
        } else if (resultCode == Activity.RESULT_CANCELED && data != null) {
            //if widget was not selected, delete id
            int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (appWidgetId != -1) {
                mAppWidgetHost.deleteAppWidgetId(appWidgetId);
            }
        }
    }

    boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.widget) {
            if (!widgetUsed) {
                // request widget picker, a selection will lead to a call of onActivityResult
                int appWidgetId = mAppWidgetHost.allocateAppWidgetId();

                Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
                pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                    // this bit of code adds work profile widgets to the default widget picker
                    //
                    // for custom widgets to be shown in the default widget picker (AppWidgetPickActivity),
                    // two extras must be added to the intent:
                    // - EXTRA_CUSTOM_INFO: containing AppWidgetProviderInfos
                    // - EXTRA_CUSTOM_EXTRAS: containing Bundles with provider and profile for each widget
                    //
                    // the size of both lists must match and data for the same widget
                    // must be in the same position in both lists

                    UserManager userMgr = (UserManager) mainActivity.getSystemService(Context.USER_SERVICE);

                    ArrayList<AppWidgetProviderInfo> allWidgets = new ArrayList<>();
                    ArrayList<Bundle> allExtras = new ArrayList<>();

                    for (android.os.UserHandle user : userMgr.getUserProfiles()) {
                        if (!Process.myUserHandle().equals(user)) {
                            // only for users not the current
                            for (AppWidgetProviderInfo info : mAppWidgetManager.getInstalledProvidersForProfile(user)) {
                                // convert to kiss provider info
//                                KissAppWidgetProviderInfo kissInfo = KissAppWidgetProviderInfo.fromProviderInfo(mainActivity, info);

                                // add widgets from provider to list
                                allWidgets.add(info);

                                // add extras for widgets
                                Bundle bdl = new Bundle();
                                bdl.putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider);
                                bdl.putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, user);

                                Bundle bdlOptions = new Bundle();
                                bdlOptions.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, info.minHeight);
                                bdl.putBundle(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, bdlOptions);

                                allExtras.add(bdl);
                            }
                        }
                    }

//                    pickIntent.setExtrasClassLoader(KissAppWidgetProviderInfo.class.getClassLoader());
                    pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, allWidgets);
                    pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, allExtras);
                }

                mainActivity.startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET);

            } else {
                // if we already have a widget we remove it
                removeAllWidgets();
            }
            return true;
        }

        return false;
    }

    void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (prefs.getBoolean("history-hide", true)) {
            if (widgetUsed) {
                menu.findItem(R.id.widget).setTitle(R.string.menu_widget_remove);
            } else {
                menu.findItem(R.id.widget).setTitle(R.string.menu_widget_add);
            }
        } else {
            menu.findItem(R.id.widget).setVisible(false);
        }
    }

    void onDataSetChanged() {
        if (widgetUsed && mainActivity.adapter.isEmpty()) {
            // when a widget is displayed the empty list would prevent touches on the widget
            mainActivity.emptyListView.setVisibility(View.GONE);
        }
    }

    /**
     * Restores all previously added widgets
     */
    private void restoreWidgets() {
        HashMap<String, Integer> widgetIds = (HashMap<String, Integer>) widgetPrefs.getAll();
        for (int appWidgetId : widgetIds.values()) {
            addWidgetToLauncher(appWidgetId, null);
        }
    }

    /**
     * Adds a widget to the widget area on the MainActivity
     *
     * @param appWidgetId id of widget to add
     */
    private void addWidgetToLauncher(int appWidgetId, Intent data) {
        // only add widgets if in minimal mode (may need launcher restart when turned on)
        if (prefs.getBoolean("history-hide", true)) {
            // remove empty list view when using widgets, this would block touches on the widget
            mainActivity.emptyListView.setVisibility(View.GONE);

            //add widget to view
            AppWidgetProviderInfo appWidgetInfo;
            if (data != null) {
                appWidgetInfo = getAppWidgetInfo(data);
            } else {
                appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
            }

            if (appWidgetInfo == null) {
                removeAllWidgets();
                return;
            }

            AppWidgetHostView hostView = mAppWidgetHost.createView(this.mainActivity.getApplicationContext(), appWidgetId, appWidgetInfo);
            hostView.setMinimumHeight(appWidgetInfo.minHeight);
            hostView.setAppWidget(appWidgetId, appWidgetInfo);

            if (Build.VERSION.SDK_INT > 15) {
//                hostView.updateAppWidgetSize(null, appWidgetInfo.minWidth, appWidgetInfo.minHeight, appWidgetInfo.minWidth, appWidgetInfo.minHeight);
            }

            widgetArea.addView(hostView);
        }

        // only one widget allowed so widgetUsed is true now, even if not added to view
        widgetUsed = true;
    }

    /**
     * Removes all widgets from the launcher
     */
    private void removeAllWidgets() {
        while (widgetArea.getChildCount() > 0) {
            AppWidgetHostView widget = (AppWidgetHostView) widgetArea.getChildAt(0);
            removeAppWidget(widget);
        }
    }

    /**
     * Removes a single widget and deletes it from persistent prefs
     *
     * @param hostView instance of a displayed widget
     */
    private void removeAppWidget(AppWidgetHostView hostView) {
        // remove widget from view
        int appWidgetId = hostView.getAppWidgetId();
        mAppWidgetHost.deleteAppWidgetId(appWidgetId);
        widgetArea.removeView(hostView);
        // remove widget id from persistent prefs
        SharedPreferences.Editor widgetPrefsEditor = widgetPrefs.edit();
        widgetPrefsEditor.remove(String.valueOf(appWidgetId));
        widgetPrefsEditor.apply();
        // only one widget allowed so widgetUsed is false now
        widgetUsed = false;
    }

    /**
     * Adds widget to Activity and persists it in prefs to be able to restore it
     *
     * @param data Intent holding widget id to add
     */
    private void addAppWidget(Intent data) {
        int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        //add widget
        addWidgetToLauncher(appWidgetId, data);
        // Save widget in preferences
        SharedPreferences.Editor widgetPrefsEditor = widgetPrefs.edit();
        widgetPrefsEditor.putInt(String.valueOf(appWidgetId), appWidgetId);
        widgetPrefsEditor.apply();
    }

    /**
     * Check if widget needs configuration and display configuration view if necessary,
     * otherwise just add the widget
     *
     * @param data Intent holding widget id to configure
     */
    private void configureAppWidget(Intent data) {
        int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

        AppWidgetProviderInfo appWidget = getAppWidgetInfo(data);

        if (appWidget != null && appWidget.configure != null) {
            // Launch over to configure widget, if needed.
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            intent.setComponent(appWidget.configure);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ComponentName provider = data.getParcelableExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER);
                android.os.UserHandle user = data.getParcelableExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, user);

                // add extras for widgets
                Bundle bdl = new Bundle();
                bdl.putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider);
                bdl.putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, user);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, bdl);
                intent.putExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, bdl);
            }

            mainActivity.startActivityForResult(intent, REQUEST_CREATE_APPWIDGET);
        } else {
            // Otherwise, finish adding the widget.
            addAppWidget(data);
        }
    }

    private AppWidgetProviderInfo getAppWidgetInfo(Intent data) {
        int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

        // tries to get from AppWidgetManager (only for current user)
        AppWidgetProviderInfo appWidget =
                mAppWidgetManager.getAppWidgetInfo(appWidgetId);

        if (appWidget == null) {
            // try to get info by provider
            ComponentName provider = data.getParcelableExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER);
            android.os.UserHandle user = data.getParcelableExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE);
            appWidget = getAppWidgetInfoByProvider(provider, user);
        }

        return appWidget;
    }

    private AppWidgetProviderInfo getAppWidgetInfoByProvider(ComponentName provider, android.os.UserHandle user) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for (AppWidgetProviderInfo info : mAppWidgetManager.getInstalledProvidersForProfile(user)) {
                if (info.provider.equals(provider)) {
                    return info;
                }
            }
        }

        return null;
    }

}
