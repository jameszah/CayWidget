package com.james.caywidge;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Context;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

public class CayIntentService extends IntentService {

    public static final String PARAM_IN_MSG = "imsg";
    public static final String PARAM_OUT_MSG = "omsg";

    RemoteViews view;
    ComponentName theWidget;
    AppWidgetManager manager;

    private static final String ACTION_FOO = "com.james.caywidge.action.FOO";
    private static final String ACTION_BAZ = "com.james.caywidge.action.BAZ";

    private static final String EXTRA_PARAM1 = "com.james.caywidge.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "com.james.caywidge.extra.PARAM2";
    private static final String EXTRA_PARAM3 = "com.james.caywidge.extra.PARAM3";
    private static final String EXTRA_PARAM4 = "com.james.caywidge.extra.PARAM4";


    public CayIntentService() {
        super("CayIntentService");
    }

    public void onCreate(){
        super.onCreate();

    }

    public static void startActionFoo(Context context, String param1, String param2) {
        Intent intent = new Intent(context, CayIntentService.class);
        intent.setAction(ACTION_FOO);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    public static void startActionBaz(Context context) {
        Intent intent = new Intent(context, CayIntentService.class);
        intent.setAction(ACTION_BAZ);

        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_FOO.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionFoo(param1, param2);
            } else if (ACTION_BAZ.equals(action)) {
                handleActionBaz();
            }
        }
    }

    private void handleActionFoo(String param1, String param2) {
    }

    private void handleActionBaz() {

        String cayenneemail = "MrPeanut@gmail.com";
        String cayennepassword = "peanutpassword";

        String devID1 = "bd7309e0-2e04-11e8-822e-bbf389efce87";
        String senID1 = "44979b00-2e39-11e8-9beb-4d400e603e7e";
        String devID2 = "bd7309e0-2e04-11e8-822e-bbf389efce87";
        String senID2 = "41989860-2e24-11e8-82f6-390cc0260849";

        Cayenne myCayenne;
        Cayenne.CayDataPoint Temp1;
        Cayenne.CayDataPoint Temp2;

        try {

            view = new RemoteViews(getPackageName(), R.layout.cay_app_widget);

            myCayenne = new Cayenne(cayenneemail, cayennepassword, -6, null);
            myCayenne.sync_getCayenneAuth();

            if (myCayenne.CayAuthToken != ""){

                myCayenne.sync_getThings();

                Temp1 = myCayenne.new CayDataPoint(devID1, senID1, null);
                Temp2 = myCayenne.new CayDataPoint(devID2, senID2, null);

                String t1 = Temp1.sync_update();
                String t2 = Temp2.sync_update();

                if (Temp1.Date_ts != null){
                    view.setTextViewText(R.id.appwidget_t1,  Temp1.sensorname + " " + t1);
                } else {
                    view.setTextViewText(R.id.appwidget_t1,  "First: bad point ???");
                }

                if (Temp2.Date_ts != null) {
                    view.setTextViewText(R.id.appwidget_t2,  Temp2.sensorname + " " + t2);
                } else {
                    view.setTextViewText(R.id.appwidget_t2,  "Second: no response ???");
                }

                view.setTextViewText(R.id.timebox, String.valueOf(DateFormat.format("h:mm:ssaa", System.currentTimeMillis())));

            } else {

                view.setTextViewText(R.id.appwidget_t1, " ");
                view.setTextViewText(R.id.appwidget_t2, " ");
                view.setTextViewText(R.id.timebox, " ");

                view.setTextViewText(R.id.appwidget_text2, String.valueOf(DateFormat.format("h:mm:ssaa", System.currentTimeMillis())) + " " + "Cayenne auth failed");
            }

            theWidget = new ComponentName(this, CayAppWidget.class);
            manager = AppWidgetManager.getInstance(this);
            manager.updateAppWidget(theWidget, view);

        } catch (Exception e){
            // hmm do nothing i guess
        }

    }
}
