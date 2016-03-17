package org.stratum0.statuswidget;

import java.util.Calendar;

/**
 * Created by tsuro on 9/1/13.
 */
public class SpaceStatus {
    public enum Status {OPEN, CLOSED, UNKNOWN}
    private static final SpaceStatus instance = new SpaceStatus();

    private Calendar lastUpdate;

    private Status status;
    private String openedBy;
    private Calendar lastChange;
    private Calendar since;

    private SpaceStatus() {
        status = Status.UNKNOWN;
        openedBy = "";
        lastChange = Calendar.getInstance();
        since = Calendar.getInstance();
    }

    public static SpaceStatus getInstance() { return instance; }

    public Calendar getLastUpdate() { return lastUpdate; }

    public Status getStatus() {
        return status;
    }

    public String getOpenedBy() {
        return openedBy;
    }

    public Calendar getLastChange() {
        return lastChange;
    }

    public Calendar getSince() { return since; }

    public void update(Status status, String openedBy, Calendar lastChange, Calendar since) {
        this.lastUpdate = Calendar.getInstance();
        this.status = status;
        this.openedBy = openedBy;
        this.lastChange = lastChange;
        this.since = since;
    }

}