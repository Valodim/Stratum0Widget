package org.stratum0.statuswidget;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.stratum0.statuswidget.GlobalVars.TAG;
import static org.stratum0.statuswidget.GlobalVars.url;

/**
 * Created by tsuro on 9/1/13.
 */
public class SpaceStatus {
    private static final SpaceStatus instance = new SpaceStatus();

    private boolean isOpen;
    private Calendar since;
    private Calendar lastUpdated;
    private String openedBy;
    private long upTimeHours;
    private long upTimeMins;

    private SpaceStatus() {}

    public static SpaceStatus getInstance() {
        return instance;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public Calendar getSince() {
        return since;
    }

    public String getOpenedBy() {
        return openedBy;
    }

    public Calendar getLastUpdated() {
        return lastUpdated;
    }

    public long getUpTimeHours() {
        return upTimeHours;
    }

    public long getUpTimeMins() {
        return upTimeMins;
    }

    public String getLastUpdatedString() {
        return String.format("%02d:%02d", lastUpdated.get(Calendar.HOUR_OF_DAY), lastUpdated.get(Calendar.MINUTE));
    }

    public void update(boolean isOpen, String openedBy, Calendar openSince, Calendar lastUpdated) {
        this.isOpen = isOpen;
        this.openedBy = openedBy;
        this.since = openSince;
        this.lastUpdated = lastUpdated;

        long difference = lastUpdated.getTimeInMillis() - since.getTimeInMillis();
        upTimeMins = (difference)/(1000*60) % 60;
        upTimeHours = (difference)/(1000*60) / 60;
    }
}

class ParseException extends Exception {
    public ParseException(Throwable e) {
        super(e);
    }
}


