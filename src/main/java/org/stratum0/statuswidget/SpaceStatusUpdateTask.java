package org.stratum0.statuswidget;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.stratum0.statuswidget.GlobalVars.TAG;
import static org.stratum0.statuswidget.GlobalVars.getStatusUrl;

/**
 * Created by Matthias Uschok <dev@uschok.de on 2013-09-30.
 */
public class SpaceStatusUpdateTask extends AsyncTask <Void, Void, Void> {

    private ArrayList<SpaceStatusListener> receiverList = new ArrayList<SpaceStatusListener>();
    private SpaceStatus status;
    private Context context;

    public SpaceStatusUpdateTask(Context context) {
        this.context = context;
        status = SpaceStatus.getInstance();
    }

    @Override
    protected Void doInBackground(Void... voids) {

        Calendar now = GregorianCalendar.getInstance();
        String result = "";

        DefaultHttpClient client = new DefaultHttpClient();
        try {
            HttpResponse response = client.execute(new HttpGet(getStatusUrl));
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
            JSONObject jsonObject = new JSONObject(result);
            String uptimeString = jsonObject.getString("since");
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            Calendar since = GregorianCalendar.getInstance();
            since.setTime(f.parse(uptimeString));

            Log.d(TAG, "UpdateTask: Open?  " + status.isOpen());
            Log.d(TAG, "UpdateTask: Opened by: " + status.getOpenedBy());
            Log.d(TAG, "UpdateTask: Open since: " + status.getSince());

            synchronized (this) {
                status.update(jsonObject.getBoolean("isOpen"), jsonObject.getString("openedBy"), since, now);
            }

        } catch (JSONException e) {
            Log.d(TAG, "Error creating JSON object: " + e);
        } catch (java.text.ParseException e) {
            Log.d(TAG, "Could not parse status response: " + e);
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
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        for (SpaceStatusListener receiver : receiverList) {
            receiver.onPostSpaceStatusUpdate(context);
        }
    }

    public void addListener(SpaceStatusListener receiver) {
        receiverList.add(receiver);
    }

}
