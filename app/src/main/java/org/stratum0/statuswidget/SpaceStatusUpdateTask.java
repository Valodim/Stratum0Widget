package org.stratum0.statuswidget;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.stratum0.statuswidget.GlobalVars.TAG;
import static org.stratum0.statuswidget.GlobalVars.statusUrl;

/**
 * Created by Matthias Uschok <dev@uschok.de> on 2013-09-30.
 */
public class SpaceStatusUpdateTask extends AsyncTask <Void, Void, SpaceStatus.Status> {

    private ArrayList<SpaceStatusListener> receiverList = new ArrayList<SpaceStatusListener>();
    private SpaceStatus status;
    private Context context;

    public SpaceStatusUpdateTask(Context context) {
        this.context = context;
        status = SpaceStatus.getInstance();
    }

    @Override
    protected SpaceStatus.Status doInBackground(Void... voids) {

        String result = "";
        SpaceStatus.Status isOpen = SpaceStatus.Status.UNKNOWN;

        DefaultHttpClient client = new DefaultHttpClient();
        final HttpParams httpParams = client.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 10000);
        HttpConnectionParams.setSoTimeout(httpParams, 10000);

        try {
            HttpResponse response = client.execute(new HttpGet(statusUrl + "/status.json"));
            if (response.getStatusLine().getStatusCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                String line;
                while ((line = br.readLine()) != null) {
                    result += line;
                }
            }
        } catch (IOException e) {
            Log.w(TAG, "Something went wrong getting the status, probably timeout: ", e);
        } catch (Exception e) {
            Log.w(TAG, "Error getting JSON message: " + e);
        }

        try {
            JSONObject jsonRoot = new JSONObject(result);
            JSONObject spaceStatus = jsonRoot.getJSONObject("state");

            Calendar lastChange = GregorianCalendar.getInstance();
            lastChange.setTimeInMillis(spaceStatus.getLong("lastchange") * 1000);
            Calendar since = GregorianCalendar.getInstance();
            since.setTimeInMillis(spaceStatus.getLong("ext_since") * 1000);

            isOpen = spaceStatus.getBoolean("open") ? SpaceStatus.Status.OPEN : SpaceStatus.Status.CLOSED;

            synchronized (this) {
                status.update(isOpen, spaceStatus.getString("trigger_person"), lastChange, since);
            }

            /*
            Log.d(TAG, "UpdateTask: Open?  " + status.getStatus());
            Log.d(TAG, "UpdateTask: Opened by: " + status.getOpenedBy());
            Log.d(TAG, "UpdateTask: Open since: " + status.getSince());
            */

        } catch (JSONException e) {
            Log.d(TAG, "Error creating JSON object: " + e);
            synchronized (this) {
                Calendar now = GregorianCalendar.getInstance();
                status.update(SpaceStatus.Status.UNKNOWN, "", now, now);
            }
        }

        return status.getStatus();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        for (SpaceStatusListener receiver : receiverList) {
            receiver.onPreSpaceStatusUpdate(context);
        }
    }

    @Override
    protected void onPostExecute(SpaceStatus.Status status) {
        super.onPostExecute(status);
        for (SpaceStatusListener receiver : receiverList) {
            receiver.onPostSpaceStatusUpdate(context);
        }
    }

    public void addListener(SpaceStatusListener receiver) {
        receiverList.add(receiver);
    }

}
