package org.stratum0.statuswidget;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import static org.stratum0.statuswidget.GlobalVars.TAG;
import static org.stratum0.statuswidget.GlobalVars.setStatusAttempts;
import static org.stratum0.statuswidget.GlobalVars.setStatusUrl;

/**
 * Created by Matthias Uschok <dev@uschok.de> on 2013-10-06.
 */
public class SpaceStatusChangeTask extends AsyncTask <String, Integer, Void> {

    private SpaceStatus status;
    private SpaceStatus.Status currentStatus, requestedStatus;
    private ArrayList<SpaceStatusListener> receiverList = new ArrayList<SpaceStatusListener>();
    private Context context;

    public SpaceStatusChangeTask(Context context) {
        this.context = context;
        status = SpaceStatus.getInstance();
    }

    @Override
    protected Void doInBackground(String... strings) {

        String updateUrl = setStatusUrl + strings[0];
        if (strings[0].contains("open")) {
            requestedStatus = SpaceStatus.Status.OPEN;
        }
        else if (strings[0].contains("close")) {
            requestedStatus = SpaceStatus.Status.CLOSED;
        }

        currentStatus = status.getStatus();
        try {
            for (int i = 0; i < setStatusAttempts; i++) {
                URL u = new URL(updateUrl);
                URLConnection c = u.openConnection();
                c.connect();
                c.getContent();
                Thread.sleep(1000);
                SpaceStatusUpdateTask updateTask = new SpaceStatusUpdateTask(null);
                updateTask.execute();
                currentStatus = updateTask.get();
                if (currentStatus == requestedStatus) {
                    break;
                }
                else {
                    publishProgress(i+1);
                    URL ircstatus = new URL(setStatusUrl + "Status%20change%20failed.%20Trying%20again...%20%28" + (i+1) + "%20of%20" + setStatusAttempts + "%29");
                    URLConnection irc = ircstatus.openConnection();
                    irc.connect();
                    irc.getContent();
                }
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

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        for (SpaceStatusListener receiver: receiverList) {
            receiver.onProgressSpaceStatusUpdate(context, values[0]);
        }
    }

    public void addListener(SpaceStatusListener receiver) {
        receiverList.add(receiver);
    }

}
