package fr.neamar.kiss;

import android.app.Application;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;

public class KissApplication extends Application {
    /**
     * Number of ms to wait, after a click occurred, to record a launch
     * Setting this value to 0 removes all animations
     */
    public static final int TOUCH_DELAY = 120;
    private DataHandler dataHandler;
    private RootHandler rootHandler;
    private IconsHandler iconsPackHandler;

    public static KissApplication getApplication(Context context) {
        return (KissApplication) context.getApplicationContext();
    }

    public DataHandler getDataHandler() {
        if (dataHandler == null) {
            dataHandler = new DataHandler(this);
        }
        return dataHandler;
    }

    public void setDataHandler(DataHandler newDataHandler) {
        dataHandler = newDataHandler;
    }

    public RootHandler getRootHandler() {
        if (rootHandler == null) {
            rootHandler = new RootHandler(this);
        }
        return rootHandler;
    }

    public void resetRootHandler(Context ctx) {
        rootHandler.resetRootHandler(ctx);
    }

    public void initDataHandler() {
        if (dataHandler == null) {
            dataHandler = new DataHandler(this);
        }
        else if(dataHandler.allProvidersHaveLoaded) {
            // Already loaded! We still need to fire the FULL_LOAD event
            Intent i = new Intent(MainActivity.FULL_LOAD_OVER);
            sendBroadcast(i);
        }
    }

    public IconsHandler getIconsHandler() {
        if (iconsPackHandler == null) {
            iconsPackHandler = new IconsHandler(this);
        }

        return iconsPackHandler;
    }

    public void resetIconsHandler() {
        iconsPackHandler = new IconsHandler(this);
    }



    private static final int HARDCODED_ID = 442;

    private static AppWidgetHost appWidgetHost;
    private static AppWidgetManager appWidgetManager;

    @Override
    public void onCreate() {
        super.onCreate();

        appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        appWidgetHost = new AppWidgetHost(getApplicationContext(), HARDCODED_ID);
        appWidgetHost.startListening();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        appWidgetHost.stopListening();
        appWidgetHost = null;
    }

    public static AppWidgetHost getAppWidgetHost() { return appWidgetHost; }

    public static AppWidgetManager getAppWidgetManager() { return appWidgetManager; }
}
