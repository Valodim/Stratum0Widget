package org.stratum0.statuswidget;

import java.util.Calendar;

/**
 * Created by tsuro on 9/1/13.
 */
public class SpaceStatus {
    public enum Status {OPEN, CLOSED, UNKNOWN}
    private static final SpaceStatus instance = new SpaceStatus();

    private Status status;
    private Calendar since;
    private Calendar lastUpdated;
    private String openedBy;
    private long upTimeHours;
    private long upTimeMins;

    private SpaceStatus() {
        status = Status.UNKNOWN;
        since = Calendar.getInstance();
        lastUpdated = Calendar.getInstance();
        openedBy = "";
        upTimeHours = 0;
        upTimeMins = 0;
    }

    public static SpaceStatus getInstance() {
        return instance;
    }

    public Status getStatus() {
        return status;
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

    public void update(Status status, String openedBy, Calendar openSince, Calendar lastUpdated) {
        this.status = status;
        this.openedBy = openedBy;
        this.since = openSince;
        this.lastUpdated = lastUpdated;

        long difference = lastUpdated.getTimeInMillis() - since.getTimeInMillis();
        upTimeMins = (difference)/(1000*60) % 60;
        upTimeHours = (difference)/(1000*60) / 60;
    }
}