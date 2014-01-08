package org.stratum0.statuswidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import static org.stratum0.statuswidget.GlobalVars.TAG;
import static org.stratum0.statuswidget.GlobalVars.appWidgetIds;
import static org.stratum0.statuswidget.GlobalVars.statusUrl;

/**
 * Created by Matthias Uschok <dev@uschok.de> on 2013-10-06.
 */
public class SpaceStatusChangeTask extends AsyncTask <String, Void, Void> {

    private ArrayList<SpaceStatusListener> receiverList = new ArrayList<SpaceStatusListener>();
    private Context context;

    public SpaceStatusChangeTask(Context context) {
        this.context = context;
    }

    @Override
    protected Void doInBackground(String... strings) {

        try {
            DefaultHttpClient client = new DefaultHttpClient();
            HttpResponse response = client.execute(new HttpGet(statusUrl + strings[0]));
            if(response.getStatusLine().getStatusCode() == 200) {
                Thread.sleep(500);
                SpaceStatusUpdateTask updateTask = new SpaceStatusUpdateTask(null);
                updateTask.execute();
                updateTask.get();
            }
        } catch (MalformedURLException e) {
            Log.e(TAG, "Update request: malformed URL.", e);
        } catch (IOException e) {
            Log.e(TAG, "Update request: could not connect to server.", e);
        } catch (InterruptedException e) {
            Log.e(TAG, "Wait for new status didn't finish:", e);
        } catch (ExecutionException e) {
            Log.e(TAG, "Error executing update task inside change task:", e);
        }

        Intent updateIntent = new Intent(context, StratumsphereStatusProvider.class);
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        context.sendBroadcast(updateIntent);

        return null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        for (SpaceStatusListener receiver : receiverList) {
            receiver.onPreSpaceStatusUpdate(context);
        }
    }

    @Override
    protected void onPostExecute(Void avoid) {
        super.onPostExecute(avoid);
        for (SpaceStatusListener receiver : receiverList) {
            receiver.onPostSpaceStatusUpdate(context);
        }
    }

    public void addListener(SpaceStatusListener receiver) {
        receiverList.add(receiver);
    }

}
