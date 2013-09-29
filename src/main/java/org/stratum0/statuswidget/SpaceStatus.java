package org.stratum0.statuswidget;

import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.stratum0.statuswidget.GlobalVars.TAG;
import static org.stratum0.statuswidget.GlobalVars.url;

/**
 * Created by tsuro on 9/1/13.
 */
public class SpaceStatus {

    private boolean isOpen;
    private Date since;
    private String openedBy;

    private static String getStatusFromJSON() {
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

    public void update() throws ParseException {
        try {
            JSONObject jsonObject = new JSONObject(getStatusFromJSON());
            String uptime = jsonObject.getString("since");
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            since = f.parse(uptime);
            isOpen = jsonObject.getBoolean("isOpen");
            openedBy = jsonObject.getString("openedBy");
        } catch (JSONException e) {
            throw new ParseException(e);
        } catch (java.text.ParseException e) {
            throw new ParseException(e);
        }
    }

    public SpaceStatus() {
    }

    public boolean isOpen() {
        return isOpen;
    }

    public Date getSince() {
        return since;
    }

    public String getOpenedBy() {
        return openedBy;
    }
}

class ParseException extends Exception {
    public ParseException(Throwable e) {
        super(e);
    }
}


