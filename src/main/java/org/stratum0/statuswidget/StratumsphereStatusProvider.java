package org.stratum0.statuswidget;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

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
import android.util.Log;
import android.app.NotificationManager;
import android.app.Notification;


public class StratumsphereStatusProvider extends AppWidgetProvider {
	
	public static final String TAG = "Stratum0";
	private static final String url = "http://status.stratum0.org/status.json";
	private static final int nID = 1;

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		
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
		
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.main);
		int currentImage = R.drawable.stratum0_unknown;

		// indicate that the status is currently updating
		for (int i=0; i<appWidgetIds.length; i++) {
			int appWidgetId = appWidgetIds[i];

			String updatingText = (String) context.getText(R.string.updating);
			views.setTextViewText(R.id.lastUpdateTextView, updatingText);
			appWidgetManager.updateAppWidget(appWidgetId, views);
		}

		String jsonText = getStatusFromJSON();
		Date now = new GregorianCalendar().getTime();

		String upTimeText = "";
		String text = String.format("%s:\n%02d:%02d", context.getText(R.string.currentTime), now.getHours(), now.getMinutes());

		if (jsonText.startsWith("{") && jsonText.endsWith("}")) {
			try {
				JSONObject jsonObject = new JSONObject(jsonText);
				String upTime = jsonObject.getString("since");
				SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
				Date d = f.parse(upTime);
				long difference = now.getTime() - d.getTime();
				//TODO Date class probably offers a better way to do this
				long upTimeMins = (difference)/(1000*60) % 60;
				long upTimeHours = (difference)/(1000*60) / 60;
				upTimeText = String.format("%02d     %02d", upTimeHours, upTimeMins);

				if (jsonObject.getBoolean("isOpen")) {
					currentImage = R.drawable.stratum0_open;
					//dismiss previous useractionrequest
					notificationManager.cancel(nID);
				}
				else {
					//check if connected to Stratum0 while space status is closed
                    if (wifiInfo.getSSID() != null && (wifiInfo.getSSID().equals("Stratum0") || wifiInfo.getSSID().equals("Stratum0_5g"))) {
                        openSpace();
                        currentImage = R.drawable.stratum0_closed;
							upTimeText = "";
							text += " WIFI";
							//request action from user
							notificationManager.notify(nID, nNotOpen);
					}
					else {
						//if not on matching SSID (or not anymore) dismiss the notification
						currentImage = R.drawable.stratum0_closed;
						notificationManager.cancel(nID);
					}
				}
			} catch (Exception e) {
				Log.w(TAG, "Exception " + e);
			}
		}
		for (int i=0; i<appWidgetIds.length; i++) {
			int appWidgetId = appWidgetIds[i];

			views.setImageViewResource(R.id.statusImageView, currentImage);
			views.setTextViewText(R.id.lastUpdateTextView, text);
			views.setTextViewText(R.id.spaceUptimeTextView, upTimeText);
			
			// Register an onClickListener to custom "click" intent
			Intent intent = new Intent(context, StratumsphereStatusProvider.class);
			intent.setAction("click");
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
			PendingIntent clickIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			views.setOnClickPendingIntent(R.id.statusImageView, clickIntent);

			appWidgetManager.updateAppWidget(appWidgetId, views);
		}
		context.getSharedPreferences("preferences", Context.MODE_PRIVATE).edit().putInt("clicks", 0).commit();

	}

	@Override
	public void onReceive(final Context context, final Intent intent) {
		if(intent.getAction().equals("click")) {

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

    private void openSpace() {
		// call some API to open the Space (change status to open)
	}

	public static String getStatusFromJSON() {
		String result = "";
		DefaultHttpClient client = new DefaultHttpClient();
		try {
			HttpResponse response = client.execute(new HttpGet(url));
			if (response.getStatusLine().getStatusCode() == 200) {
				BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				String line;
				while ((line = br.readLine()) != null) {
					result += line;
				}
			}
		} catch (Exception e) {
			Log.w(TAG, "Exception " + e);
		}
		return result;
	}



}
