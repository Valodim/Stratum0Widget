package org.stratum0.statuswidget;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.widget.RemoteViews;

import java.util.Calendar;


public class StratumsphereStatusProvider extends AppWidgetProvider implements SpaceStatusListener {
	
	private static final int nID = 1;
    private SpaceStatus status;
    private AppWidgetManager appWidgetManager;

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		
		GlobalVars.appWidgetIds = appWidgetIds;
        this.appWidgetManager = appWidgetManager;

        SpaceStatusUpdateTask updateTask = new SpaceStatusUpdateTask(context);
        updateTask.addListener(this);
        updateTask.execute();

        context.getSharedPreferences("preferences", Context.MODE_PRIVATE).edit().putInt("clicks", 0).commit();

	}

	@Override
	public void onReceive(final Context context, final Intent intent) {
		if(intent.getAction().equals("click")) {

            boolean firstrun = context.getSharedPreferences("preferences", Context.MODE_PRIVATE).getBoolean("firstrun", true);

            if(firstrun) {
                Intent firstrunIntent = new Intent(context, FirstRunActivity.class);
                firstrunIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(firstrunIntent);
                context.getSharedPreferences("preferences", Context.MODE_PRIVATE).edit().putBoolean("firstrun", false).commit();
            }

            // Increment click count for every received click intent
			final SharedPreferences prefs = context.getSharedPreferences("preferences", Context.MODE_PRIVATE);
			int clickCount = prefs.getInt("clicks", 0);
			prefs.edit().putInt("clicks", ++clickCount).commit();

			final Handler handler = new Handler() {
				public void handleMessage(Message msg) {

					int clickCount = prefs.getInt("clicks", 0);

					if (clickCount > 1) {
						Intent activityIntent = new Intent(context, StatusActivity.class);
						activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						context.startActivity(activityIntent);
					}
					else {
						int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);

						Intent updateIntent = new Intent(context, StratumsphereStatusProvider.class);
						updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
						updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
						context.sendBroadcast(updateIntent);
					}

					prefs.edit().putInt("clicks", 0).commit();
				}
			};

			// On the first recv'd click intent, wait 500ms before calling the handler to wait
			// for a possible second click
			if (clickCount == 1) new Thread() {
				@Override
				public void run(){
					try {
						synchronized(this) { wait(500); }
						handler.sendEmptyMessage(0);
					} catch(InterruptedException ex) {}
				}
			}.start();
		}
		super.onReceive(context, intent);
	}

    @Override
    public void onPreSpaceStatusUpdate(Context context) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.main);
        String updatingText = (String) context.getText(R.string.updating);

        // indicate that the status is currently updating
        for (int i=0; i<GlobalVars.appWidgetIds.length; i++) {
            int appWidgetId = GlobalVars.appWidgetIds[i];

            views.setTextViewText(R.id.lastUpdateTextView, updatingText);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

    }

    @Override
    public void onPostSpaceStatusUpdate(Context context) {

        status = SpaceStatus.getInstance();
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.main);
        int currentImage = R.drawable.stratum0_unknown;

        //get WiFi APIs
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        //Prepare notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification nNotOpen = new Notification();

        //legacy work for Android 2.x (where notifications need an intenthandler)
        Intent notificationIntent = new Intent(context, StratumsphereStatusProvider.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
        nNotOpen.defaults = Notification.DEFAULT_ALL;

        //setting up the notification
        nNotOpen.icon = R.drawable.stratum0_unknown;
        nNotOpen.tickerText = context.getText(R.string.nNotOpen);
        nNotOpen.when = System.currentTimeMillis();
        nNotOpen.defaults = Notification.DEFAULT_ALL;
        nNotOpen.setLatestEventInfo(context, context.getText(R.string.nNotOpenLatestEventInfo1), context.getText(R.string.nNotOpenLatestEventInfo2), contentIntent);

        String upTimeText = String.format("%02d     %02d", status.getUpTimeHours(), status.getUpTimeMins());
        String lastUpdateText = String.format("%s:\n%02d:%02d", context.getText(R.string.currentTime), status.getLastUpdated().get(Calendar.HOUR_OF_DAY), status.getLastUpdated().get(Calendar.MINUTE));

        if (status.getStatus() == SpaceStatus.Status.OPEN) {
            currentImage = R.drawable.stratum0_open;
            //dismiss previous useractionrequest
            notificationManager.cancel(nID);
        }
        else if (status.getStatus() == SpaceStatus.Status.UNKNOWN) {
            currentImage = R.drawable.stratum0_unknown;
            notificationManager.cancel(nID);
        }
        else {
            //check if connected to Stratum0 while space status is closed
            if (wifiInfo.getSSID() != null && (wifiInfo.getSSID().equals("Stratum0") || wifiInfo.getSSID().equals("Stratum0_5g"))) {
                currentImage = R.drawable.stratum0_closed;
                upTimeText = "";
                lastUpdateText += " WIFI";
                //request action from user
                notificationManager.notify(nID, nNotOpen);
            }
            else {
                //if not on matching SSID (or not anymore) dismiss the notification
                currentImage = R.drawable.stratum0_closed;
                notificationManager.cancel(nID);
            }
        }

        for (int i=0; i<GlobalVars.appWidgetIds.length; i++) {
            int appWidgetId = GlobalVars.appWidgetIds[i];

            views.setImageViewResource(R.id.statusImageView, currentImage);
            views.setTextViewText(R.id.lastUpdateTextView, lastUpdateText);
            views.setTextViewText(R.id.spaceUptimeTextView, upTimeText);

            // Register an onClickListener to custom "click" intent
            Intent intent = new Intent(context, StratumsphereStatusProvider.class);
            intent.setAction("click");
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, GlobalVars.appWidgetIds);
            PendingIntent clickIntent = PendingIntent.getBroadcast(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.statusImageView, clickIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

    }

    @Override
    public void onProgressSpaceStatusUpdate(Context context, int progress) {}
}
