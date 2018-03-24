package com.james.caywidge;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
import android.widget.RemoteViews;


/**
 * Implementation of App Widget functionality.
 */
public class CayAppWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {


            CayIntentService.startActionBaz(context);

            final int N = appWidgetIds.length;

            for (int i=0; i<N; i++) {
                int appWidgetId = appWidgetIds[i];

                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.cay_app_widget);
                Intent newintent = new Intent(context, getClass());
                newintent.setAction("cayclick");

                //Toast.makeText(context, "Intent Received: "+action, Toast.LENGTH_SHORT).show();

                PendingIntent pi = PendingIntent.getBroadcast(context, 0, newintent, 0);
                views.setOnClickPendingIntent(R.id.gobutton, pi);
                appWidgetManager.updateAppWidget(appWidgetId, views);
            }
    }


    @Override
    public void onReceive(Context context, Intent intent) {

        super.onReceive(context, intent);

        String sss = intent.getAction();

        if (sss == "cayclick"){

            CayIntentService.startActionBaz(context);
        }

        AppWidgetManager appWidgetManager= AppWidgetManager.getInstance(context);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.cay_app_widget);
        ComponentName watchWidget = new ComponentName(context, CayAppWidget.class);

        //Toast.makeText(context, sss, Toast.LENGTH_SHORT).show();

        sss = String.valueOf(DateFormat.format("h:mm:ssaa", System.currentTimeMillis())) + " " + sss;

        remoteViews.setTextViewText(R.id.appwidget_text2, sss);
        appWidgetManager.updateAppWidget(watchWidget, remoteViews);
    }

    @Override
    public void onEnabled(Context context) {
    }

    @Override
    public void onDisabled(Context context) {
    }
}